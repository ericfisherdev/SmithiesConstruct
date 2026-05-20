package slimeknights.sconstruct.smeltery.client;

import java.util.Optional;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import slimeknights.sconstruct.common.config.ClientConfig;
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

    /**
     * Draws the molten metal pooled in the bowl as stacked horizontal layers — one per fluid in
     * the multi-fluid tank (SMTCON-221). Layers are emitted bottom-up in tank insertion order so
     * the visual stack matches the in-game intuition that the first fluid in is the deepest.
     * Each layer's height is proportional to its share of the tank capacity; the running Y
     * offset is shared across consecutive layers so no horizontal seam is rendered between them.
     */
    private static void renderFluid(SmelteryControllerBlockEntity controller, BoundingBox bounds, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        // The smeltery's SmelteryFluidTank exposes a single shared capacity across every virtual
        // tank slot (all entries share the bowl's total pool), so reading tank 0's capacity is
        // sufficient here; the iteration below uses that same value for every layer's fraction.
        IFluidHandler handler = controller.getFluidHandler();
        int capacity = handler.getTankCapacity(0);
        if (capacity <= 0) {
            return;
        }
        int tankCount = handler.getTanks();
        AABB box = interiorBox(controller, bounds);
        float minY = (float) box.minY + LAYER_INSET;
        float maxY = (float) box.maxY - LAYER_INSET;
        float usableHeight = maxY - minY;
        if (usableHeight <= MIN_RENDERABLE_HEIGHT) {
            return;
        }
        float runningY = minY;
        for (int tank = 0; tank < tankCount; tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (fluid.isEmpty()) {
                continue;
            }
            float fillFraction = Math.clamp((float) fluid.getAmount() / capacity, 0.0F, 1.0F);
            float layerHeight = usableHeight * fillFraction;
            if (layerHeight <= MIN_RENDERABLE_HEIGHT) {
                continue;
            }
            float layerTop = Math.min(runningY + layerHeight, maxY);
            FluidRenderer.renderLayer(poseStack, buffers, box, fluid, runningY, layerTop, packedLight);
            runningY = layerTop;
            if (runningY >= maxY) {
                break;
            }
        }
    }

    /**
     * Vertical inset applied at the very bottom and very top of the fluid stack — keeps the
     * deepest layer's floor and the topmost layer's surface from z-fighting against the bowl's
     * seared blocks. The inset is <em>not</em> applied between layers, so consecutive fluids
     * share their Y boundary cleanly without a visible seam.
     */
    private static final float LAYER_INSET = 0.01F;

    /** Layers thinner than this contribute no visible pixels and are skipped entirely. */
    private static final float MIN_RENDERABLE_HEIGHT = 0.0F;

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
        int slotCount = meltingSlots.getSlots();
        if (slotCount <= 0) {
            return;
        }
        BlockPos origin = controller.getBlockPos();
        Level level = controller.getLevel();
        int width = bounds.maxX() - bounds.minX() + 1;
        int depth = bounds.maxZ() - bounds.minZ() + 1;
        int layerArea = width * depth;
        long gameTime = level == null ? 0L : level.getGameTime();
        float spin = (gameTime + partialTick) * SPIN_DEGREES_PER_TICK;
        // Rotate the loop's starting index per frame (SMTCON-229) so a smeltery whose contents
        // exceed the quad budget cycles which items it shows rather than always rendering the
        // first N. The rotation uses gameTime directly — a per-frame nudge would shimmer too
        // fast to be readable; once per game tick (20 Hz) is the same cadence TC's renderer uses.
        int startOffset = (int) Math.floorMod(gameTime, slotCount);
        int quadBudget = ClientConfig.MAX_SMELTERY_ITEM_QUADS.get();
        int quadsRendered = 0;
        RandomSource quadRandom = RandomSource.create(SLOT_QUAD_COUNT_SEED);
        for (int step = 0; step < slotCount; step++) {
            int slot = (startOffset + step) % slotCount;
            ItemStack stack = meltingSlots.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int cellY = bounds.minY() + slot / layerArea;
            if (cellY > bounds.maxY()) {
                // More items than interior cells — should not happen once the inventory is sized
                // to the bowl, but guard so a stray slot is not drawn outside the structure.
                continue;
            }
            int stackQuads = quadCountFor(stack, level, quadRandom);
            if (quadsRendered > 0 && quadsRendered + stackQuads > quadBudget) {
                // Budget exhausted for this frame. Subsequent slots will get their turn on later
                // frames thanks to the rotation cursor; render at least the first item so a bowl
                // with one over-budget model still shows something.
                break;
            }
            quadsRendered += stackQuads;
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
     * Approximate quad count for {@code stack}'s baked model — summed over the six face buckets
     * plus the no-direction bucket. Used by the SMTCON-229 budget check to decide whether the
     * next item fits before submitting it to the buffer source. A model that throws on quad
     * resolution (mod-broken asset, missing texture) falls back to a per-item conservative
     * estimate so one bad model does not starve the rest of the budget.
     */
    private int quadCountFor(ItemStack stack, Level level, RandomSource random) {
        try {
            BakedModel model = itemRenderer.getModel(stack, level, null, 0);
            int total = model.getQuads(null, null, random).size();
            for (Direction direction : Direction.values()) {
                total += model.getQuads(null, direction, random).size();
            }
            return Math.max(1, total);
        }
        catch (IndexOutOfBoundsException | IllegalStateException | IllegalArgumentException broken) {
            // A mod-side model throwing during quad resolution must not kill the whole render
            // pass — these three are the realistic failure modes (malformed quad lists, registry
            // race conditions, bad face-bucket lookups). Fall back to a fixed estimate so the
            // budget arithmetic stays sane. NPE is intentionally not caught — that's a Smithies'
            // bug to fix at source, not to paper over here.
            return FALLBACK_ITEM_QUADS;
        }
    }

    /** Deterministic seed for the {@link RandomSource} fed into {@code BakedModel.getQuads}. */
    private static final long SLOT_QUAD_COUNT_SEED = 42L;

    /** Per-item quad estimate used when a model's {@code getQuads} call throws. */
    private static final int FALLBACK_ITEM_QUADS = 12;

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
