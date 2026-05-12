package slimeknights.tconstruct.port1211.common.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;

/**
 * Pinned-behaviour tests for the {@link ToolModifiers} data component. Covers both codecs,
 * insertion-order preservation across every transformation the component participates in,
 * structural-update via {@link ToolModifiers#with}, and the record's immutability contract.
 */
class ToolModifiersTest {

    private static final ResourceLocation SHARPNESS = ResourceLocation.fromNamespaceAndPath("tconstruct", "sharpness");
    private static final ResourceLocation HASTE = ResourceLocation.fromNamespaceAndPath("tconstruct", "haste");
    private static final ResourceLocation REINFORCED = ResourceLocation.fromNamespaceAndPath("tconstruct", "reinforced");

    /** Three-entry fixture with a deliberate non-alphabetic key order to catch order regressions. */
    private static ToolModifiers fixture() {
        Map<ResourceLocation, Integer> seed = new LinkedHashMap<>();
        seed.put(SHARPNESS, 3);
        seed.put(HASTE, 2);
        seed.put(REINFORCED, 1);
        return new ToolModifiers(seed);
    }

    @Test
    void roundTripsThroughJsonCodecPreservesInsertionOrder() {
        ToolModifiers original = fixture();
        JsonElement encoded = ToolModifiers.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        ToolModifiers decoded = ToolModifiers.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original, decoded, "JSON round-trip must preserve the map verbatim");
        assertIterableEquals(List.of(SHARPNESS, HASTE, REINFORCED), decoded.levels().keySet(), "Insertion order must survive the JSON round-trip");
    }

    @Test
    void roundTripsThroughNbtOpsPreservesInsertionOrder() {
        // This is the codec path the DataComponentType actually exercises in production —
        // ItemStack persistence goes through NbtOps, and Minecraft's CompoundTag is
        // contractually unordered (HashMap-backed). The list-shaped CODEC must compensate.
        ToolModifiers original = fixture();
        Tag encoded = ToolModifiers.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        ToolModifiers decoded = ToolModifiers.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original, decoded, "NBT round-trip must preserve the map verbatim");
        assertIterableEquals(List.of(SHARPNESS, HASTE, REINFORCED), decoded.levels().keySet(), "Insertion order must survive the NBT round-trip (the actual production save path)");
    }

    @Test
    void roundTripsThroughStreamCodecPreservesInsertionOrder() {
        ToolModifiers original = fixture();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolModifiers.STREAM_CODEC.encode(buf, original);
            ToolModifiers decoded = ToolModifiers.STREAM_CODEC.decode(buf);
            assertEquals(original, decoded, "binary stream round-trip must preserve the map");
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte written by the encoder");
            assertIterableEquals(List.of(SHARPNESS, HASTE, REINFORCED), decoded.levels().keySet(), "Insertion order must survive the binary round-trip");
        }
        finally {
            buf.release();
        }
    }

    @Test
    void emptyModifiersRoundTrips() {
        ToolModifiers empty = ToolModifiers.empty();
        JsonElement encoded = ToolModifiers.CODEC.encodeStart(JsonOps.INSTANCE, empty).getOrThrow();
        assertEquals(empty, ToolModifiers.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolModifiers.STREAM_CODEC.encode(buf, empty);
            assertEquals(empty, ToolModifiers.STREAM_CODEC.decode(buf));
        }
        finally {
            buf.release();
        }
    }

    @Test
    void withAddsANewModifierAppendingToTheEndOfTheIterationOrder() {
        ToolModifiers updated = fixture().with(ResourceLocation.fromNamespaceAndPath("tconstruct", "luck"), 1);

        assertIterableEquals(List.of(SHARPNESS, HASTE, REINFORCED, ResourceLocation.fromNamespaceAndPath("tconstruct", "luck")), updated.levels().keySet(),
                "A new modifier should append to the end of the existing iteration order");
    }

    @Test
    void withUpdatesAnExistingModifierWithoutChangingIterationSlot() {
        // HASTE is the middle entry; updating it must not move it.
        ToolModifiers updated = fixture().with(HASTE, 5);

        assertEquals(Integer.valueOf(5), updated.levels().get(HASTE), "with() must update the level on an existing key");
        assertIterableEquals(List.of(SHARPNESS, HASTE, REINFORCED), updated.levels().keySet(), "Updating an existing key must leave its insertion slot untouched");
    }

    @Test
    void withReturnsANewInstanceAndDoesNotMutateTheOriginal() {
        ToolModifiers original = fixture();
        ToolModifiers updated = original.with(HASTE, 99);

        assertNotSame(original, updated, "with() must return a fresh ToolModifiers instance");
        assertEquals(Integer.valueOf(2), original.levels().get(HASTE), "The original instance's HASTE level must remain unchanged after with()");
    }

    @Test
    void recordsWithSameLevelsAreEqual() {
        // Construct the same map twice via different paths to confirm equals/hashCode are
        // value-based, not reference-based.
        ToolModifiers a = fixture();
        Map<ResourceLocation, Integer> seed = new LinkedHashMap<>();
        seed.put(SHARPNESS, 3);
        seed.put(HASTE, 2);
        seed.put(REINFORCED, 1);
        ToolModifiers b = new ToolModifiers(seed);

        assertEquals(a, b, "records with identical levels must compare equal");
        assertEquals(a.hashCode(), b.hashCode(), "equal records must share a hashCode");
    }

    @Test
    void constructorDefensivelyCopiesInputMap() {
        // Caller passes a mutable map and then mutates it — the record must not see the change,
        // protecting component snapshots from drift when callers recycle their build buffer.
        Map<ResourceLocation, Integer> mutable = new LinkedHashMap<>();
        mutable.put(SHARPNESS, 1);
        ToolModifiers snapshot = new ToolModifiers(mutable);

        mutable.put(HASTE, 7);

        assertEquals(Map.of(SHARPNESS, 1), snapshot.levels(), "external mutation of the input map must not leak into the record");
    }

    @Test
    void levelsMapIsUnmodifiable() {
        ToolModifiers modifiers = new ToolModifiers(Map.of(SHARPNESS, 1));
        assertThrows(UnsupportedOperationException.class, () -> modifiers.levels().put(HASTE, 1), "levels() must return an unmodifiable view so the record stays immutable");
    }

    @Test
    void emptyFactoryReturnsTheCanonicalEmptyInstance() {
        // The empty() factory is documented as returning the same instance on every call so
        // "no modifiers" never allocates. Two consecutive empty() calls must be reference-equal.
        ToolModifiers first = ToolModifiers.empty();
        ToolModifiers second = ToolModifiers.empty();
        assertSame(first, second, "empty() must reuse the canonical singleton instance");
        assertTrue(first.levels().isEmpty(), "empty() levels map must be empty");
        assertNotSame(first, new ToolModifiers(Map.of()),
                "constructing a fresh instance should not collapse onto the singleton — guards" + " against the empty() factory accidentally returning a fresh allocation");
    }
}
