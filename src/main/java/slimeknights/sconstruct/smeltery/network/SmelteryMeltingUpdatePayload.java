package slimeknights.sconstruct.smeltery.network;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.smeltery.block.entity.SmelteryControllerBlockEntity;

/**
 * Server&rarr;client payload syncing a smeltery's melting-slot contents to clients tracking its
 * chunk (SMTCON-214). Carries the controller {@link BlockPos} and one {@link ItemStack} per
 * melting slot, so a client can draw the items currently being melted floating inside the
 * smeltery interior. It mirrors {@link SmelteryFluidUpdatePayload}: the SMTCON-124 delta-sync
 * style, sent only when the slot contents change, rather than resending the controller's whole
 * block-entity NBT.
 *
 * @param controller the position of the smeltery controller this update is for
 * @param items      the stacks in the controller's melting slots, slot-indexed; empties included
 */
public record SmelteryMeltingUpdatePayload(BlockPos controller, List<ItemStack> items) implements CustomPacketPayload {

    /** Channel id under the mod namespace. */
    public static final CustomPacketPayload.Type<SmelteryMeltingUpdatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "smeltery_melting_update"));

    /** Network codec — the controller position followed by the length-prefixed slot list. */
    public static final StreamCodec<RegistryFriendlyByteBuf, SmelteryMeltingUpdatePayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, SmelteryMeltingUpdatePayload::controller,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), SmelteryMeltingUpdatePayload::items, SmelteryMeltingUpdatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Client-side handler. Applies the synced slot contents to the client copy of the smeltery
     * controller block-entity so the next frame renders the items being melted. The work is
     * enqueued onto the main client thread, where reading the level and mutating the
     * block-entity is safe.
     */
    public static void handleClient(SmelteryMeltingUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockEntity be = context.player().level().getBlockEntity(payload.controller());
            if (be instanceof SmelteryControllerBlockEntity controller) {
                controller.applyMeltingUpdate(payload.items());
            }
        });
    }
}
