package slimeknights.sconstruct.smeltery.client;

import java.util.Optional;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.blaze3d.vertex.PoseStack;

import slimeknights.sconstruct.smeltery.block.entity.SmelteryControllerBlockEntity;

/**
 * {@link BlockEntityRenderer} for the smeltery controller (SMTCON-126) — draws the molten metal
 * pooled inside an assembled smeltery's bowl. The fluid level is derived from the controller's
 * tank fill, so it rises and falls smoothly as metal is melted in or drained out, and the body
 * of fluid is drawn as renderer geometry by {@link FluidRenderer} rather than placed as fake
 * blocks in the world.
 *
 * <p>The renderer draws nothing for an unassembled controller or an empty tank.
 */
public class SmelteryRenderer implements BlockEntityRenderer<SmelteryControllerBlockEntity> {

    public SmelteryRenderer(BlockEntityRendererProvider.Context context) {
        // No per-instance render state — the renderer reads everything off the block-entity.
    }

    @Override
    public void render(SmelteryControllerBlockEntity controller, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Optional<BoundingBox> bounds = controller.getRenderBounds();
        if (bounds.isEmpty()) {
            return;
        }
        FluidStack fluid = controller.getFluidHandler().getFluidInTank(0);
        if (fluid.isEmpty()) {
            return;
        }
        int capacity = controller.getFluidHandler().getTankCapacity(0);
        if (capacity <= 0) {
            return;
        }
        float fill = Math.clamp((float) fluid.getAmount() / capacity, 0.0F, 1.0F);
        FluidRenderer.renderInsideBox(poseStack, buffers, interiorBox(controller, bounds.get()), fluid, fill, packedLight);
    }

    /**
     * Reports the assembled interior as the renderer's bounding box so the fluid is not frustum-
     * culled when the controller block itself drifts off-screen.
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
