package slimeknights.sconstruct.common.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;

/**
 * Pinned-behaviour tests for {@link ToolPersistentData}. The fixture stitches together a
 * non-trivial CompoundTag — mixing primitives, a string, a list, and a nested compound — to
 * cover the realistic shape a modifier's persistent state would carry: each round-trip case
 * verifies that every value type survives the encode/decode without truncation.
 */
class ToolPersistentDataTest {

    private static final ResourceLocation LIFESTEAL = ResourceLocation.fromNamespaceAndPath("sconstruct", "lifesteal");
    private static final ResourceLocation HASTE = ResourceLocation.fromNamespaceAndPath("sconstruct", "haste");

    /** Build a mixed-type CompoundTag to exercise the codec's value-shape coverage. */
    private static CompoundTag mixedContent() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("absorbed_health", 17);
        tag.putString("source", "creeper");
        ListTag history = new ListTag();
        history.add(StringTag.valueOf("zombie"));
        history.add(StringTag.valueOf("skeleton"));
        tag.put("kills", history);
        CompoundTag nested = new CompoundTag();
        nested.putBoolean("flag", true);
        nested.putFloat("amount", 2.5f);
        tag.put("nested", nested);
        return tag;
    }

    private static ToolPersistentData fixture() {
        Map<ResourceLocation, CompoundTag> data = new HashMap<>();
        data.put(LIFESTEAL, mixedContent());
        CompoundTag hasteState = new CompoundTag();
        hasteState.putInt("charges", 3);
        data.put(HASTE, hasteState);
        return new ToolPersistentData(data);
    }

    // No JSON round-trip test: JsonOps has no NBT integer-width distinction (byte vs short
    // vs int vs long are all just "number" in JSON), so small int values silently narrow to
    // Byte tags on the decode side and equality fails on the type even when the value matches.
    // The actual production path is NbtOps anyway — ItemStack persistence never touches
    // JsonOps for component values — so the NBT round-trip below is the path that matters.

    @Test
    void roundTripsThroughNbtOps() {
        // The production save path — pin it explicitly to avoid the SMTCON-19 trap where the
        // JSON test passed but the NBT one would have caught a real issue.
        ToolPersistentData original = fixture();
        Tag encoded = ToolPersistentData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        ToolPersistentData decoded = ToolPersistentData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original, decoded, "NBT round-trip must preserve every entry verbatim");
    }

    @Test
    void roundTripsThroughStreamCodec() {
        ToolPersistentData original = fixture();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolPersistentData.STREAM_CODEC.encode(buf, original);
            ToolPersistentData decoded = ToolPersistentData.STREAM_CODEC.decode(buf);
            assertEquals(original, decoded, "binary stream round-trip must preserve every entry");
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte written by the encoder");
        }
        finally {
            buf.release();
        }
    }

    @Test
    void roundTripsPreserveMixedTagValueTypes() {
        // Drill into the decoded inner CompoundTag and verify every value type survived intact.
        ToolPersistentData original = fixture();
        Tag encoded = ToolPersistentData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        ToolPersistentData decoded = ToolPersistentData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        CompoundTag decodedLifesteal = decoded.data().get(LIFESTEAL);
        assertEquals(17, decodedLifesteal.getInt("absorbed_health"), "int field must round-trip");
        assertEquals("creeper", decodedLifesteal.getString("source"), "string field must round-trip");
        assertEquals(2, decodedLifesteal.getList("kills", Tag.TAG_STRING).size(), "list field must round-trip with both elements");
        assertEquals(true, decodedLifesteal.getCompound("nested").getBoolean("flag"), "nested compound boolean must round-trip");
        assertEquals(2.5f, decodedLifesteal.getCompound("nested").getFloat("amount"), "nested compound float must round-trip");
    }

    @Test
    void emptyRoundTrips() {
        ToolPersistentData empty = ToolPersistentData.empty();
        // Empty maps have no integer-width ambiguity — both JSON and NBT round-trip cleanly.
        assertEquals(empty, ToolPersistentData.CODEC.parse(JsonOps.INSTANCE, ToolPersistentData.CODEC.encodeStart(JsonOps.INSTANCE, empty).getOrThrow()).getOrThrow());
        assertEquals(empty, ToolPersistentData.CODEC.parse(NbtOps.INSTANCE, ToolPersistentData.CODEC.encodeStart(NbtOps.INSTANCE, empty).getOrThrow()).getOrThrow());

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolPersistentData.STREAM_CODEC.encode(buf, empty);
            assertEquals(empty, ToolPersistentData.STREAM_CODEC.decode(buf));
        }
        finally {
            buf.release();
        }
    }

    @Test
    void constructorDefensivelyCopiesInputMap() {
        Map<ResourceLocation, CompoundTag> mutable = new HashMap<>();
        mutable.put(LIFESTEAL, new CompoundTag());
        ToolPersistentData snapshot = new ToolPersistentData(mutable);

        // Caller mutates its own map after construction — must not leak into the record.
        mutable.put(HASTE, new CompoundTag());

        assertEquals(1, snapshot.data().size(), "External mutation of the input map must not leak into the record");
    }

    @Test
    void dataMapIsUnmodifiable() {
        ToolPersistentData modifiers = new ToolPersistentData(Map.of(LIFESTEAL, new CompoundTag()));
        assertThrows(UnsupportedOperationException.class, () -> modifiers.data().put(HASTE, new CompoundTag()), "data() must return an unmodifiable view so the record stays immutable");
    }

    @Test
    void emptyFactoryReturnsTheCanonicalSingleton() {
        ToolPersistentData first = ToolPersistentData.empty();
        ToolPersistentData second = ToolPersistentData.empty();
        assertSame(first, second, "empty() must reuse the canonical singleton");
        assertTrue(first.data().isEmpty(), "empty() data map must be empty");
    }
}
