package slimeknights.sconstruct.smeltery.client;

import java.util.Optional;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import slimeknights.sconstruct.smeltery.block.entity.SmelteryControllerBlockEntity;

/**
 * {@link BlockEntityRenderer} for the smeltery controller (SMTCON-126 / SMTCON-214) — draws the
 * molten metal pooled inside an assembled smeltery's bowl, and the items currently being melted
 * floating above it.
 *
 * <p>The fluid level is derived from the controller's tank fill, so it rises and falls smoothly
 * as metal is melted in or drained out. The melting items are read from the controller's
 * melting-slot inventory (kept current on the client by {@code SmelteryMeltingUpdatePayload})
 * and laid out one per interior cell, slowly rotating so a loaded smeltery reads as busy.
 *
 * <p>The renderer draws nothing for an unassembled controller.
 */
public class SmelteryRenderer implements BlockEntityRenderer<SmelteryControllerBlockEntity> {

    /** Edge length the melting items are scaled to — small enough that one sits clear inside a cell. */
    private static final float ITEM_SCALE = 0.625F;

    /** Degrees per game tick the melting items spin about their vertical axis. */
    private static final float SPIN_DEGREES_PER_TICK = 2.0F;

    private final ItemRenderer itemRenderer;

    public SmelteryRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SmelteryControllerBlockEntity controller, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Optional<BoundingBox> bounds = controller.getRenderBounds();
        if (bounds.isEmpty()) {
            return;
        }
        renderFluid(controller, bounds.get(), poseStack, buffers, packedLight);
        renderMeltingItems(controller, bounds.get(), partialTick, poseStack, buffers, packedLight, packedOverlay);
    }

    /** Draws the molten metal pooled in the bowl, scaled to the tank's fill fraction. */
    private static void renderFluid(SmelteryControllerBlockEntity controller, BoundingBox bounds, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        FluidStack fluid = controller.getFluidHandler().getFluidInTank(0);
        if (fluid.isEmpty()) {
            return;
        }
        int capacity = controller.getFluidHandler().getTankCapacity(0);
        if (capacity <= 0) {
            return;
        }
        float fill = Math.clamp((float) fluid.getAmount() / capacity, 0.0F, 1.0F);
        FluidRenderer.renderInsideBox(poseStack, buffers, interiorBox(controller, bounds), fluid, fill, packedLight);
    }

    /**
     * Draws each non-empty melting slot as an item floating at the centre of its interior cell.
     * Slot {@code i} maps to the {@code i}-th interior cell in row-major (x, then z, then y)
     * order — the smeltery sizes its melting inventory to the interior volume, so the mapping is
     * one-to-one. Items spin slowly about the vertical axis.
     */
    // Level is AutoCloseable in the type system, but the world is owned by the client, not by
    // this renderer — PMD's CloseResource heuristic does not model that.
    @SuppressWarnings("PMD.CloseResource")
    private void renderMeltingItems(SmelteryControllerBlockEntity controller, BoundingBox bounds, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            int packedOverlay) {
        IItemHandler meltingSlots = controller.getItemHandler();
        BlockPos origin = controller.getBlockPos();
        Level level = controller.getLevel();
        int width = bounds.maxX() - bounds.minX() + 1;
        int depth = bounds.maxZ() - bounds.minZ() + 1;
        int layerArea = width * depth;
        float spin = ((level == null ? 0L : level.getGameTime()) + partialTick) * SPIN_DEGREES_PER_TICK;
        for (int slot = 0; slot < meltingSlots.getSlots(); slot++) {
            ItemStack stack = meltingSlots.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int cellY = bounds.minY() + slot / layerArea;
            if (cellY > bounds.maxY()) {
                // More items than interior cells — should not happen once the inventory is sized
                // to the bowl, but guard so a stray slot is not drawn outside the structure.
                break;
            }
            int cellX = bounds.minX() + slot % width;
            int cellZ = bounds.minZ() + slot / width % depth;
            poseStack.pushPose();
            poseStack.translate(cellX + 0.5 - origin.getX(), cellY + 0.5 - origin.getY(), cellZ + 0.5 - origin.getZ());
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, packedOverlay, poseStack, buffers, level, slot);
            poseStack.popPose();
        }
    }

    /**
     * Reports the assembled interior as the renderer's bounding box so the fluid and items are
     * not frustum-culled when the controller block itself drifts off-screen.
     */
    @Override
    public AABB getRenderBoundingBox(SmelteryControllerBlockEntity controller) {
        Optional<BoundingBox> bounds = controller.getRenderBounds();
        return bounds.isPresent() ? worldBox(bounds.get()) : BlockEntityRenderer.super.getRenderBoundingBox(controller);
    }

    /** The interior bounding box translated into block-entity-relative coordinates for rendering. */
    private static AABB interiorBox(SmelteryControllerBlockEntity controller, BoundingBox bounds) {
        BlockPos origin = controller.getBlockPos();
        return new AABB(bounds.minX() - origin.getX(), bounds.minY() - origin.getY(), bounds.minZ() - origin.getZ(), bounds.maxX() + 1.0 - origin.getX(), bounds.maxY() + 1.0 - origin.getY(),
                bounds.maxZ() + 1.0 - origin.getZ());
    }

    /** The interior bounding box as a world-space {@link AABB} spanning every interior block. */
    private static AABB worldBox(BoundingBox bounds) {
        return new AABB(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX() + 1.0, bounds.maxY() + 1.0, bounds.maxZ() + 1.0);
    }
}
