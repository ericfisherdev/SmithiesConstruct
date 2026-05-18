package slimeknights.sconstruct.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

/**
 * Pinned-behaviour tests for {@link ToolStationActionPayload}. Covers record-equality of the
 * codec-driven fields (pos + action) and the channel id under the mod namespace. The
 * {@link StreamCodec.composite} round-trip is exercised at server boot integration; a unit-
 * level mock of {@link net.minecraft.network.RegistryFriendlyByteBuf} would add no signal here.
 *
 * <p>The {@link ToolStationActionPayload.Action} enum's ordinal contract is also pinned — the
 * STREAM_CODEC encodes the action as an ordinal varint, and reordering the constants is a
 * wire-protocol break that this test surfaces.
 */
class ToolStationActionPayloadTest {

    @Test
    void recordFieldsRoundTripViaConstructor() {
        BlockPos pos = new BlockPos(12, 70, -3);
        ToolStationActionPayload payload = new ToolStationActionPayload(pos, ToolStationActionPayload.Action.BUILD);
        assertEquals(pos, payload.pos());
        assertEquals(ToolStationActionPayload.Action.BUILD, payload.action());
    }

    @Test
    void typeChannelIdIsUnderModNamespace() {
        assertEquals("sconstruct", ToolStationActionPayload.TYPE.id().getNamespace());
        assertEquals("tool_station_action", ToolStationActionPayload.TYPE.id().getPath());
    }

    @Test
    void streamCodecExists() {
        // The STREAM_CODEC is composite of BlockPos.STREAM_CODEC + Action.STREAM_CODEC; a null
        // here would mean the static-init order regressed and the payload would fail to
        // register at boot time.
        assertNotNull(ToolStationActionPayload.STREAM_CODEC);
        assertNotNull(ToolStationActionPayload.Action.STREAM_CODEC);
    }

    @Test
    void actionEnumOrdinalContractStable() {
        // Wire-format pin: ordinals are persisted on the wire. A reordering of the enum
        // constants is a backwards-incompatible protocol break.
        assertEquals(0, ToolStationActionPayload.Action.BUILD.ordinal());
        assertEquals(1, ToolStationActionPayload.Action.MODIFY.ordinal());
        assertEquals(2, ToolStationActionPayload.Action.RENAME.ordinal());
    }
}
