package slimeknights.sconstruct.smeltery.network;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.smeltery.block.entity.SmelteryControllerBlockEntity;

/**
 * Server&rarr;client payload syncing a smeltery's molten contents to clients tracking its chunk
 * (SMTCON-124). Carries the controller {@link BlockPos} and the tank's fluid stacks, so a client
 * can render the smeltery's fill level and tank UI without the server resending the controller's
 * full block-entity NBT every change.
 *
 * @param controller the position of the smeltery controller this update is for
 * @param contents   the fluid stacks currently held in the smeltery tank
 */
public record SmelteryFluidUpdatePayload(BlockPos controller, List<FluidStack> contents) implements CustomPacketPayload {

    /** Channel id under the mod namespace. */
    public static final CustomPacketPayload.Type<SmelteryFluidUpdatePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "smeltery_fluid_update"));

    /** Network codec — the controller position followed by the length-prefixed fluid list. */
    public static final StreamCodec<RegistryFriendlyByteBuf, SmelteryFluidUpdatePayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, SmelteryFluidUpdatePayload::controller,
            FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()), SmelteryFluidUpdatePayload::contents, SmelteryFluidUpdatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Client-side handler. Applies the synced fluid contents to the client copy of the smeltery
     * controller block-entity so the next frame renders the updated tank. The work is enqueued
     * onto the main client thread, where reading the level and mutating the block-entity is safe.
     */
    public static void handleClient(SmelteryFluidUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockEntity be = context.player().level().getBlockEntity(payload.controller());
            if (be instanceof SmelteryControllerBlockEntity controller) {
                controller.applyFluidUpdate(payload.contents());
            }
        });
    }
}
