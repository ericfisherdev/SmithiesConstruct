package slimeknights.sconstruct.port1211.smeltery.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryControllerBlockEntity;

/**
 * Server&rarr;client payload syncing a smeltery's heat to clients tracking its chunk
 * (SMTCON-124). Carries the controller {@link BlockPos}, the current internal temperature, and
 * the temperature the active fuel can sustain — so a client can render the heat gauge.
 *
 * @param controller  the position of the smeltery controller this update is for
 * @param currentTemp the smeltery's current internal temperature in kelvin
 * @param targetTemp  the temperature in kelvin the active fuel source can sustain
 */
public record SmelteryFuelUpdatePayload(BlockPos controller, int currentTemp, int targetTemp) implements CustomPacketPayload {

    /** Channel id under the mod namespace. */
    public static final CustomPacketPayload.Type<SmelteryFuelUpdatePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "smeltery_fuel_update"));

    /** Network codec — the controller position followed by the two temperatures. */
    public static final StreamCodec<RegistryFriendlyByteBuf, SmelteryFuelUpdatePayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, SmelteryFuelUpdatePayload::controller,
            ByteBufCodecs.VAR_INT, SmelteryFuelUpdatePayload::currentTemp, ByteBufCodecs.VAR_INT, SmelteryFuelUpdatePayload::targetTemp, SmelteryFuelUpdatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Client-side handler. Applies the synced temperatures to the client copy of the smeltery
     * controller block-entity so the heat gauge renders the updated value. The work is enqueued
     * onto the main client thread, where reading the level and mutating the block-entity is safe.
     */
    public static void handleClient(SmelteryFuelUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockEntity be = context.player().level().getBlockEntity(payload.controller());
            if (be instanceof SmelteryControllerBlockEntity controller) {
                controller.applyFuelUpdate(payload.currentTemp(), payload.targetTemp());
            }
        });
    }
}
