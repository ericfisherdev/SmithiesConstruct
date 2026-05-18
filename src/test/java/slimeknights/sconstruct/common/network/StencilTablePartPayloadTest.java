package slimeknights.sconstruct.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.tools.PartType;

/**
 * Pinned-behaviour tests for {@link StencilTablePartPayload}. Covers the codec-driven fields
 * (pos + direction) at the record-equality level — the {@code StreamCodec.composite} machinery
 * is exercised under server boot integration so a unit-level codec round-trip would need a
 * mocked {@link net.minecraft.network.RegistryFriendlyByteBuf} pipeline that isn't worth its
 * weight here — and the cursor stepping math via {@link StencilTablePartPayload#stepPart}.
 *
 * <p>The {@code stepPart} test is the load-bearing one: it's the entire client-supplied input
 * → server-applied state contract, and a regression here would let a malformed direction value
 * silently land the cursor on a wrong PartType.
 */
class StencilTablePartPayloadTest {

    @Test
    void recordFieldsRoundTripViaConstructor() {
        // Records expose their canonical constructor + accessors as the trivial codec round-
        // trip equivalent. Exercises that the BlockPos and direction values land on the right
        // accessor names — a swapped field order in the record header would surface here.
        BlockPos pos = new BlockPos(5, 64, -12);
        StencilTablePartPayload payload = new StencilTablePartPayload(pos, -1);
        assertEquals(pos, payload.pos());
        assertEquals(-1, payload.direction());
    }

    @Test
    void typeChannelIdIsUnderModNamespace() {
        // The CustomPacketPayload.Type#id is the on-wire channel identifier. A namespace
        // mismatch (e.g. "minecraft:") would silently route the packet to a vanilla handler.
        assertEquals("sconstruct", StencilTablePartPayload.TYPE.id().getNamespace());
        assertEquals("stencil_table_part", StencilTablePartPayload.TYPE.id().getPath());
    }

    @Test
    void stepPartAdvancesByOnePositiveDirection() {
        assertEquals(PartType.AXEHEAD, StencilTablePartPayload.stepPart(PartType.PICKHEAD, 1));
    }

    @Test
    void stepPartRetreatsByOneNegativeDirection() {
        assertEquals(PartType.PICKHEAD, StencilTablePartPayload.stepPart(PartType.AXEHEAD, -1));
    }

    @Test
    void stepPartWrapsForwardFromLastToFirst() {
        PartType[] values = PartType.values();
        PartType last = values[values.length - 1];
        assertEquals(values[0], StencilTablePartPayload.stepPart(last, 1));
    }

    @Test
    void stepPartWrapsBackwardFromFirstToLast() {
        PartType[] values = PartType.values();
        PartType last = values[values.length - 1];
        assertEquals(last, StencilTablePartPayload.stepPart(values[0], -1));
    }

    @Test
    void stepPartReducesLargerDirectionsModuloEnumLength() {
        // Step amounts larger than values().length must reduce. A future "fast forward" button
        // could pass a larger direction without the server needing a separate handler.
        PartType[] values = PartType.values();
        int len = values.length;
        // Step by exactly len lands on the same value.
        assertEquals(PartType.PICKHEAD, StencilTablePartPayload.stepPart(PartType.PICKHEAD, len));
        // Step by len + 1 lands on the next value.
        assertEquals(PartType.AXEHEAD, StencilTablePartPayload.stepPart(PartType.PICKHEAD, len + 1));
        // Negative wraparound similarly.
        assertEquals(values[len - 2], StencilTablePartPayload.stepPart(values[len - 1], -(len + 1)));
    }
}
