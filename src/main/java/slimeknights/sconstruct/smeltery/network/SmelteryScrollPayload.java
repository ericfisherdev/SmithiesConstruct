package slimeknights.sconstruct.smeltery.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.smeltery.inventory.SmelteryControllerMenu;

/**
 * Client&rarr;server payload reporting that the player scrolled the smeltery controller's
 * melting-slot window (SMTCON-216). The melting inventory can be larger than the GUI's visible
 * grid, so the screen scrolls a window over it; the visible window must agree on both sides, or
 * a shift-click or slot click would land on the wrong inventory slot. The screen sends this
 * whenever its scroll offset changes.
 *
 * @param containerId the id of the menu being scrolled — guards a stale packet from a closed menu
 * @param scrollRow   the first melting-grid row the window should show
 */
public record SmelteryScrollPayload(int containerId, int scrollRow) implements CustomPacketPayload {

    /** Channel id under the mod namespace. */
    public static final CustomPacketPayload.Type<SmelteryScrollPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "smeltery_scroll"));

    /** Network codec — the container id followed by the scroll row. */
    public static final StreamCodec<RegistryFriendlyByteBuf, SmelteryScrollPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, SmelteryScrollPayload::containerId,
            ByteBufCodecs.VAR_INT, SmelteryScrollPayload::scrollRow, SmelteryScrollPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Server-side handler. Applies the scroll offset to the player's open controller menu so the
     * server's visible window matches the client's. {@link SmelteryControllerMenu#setScrollRow}
     * clamps the value, so an out-of-range row from a malformed packet cannot misplace the window.
     */
    public static void handleServer(SmelteryScrollPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof SmelteryControllerMenu menu && menu.containerId == payload.containerId()) {
                menu.setScrollRow(payload.scrollRow());
            }
        });
    }
}
