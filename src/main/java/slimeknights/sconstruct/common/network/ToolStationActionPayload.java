package slimeknights.sconstruct.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.tools.block.entity.ToolStationBlockEntity;
import slimeknights.sconstruct.tools.inventory.ToolStationMenu;

/**
 * Client→server payload for the Tool Station / Tool Forge action buttons (SMTCON-94). Carries
 * the BE's {@link BlockPos} so the server can locate the station the player is interacting with,
 * plus an {@link Action} discriminant identifying which button the player pressed.
 *
 * <p>Server-side handling validates the player has the matching {@link ToolStationMenu} open
 * against the supplied pos and is still within reach before delegating to the action's handler.
 * Malformed payloads (unknown action ordinal, mis-encoded BlockPos) surface as decoder errors
 * during {@link #STREAM_CODEC} read — NeoForge's payload pipeline disconnects the client when
 * the decoder throws.
 *
 * <p>Wire format: a {@link BlockPos} (long-packed) followed by a varint enum ordinal. The
 * {@link Action#STREAM_CODEC} clamps the ordinal range so out-of-bounds values fail at decode
 * time rather than reaching the server handler.
 *
 * <p>Registered on the play-to-server channel via
 * {@link slimeknights.sconstruct.common.TinkerNetwork#onRegisterPayloads}.
 */
public record ToolStationActionPayload(BlockPos pos, Action action) implements CustomPacketPayload {

    /** Channel id under the mod namespace. */
    public static final CustomPacketPayload.Type<ToolStationActionPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "tool_station_action"));

    /**
     * Network codec. Composite of the standard long-packed BlockPos encoding and the
     * {@link Action} stream codec — the order matches the {@link ToolStationActionPayload}
     * record component order.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolStationActionPayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, ToolStationActionPayload::pos, Action.STREAM_CODEC,
            ToolStationActionPayload::action, ToolStationActionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Server-side handler. Validates the active menu + pos + reach, then asks the BE to
     * re-resolve its output. The build / modify discriminant currently selects between two
     * BE consumer methods (build commits the input-derived tool; modify clears slot 0). The
     * {@link Action#RENAME} variant is reserved — the rename UI lands in a follow-up ticket
     * carrying the text payload separately.
     */
    public static void handleServer(ToolStationActionPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player.containerMenu instanceof ToolStationMenu menu)) {
            return;
        }
        ToolStationBlockEntity station = menu.getBlockEntity();
        if (station == null || !station.getBlockPos().equals(payload.pos())) {
            return;
        }
        if (!menu.stillValid(player)) {
            return;
        }
        switch (payload.action()) {
        case BUILD:
            // BUILD commits the current input-derived output — server-side refreshOutput
            // is idempotent and already runs on every input-change; calling it here is the
            // explicit "rebuild now" hook the future automation API (hopper insert, redstone
            // pulse) hangs on.
            station.refreshOutputFromAction();
            break;
        case MODIFY:
            // MODIFY consumes slot 0's existing built tool (the input-tool sentinel) and
            // re-stamps the output. Currently the BE's consumeModifyInput is the load-bearing
            // step.
            station.consumeModifyInput();
            break;
        case RENAME:
            // RENAME is reserved for the in-progress rename UI follow-up; the payload is
            // accepted on the wire but currently no-ops on the server.
            break;
        }
    }

    /**
     * Discriminator for the three button-driven actions a tool station / forge can take.
     * Persisted on the wire as a varint ordinal through {@link #STREAM_CODEC}; reordering the
     * constants is a wire-protocol break and forbidden.
     */
    public enum Action {
        BUILD, MODIFY, RENAME;

        /** Cached enum array — {@link Enum#values()} allocates a new array on every call, and
         *  this codec runs on every inbound payload decode. */
        private static final Action[] VALUES = values();

        /** Stream codec gated against {@link #VALUES}'s ordinal range. Out-of-range values
         *  surface as decoder errors during read, which the payload pipeline upgrades to a
         *  client disconnect — exactly the "disconnects on malformed payload" AC. */
        public static final StreamCodec<io.netty.buffer.ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(ordinal -> {
            if (ordinal < 0 || ordinal >= VALUES.length) {
                throw new IllegalArgumentException("ToolStationActionPayload.Action ordinal out of range: " + ordinal);
            }
            return VALUES[ordinal];
        }, Action::ordinal);
    }
}
