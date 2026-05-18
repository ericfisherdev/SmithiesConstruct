package slimeknights.sconstruct.common.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;

/**
 * Pinned-behaviour tests for the {@link ToolStats} data component. Exercises all three codec
 * paths (JSON / NBT / binary stream) plus the {@link ToolStats#zero} singleton and equality.
 */
class ToolStatsTest {

    /** Realistic non-zero fixture with every field set to a distinguishable value. */
    private static ToolStats fixture() {
        return new ToolStats(/* maxDurability */ 1024, /* attackDamage */ 5.5f, /* attackSpeed */ 1.25f, /* miningSpeed */ 7.0f, /* harvestLevel */ 3, /* freeModifiers */ 2, /* drawSpeed */ 0.8f,
                /* bowRange */ 12.5f, /* projectileBonus */ 2.25f);
    }

    @Test
    void roundTripsThroughJsonCodec() {
        ToolStats original = fixture();
        JsonElement encoded = ToolStats.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        ToolStats decoded = ToolStats.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original, decoded, "JSON round-trip must preserve every stat field");
    }

    @Test
    void roundTripsThroughNbtOps() {
        // ItemStack persistence in 1.20.5+ goes through NbtOps; exercising it here pins the
        // actual production save/load path, not just the JSON path.
        ToolStats original = fixture();
        Tag encoded = ToolStats.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        ToolStats decoded = ToolStats.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original, decoded, "NBT round-trip must preserve every stat field");
    }

    @Test
    void roundTripsThroughStreamCodec() {
        ToolStats original = fixture();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolStats.STREAM_CODEC.encode(buf, original);
            ToolStats decoded = ToolStats.STREAM_CODEC.decode(buf);
            assertEquals(original, decoded, "binary stream round-trip must preserve every stat field");
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte written by the encoder");
        }
        finally {
            buf.release();
        }
    }

    @Test
    void zeroIsACanonicalSnapshotOfAllZeroValues() {
        ToolStats zero = ToolStats.zero();
        assertEquals(0, zero.maxDurability(), "zero() must set maxDurability to 0");
        assertEquals(0f, zero.attackDamage(), "zero() must set attackDamage to 0");
        assertEquals(0f, zero.attackSpeed(), "zero() must set attackSpeed to 0");
        assertEquals(0f, zero.miningSpeed(), "zero() must set miningSpeed to 0");
        assertEquals(0, zero.harvestLevel(), "zero() must set harvestLevel to 0");
        assertEquals(0, zero.freeModifiers(), "zero() must set freeModifiers to 0");
        assertEquals(0f, zero.drawSpeed(), "zero() must set drawSpeed to 0");
        assertEquals(0f, zero.bowRange(), "zero() must set bowRange to 0");
        assertEquals(0f, zero.projectileBonus(), "zero() must set projectileBonus to 0");
    }

    @Test
    void zeroReturnsTheCanonicalSingleton() {
        // The zero() factory is documented as returning the same instance on every call so the
        // sentinel "stats not yet computed" never allocates per stack.
        ToolStats first = ToolStats.zero();
        ToolStats second = ToolStats.zero();
        assertSame(first, second, "zero() must reuse the canonical singleton instance");
    }

    @Test
    void rejectsNonFiniteFloatStats() {
        // Each float field gets its own assertion so that a future refactor that accidentally
        // drops one of them from the validator surfaces here.
        assertThrows(IllegalArgumentException.class, () -> new ToolStats(100, Float.NaN, 1f, 1f, 1, 0, 1f, 1f, 1f), "NaN attackDamage must be rejected");
        assertThrows(IllegalArgumentException.class, () -> new ToolStats(100, 1f, Float.POSITIVE_INFINITY, 1f, 1, 0, 1f, 1f, 1f), "+Infinity attackSpeed must be rejected");
        assertThrows(IllegalArgumentException.class, () -> new ToolStats(100, 1f, 1f, Float.NEGATIVE_INFINITY, 1, 0, 1f, 1f, 1f), "-Infinity miningSpeed must be rejected");
        assertThrows(IllegalArgumentException.class, () -> new ToolStats(100, 1f, 1f, 1f, 1, 0, Float.NaN, 1f, 1f), "NaN drawSpeed must be rejected");
        assertThrows(IllegalArgumentException.class, () -> new ToolStats(100, 1f, 1f, 1f, 1, 0, 1f, Float.POSITIVE_INFINITY, 1f), "+Infinity bowRange must be rejected");
        assertThrows(IllegalArgumentException.class, () -> new ToolStats(100, 1f, 1f, 1f, 1, 0, 1f, 1f, Float.NaN), "NaN projectileBonus must be rejected");
    }

    @Test
    void rejectsNegativeCountFields() {
        assertThrows(IllegalArgumentException.class, () -> new ToolStats(-1, 1f, 1f, 1f, 1, 0, 1f, 1f, 1f), "Negative maxDurability must be rejected");
        assertThrows(IllegalArgumentException.class, () -> new ToolStats(100, 1f, 1f, 1f, 1, -1, 1f, 1f, 1f), "Negative freeModifiers must be rejected");
    }

    @Test
    void recordsWithSameFieldsAreEqual() {
        ToolStats a = fixture();
        ToolStats b = fixture();
        assertEquals(a, b, "records with identical fields must compare equal");
        assertEquals(a.hashCode(), b.hashCode(), "equal records must share a hashCode");
    }

    @Test
    void recordsWithAnyDifferingFieldAreNotEqual() {
        // Quick sanity check that the record's auto-generated equals actually inspects every
        // field — a missing field in the canonical constructor would slip through if equals
        // only compared a subset.
        ToolStats base = fixture();
        ToolStats different = new ToolStats(base.maxDurability() + 1, base.attackDamage(), base.attackSpeed(), base.miningSpeed(), base.harvestLevel(), base.freeModifiers(), base.drawSpeed(),
                base.bowRange(), base.projectileBonus());
        assertNotEquals(base, different, "records differing in maxDurability must not compare equal");
    }
}
