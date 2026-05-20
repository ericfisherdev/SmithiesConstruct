package slimeknights.sconstruct.smeltery.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Renders a body of fluid as a box of textured quads (SMTCON-126) — the helper behind the
 * smeltery's in-bowl fluid display. Drawing the fluid as block-entity-renderer geometry rather
 * than placing fake {@code LiquidBlock}s keeps the world's block grid clean (no spurious
 * neighbour updates) and lets the level rise and fall smoothly between integer block heights.
 *
 * <p>The fluid's still texture and tint are read from its {@link IClientFluidTypeExtensions};
 * because the texture is resolved through the block atlas, an animated still texture animates
 * here for free.
 */
public final class FluidRenderer {

    /** Slight inset, in blocks, that keeps the fluid quads off the bowl walls to avoid z-fighting. */
    private static final float INSET = 0.01F;

    /** Opaque-alpha bits OR-ed onto a fluid tint so a tint with no alpha channel still draws. */
    private static final int OPAQUE_ALPHA = 0xFF000000;

    private FluidRenderer() {
    }

    /**
     * The shared per-draw state threaded through the quad helpers — the buffer being written,
     * the transform, the packed light, the ARGB tint, and the still sprite's UV rectangle.
     */
    private record QuadContext(VertexConsumer consumer, Matrix4f matrix, int light, int color, float u0, float u1, float v0, float v1) {
    }

    /** One of the five fluid faces — the top surface and the four inner walls. */
    private enum Face {
        TOP, WEST, EAST, NORTH, SOUTH
    }

    /** The five faces, cached so the per-frame render loop allocates no {@code values()} array. */
    private static final Face[] FACES = Face.values();

    /**
     * Submits the fluid surface and side quads filling {@code box} up to {@code fillFraction} of
     * its height. Five quads are drawn — the top surface plus the four inner walls — inset
     * slightly from {@code box} so they never co-planar-fight the smeltery's seared walls.
     *
     * @param poseStack    the current pose, already translated to the block-entity origin
     * @param buffers      the buffer source to draw into
     * @param box          the interior volume to fill, in block-entity-relative coordinates
     * @param fluid        the fluid to draw; an empty stack draws nothing
     * @param fillFraction how full the box is, clamped to {@code [0, 1]}
     * @param light        the packed light value to render the quads at
     */
    public static void renderInsideBox(PoseStack poseStack, MultiBufferSource buffers, AABB box, FluidStack fluid, float fillFraction, int light) {
        if (fluid.isEmpty() || fillFraction <= 0.0F) {
            return;
        }
        float fraction = Math.clamp(fillFraction, 0.0F, 1.0F);
        float minY = (float) box.minY + INSET;
        float topY = minY + (float) (box.maxY - box.minY - 2 * INSET) * fraction;
        renderLayer(poseStack, buffers, box, fluid, minY, topY, light);
    }

    /**
     * Submits one explicit-Y-range fluid layer (SMTCON-221) — the multi-alloy primitive the
     * smeltery renderer stacks to visualise several molten metals at once. Unlike
     * {@link #renderInsideBox} this caller controls both the bottom and top of the layer
     * directly, so consecutive layers can share a Y boundary without the bottom-and-top
     * {@link #INSET} that {@code renderInsideBox} bakes in (which would otherwise leave a
     * visible seam between layers).
     *
     * <p>The X/Z inset still applies — the layer is drawn flush against the bowl interior on
     * the horizontal faces, then inset away from the seared walls to avoid z-fighting.
     *
     * @param poseStack the current pose, already translated to the block-entity origin
     * @param buffers   the buffer source to draw into
     * @param box       the bowl bounds, in block-entity-relative coordinates — supplies the X/Z
     *                  extents only; the Y range comes from {@code minY}/{@code topY}
     * @param fluid     the fluid to draw; an empty stack draws nothing
     * @param minY      the bottom of the layer in block-entity-relative coordinates
     * @param topY      the top of the layer in block-entity-relative coordinates
     * @param light     the packed light value to render the quads at
     */
    public static void renderLayer(PoseStack poseStack, MultiBufferSource buffers, AABB box, FluidStack fluid, float minY, float topY, int light) {
        if (fluid.isEmpty() || topY <= minY) {
            return;
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
        int color = OPAQUE_ALPHA | extensions.getTintColor(fluid);

        float minX = (float) box.minX + INSET;
        float minZ = (float) box.minZ + INSET;
        float maxX = (float) box.maxX - INSET;
        float maxZ = (float) box.maxZ - INSET;

        QuadContext ctx = new QuadContext(buffers.getBuffer(RenderType.translucent()), poseStack.last().pose(), light, color, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());

        // The top surface plus the four inner walls — each quad's corners are derived from the
        // box extents inside quad(), so the per-frame render path allocates no scratch arrays.
        for (Face face : FACES) {
            quad(ctx, face, minX, maxX, minZ, maxZ, minY, topY);
        }
    }

    /**
     * Emits the four-vertex quad for {@code face}, deriving its corners and normal from the
     * fluid-box extents. The top surface sits at {@code topY}; each wall runs from {@code minY}
     * up to {@code topY} and faces the bowl interior. Corners are computed inline, so the
     * per-frame render path allocates nothing.
     */
    private static void quad(QuadContext ctx, Face face, float minX, float maxX, float minZ, float maxZ, float minY, float topY) {
        switch (face) {
        case TOP -> {
            vertex(ctx, minX, topY, minZ, ctx.u0(), ctx.v1(), 0.0F, 1.0F, 0.0F);
            vertex(ctx, minX, topY, maxZ, ctx.u1(), ctx.v1(), 0.0F, 1.0F, 0.0F);
            vertex(ctx, maxX, topY, maxZ, ctx.u1(), ctx.v0(), 0.0F, 1.0F, 0.0F);
            vertex(ctx, maxX, topY, minZ, ctx.u0(), ctx.v0(), 0.0F, 1.0F, 0.0F);
        }
        case WEST -> {
            vertex(ctx, minX, minY, minZ, ctx.u0(), ctx.v1(), 1.0F, 0.0F, 0.0F);
            vertex(ctx, minX, minY, maxZ, ctx.u1(), ctx.v1(), 1.0F, 0.0F, 0.0F);
            vertex(ctx, minX, topY, maxZ, ctx.u1(), ctx.v0(), 1.0F, 0.0F, 0.0F);
            vertex(ctx, minX, topY, minZ, ctx.u0(), ctx.v0(), 1.0F, 0.0F, 0.0F);
        }
        case EAST -> {
            vertex(ctx, maxX, minY, maxZ, ctx.u0(), ctx.v1(), -1.0F, 0.0F, 0.0F);
            vertex(ctx, maxX, minY, minZ, ctx.u1(), ctx.v1(), -1.0F, 0.0F, 0.0F);
            vertex(ctx, maxX, topY, minZ, ctx.u1(), ctx.v0(), -1.0F, 0.0F, 0.0F);
            vertex(ctx, maxX, topY, maxZ, ctx.u0(), ctx.v0(), -1.0F, 0.0F, 0.0F);
        }
        case NORTH -> {
            vertex(ctx, maxX, minY, minZ, ctx.u0(), ctx.v1(), 0.0F, 0.0F, 1.0F);
            vertex(ctx, minX, minY, minZ, ctx.u1(), ctx.v1(), 0.0F, 0.0F, 1.0F);
            vertex(ctx, minX, topY, minZ, ctx.u1(), ctx.v0(), 0.0F, 0.0F, 1.0F);
            vertex(ctx, maxX, topY, minZ, ctx.u0(), ctx.v0(), 0.0F, 0.0F, 1.0F);
        }
        case SOUTH -> {
            vertex(ctx, minX, minY, maxZ, ctx.u0(), ctx.v1(), 0.0F, 0.0F, -1.0F);
            vertex(ctx, maxX, minY, maxZ, ctx.u1(), ctx.v1(), 0.0F, 0.0F, -1.0F);
            vertex(ctx, maxX, topY, maxZ, ctx.u1(), ctx.v0(), 0.0F, 0.0F, -1.0F);
            vertex(ctx, minX, topY, maxZ, ctx.u0(), ctx.v0(), 0.0F, 0.0F, -1.0F);
        }
        }
    }

    /** Emits one quad vertex in the {@code BLOCK} vertex format used by the translucent layer. */
    private static void vertex(QuadContext ctx, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        ctx.consumer().addVertex(ctx.matrix(), x, y, z).setColor(ctx.color()).setUv(u, v).setLight(ctx.light()).setNormal(nx, ny, nz);
    }
}
