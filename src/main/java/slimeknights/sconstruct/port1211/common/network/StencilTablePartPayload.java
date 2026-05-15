package slimeknights.sconstruct.port1211.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.tools.PartType;
import slimeknights.sconstruct.port1211.tools.block.entity.StencilTableBlockEntity;
import slimeknights.sconstruct.port1211.tools.inventory.StencilTableMenu;

/**
 * Client→server payload for the Stencil Table's cycle button (SMTCON-91). Carries the BE's
 * {@link BlockPos} so the server can locate the stencil table the player is interacting with,
 * plus a signed {@code direction} field ({@code -1} for previous, {@code +1} for next) that
 * the server uses to step the cursor through {@link PartType#values()} with wraparound.
 *
 * <p>Wire format: a {@link BlockPos} (long-packed) followed by a varint signed direction. The
 * direction is allowed to be any non-zero integer — server-side handling reduces it modulo the
 * enum length so a future "skip to last" button can reuse this payload with a larger step.
 *
 * <p>Registered on the play-to-server channel via {@link #register} from
 * {@link slimeknights.sconstruct.port1211.common.TinkerNetwork#configure}.
 */
public record StencilTablePartPayload(BlockPos pos, int direction) implements CustomPacketPayload {

    /** Channel id under the mod namespace. */
    public static final CustomPacketPayload.Type<StencilTablePartPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "stencil_table_part"));

    /**
     * Network codec. {@link BlockPos#STREAM_CODEC} is the standard long-packed encoding;
     * {@link ByteBufCodecs#VAR_INT} keeps the direction wire-compact (one byte for the typical
     * ±1 values).
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, StencilTablePartPayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, StencilTablePartPayload::pos, ByteBufCodecs.VAR_INT,
            StencilTablePartPayload::direction, StencilTablePartPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Server-side handler. Looks up the BE at {@link #pos} in the player's level, verifies it
     * is a {@link StencilTableBlockEntity}, computes the new {@link PartType} via {@link
     * #stepPart}, and asks the BE to mutate its cursor.
     *
     * <p>Runs on the main server thread — NeoForge's {@link IPayloadContext#enqueueWork} is
     * <em>not</em> needed because we declare this handler as the
     * {@link net.neoforged.neoforge.network.registration.PayloadRegistrar#playToServer}
     * variant which is dispatched on the server tick by default in 1.21.1.
     */
    // Level / ServerLevel are AutoCloseable in the type system, but the world instance is owned
    // by the server lifecycle — not by an inbound packet handler. Closing it here would tear
    // down every player's world.
    @SuppressWarnings("PMD.CloseResource")
    public static void handleServer(StencilTablePartPayload payload, IPayloadContext context) {
        Player player = context.player();
        // The player must have the Stencil Table menu open AND that menu must reference the
        // BE at the supplied pos. Without this guard, a rogue client could send a packet for
        // any stencil table in the world and rotate its cursor.
        if (!(player.containerMenu instanceof StencilTableMenu menu)) {
            return;
        }
        StencilTableBlockEntity table = menu.getBlockEntity();
        if (table == null || !table.getBlockPos().equals(payload.pos())) {
            return;
        }
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            // Belt-and-braces — the server channel should never deliver a non-ServerLevel.
            return;
        }
        // Reach check — the player must still be within interaction range, since a menu can
        // remain open across teleports. AbstractContainerMenu#stillValid uses 64 (8 blocks)
        // as the standard distance squared.
        if (!menu.stillValid(player)) {
            return;
        }
        PartType nextPart = stepPart(table.getSelectedPart(), payload.direction());
        table.setSelectedPart(nextPart, serverLevel);
    }

    /**
     * Compute the new {@link PartType} cursor by stepping the enum array. Wraparound on both
     * ends — any non-zero direction reduced modulo {@code values().length}.
     *
     * <p>Package-private so the unit-test can drive the stepping math directly without spinning
     * up a server or a BE.
     */
    public static PartType stepPart(PartType current, int direction) {
        PartType[] values = PartType.values();
        int len = values.length;
        // Java's % can return a negative for negative dividends — normalise to [0, len).
        int newOrdinal = ((current.ordinal() + direction) % len + len) % len;
        return values[newOrdinal];
    }
}
