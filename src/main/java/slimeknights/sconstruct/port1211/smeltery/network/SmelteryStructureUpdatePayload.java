package slimeknights.sconstruct.port1211.smeltery.network;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryControllerBlockEntity;

import io.netty.buffer.ByteBuf;

/**
 * Server&rarr;client payload syncing a smeltery's assembled shape to clients tracking its chunk
 * (SMTCON-124). Carries the controller {@link BlockPos} and the interior {@link BoundingBox} of
 * the assembled smeltery — or {@link Optional#empty()} when the controller is not assembled — so
 * a client can render the molten-bowl outline.
 *
 * @param controller the position of the smeltery controller this update is for
 * @param bounds     the interior bounding box of the assembled smeltery, or empty when unassembled
 */
public record SmelteryStructureUpdatePayload(BlockPos controller, Optional<BoundingBox> bounds) implements CustomPacketPayload {

    /** Channel id under the mod namespace. */
    public static final CustomPacketPayload.Type<SmelteryStructureUpdatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "smeltery_structure_update"));

    /** Wire codec for a {@link BoundingBox} — its six corner coordinates as fixed-width ints. */
    private static final StreamCodec<ByteBuf, BoundingBox> BOX_CODEC = StreamCodec.composite(ByteBufCodecs.INT, BoundingBox::minX, ByteBufCodecs.INT, BoundingBox::minY, ByteBufCodecs.INT,
            BoundingBox::minZ, ByteBufCodecs.INT, BoundingBox::maxX, ByteBufCodecs.INT, BoundingBox::maxY, ByteBufCodecs.INT, BoundingBox::maxZ, BoundingBox::new);

    /** Network codec — the controller position followed by the optional interior bounding box. */
    public static final StreamCodec<RegistryFriendlyByteBuf, SmelteryStructureUpdatePayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, SmelteryStructureUpdatePayload::controller,
            ByteBufCodecs.optional(BOX_CODEC), SmelteryStructureUpdatePayload::bounds, SmelteryStructureUpdatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Client-side handler. Applies the synced interior bounds to the client copy of the smeltery
     * controller block-entity so the molten-bowl outline renders the updated shape. The work is
     * enqueued onto the main client thread, where reading the level and mutating the
     * block-entity is safe.
     */
    public static void handleClient(SmelteryStructureUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockEntity be = context.player().level().getBlockEntity(payload.controller());
            if (be instanceof SmelteryControllerBlockEntity controller) {
                controller.applyStructureUpdate(payload.bounds());
            }
        });
    }
}
