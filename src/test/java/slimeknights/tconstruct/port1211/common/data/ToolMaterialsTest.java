package slimeknights.tconstruct.port1211.common.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;

/**
 * Pinned-behaviour tests for the {@link ToolMaterials} data component. Exercises both codecs
 * (JSON round-trip via {@link JsonOps#INSTANCE} and binary round-trip via a {@link
 * FriendlyByteBuf}) plus the record's immutability contract.
 */
class ToolMaterialsTest {

    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");
    private static final ResourceLocation WOOD = ResourceLocation.fromNamespaceAndPath("tconstruct", "wood");
    private static final ResourceLocation STRING = ResourceLocation.fromNamespaceAndPath("minecraft", "string");

    @Test
    void roundTripsThroughJsonCodec() {
        ToolMaterials original = new ToolMaterials(List.of(IRON, WOOD, STRING));
        JsonElement encoded = ToolMaterials.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        ToolMaterials decoded = ToolMaterials.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original, decoded, "JSON round-trip must preserve the parts list verbatim");
    }

    @Test
    void roundTripsThroughStreamCodec() {
        ToolMaterials original = new ToolMaterials(List.of(IRON, WOOD));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ToolMaterials.STREAM_CODEC.encode(buf, original);
        ToolMaterials decoded = ToolMaterials.STREAM_CODEC.decode(buf);
        assertEquals(original, decoded, "binary stream round-trip must preserve the parts list");
        assertEquals(0, buf.readableBytes(), "decoder should consume every byte written by the encoder");
    }

    @Test
    void emptyMaterialsRoundTrips() {
        ToolMaterials empty = ToolMaterials.empty();
        JsonElement encoded = ToolMaterials.CODEC.encodeStart(JsonOps.INSTANCE, empty).getOrThrow();
        assertEquals(empty, ToolMaterials.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ToolMaterials.STREAM_CODEC.encode(buf, empty);
        assertEquals(empty, ToolMaterials.STREAM_CODEC.decode(buf));
    }

    @Test
    void recordsWithSameMaterialsAreEqual() {
        ToolMaterials a = new ToolMaterials(List.of(IRON, WOOD));
        ToolMaterials b = new ToolMaterials(List.of(IRON, WOOD));
        assertEquals(a, b, "records with identical parts must compare equal");
        assertEquals(a.hashCode(), b.hashCode(), "equal records must share a hashCode");
    }

    @Test
    void constructorDefensivelyCopiesInputList() {
        // Caller passes a mutable list and then mutates it — the record must not see the change,
        // otherwise component snapshots could be silently corrupted by callers that recycle
        // their build buffer.
        List<ResourceLocation> mutable = new ArrayList<>(List.of(IRON));
        ToolMaterials snapshot = new ToolMaterials(mutable);

        mutable.add(WOOD);

        assertEquals(List.of(IRON), snapshot.parts(), "external mutation of the input list must not leak into the record");
    }

    @Test
    void partsListIsUnmodifiable() {
        ToolMaterials materials = new ToolMaterials(List.of(IRON));
        assertThrows(UnsupportedOperationException.class, () -> materials.parts().add(WOOD), "parts() must return an unmodifiable view so the record stays immutable");
    }

    @Test
    void emptyFactoryReturnsTheCanonicalEmptyInstance() {
        // The empty() factory is documented as returning the same instance on every call so
        // "no materials" never allocates. Two consecutive empty() calls must be reference-equal.
        ToolMaterials first = ToolMaterials.empty();
        ToolMaterials second = ToolMaterials.empty();
        assertSame(first, second, "empty() must reuse the canonical singleton instance");
        assertTrue(first.parts().isEmpty(), "empty() parts list must be empty");
        assertNotSame(first, new ToolMaterials(List.of()),
                "constructing a fresh instance should not collapse onto the singleton — " + "the test guards against the empty() factory accidentally returning a fresh allocation");
    }
}
