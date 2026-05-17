package slimeknights.sconstruct.port1211.smeltery.client;

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
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
        int color = OPAQUE_ALPHA | extensions.getTintColor(fluid);

        float fraction = Math.clamp(fillFraction, 0.0F, 1.0F);
        float minX = (float) box.minX + INSET;
        float minZ = (float) box.minZ + INSET;
        float maxX = (float) box.maxX - INSET;
        float maxZ = (float) box.maxZ - INSET;
        float minY = (float) box.minY + INSET;
        float topY = minY + (float) (box.maxY - box.minY - 2 * INSET) * fraction;

        QuadContext ctx = new QuadContext(buffers.getBuffer(RenderType.translucent()), poseStack.last().pose(), light, color, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());

        // Top surface — the fluid level the player looks down on.
        quad(ctx, new float[] { minX, topY, minZ, minX, topY, maxZ, maxX, topY, maxZ, maxX, topY, minZ }, 0.0F, 1.0F, 0.0F);
        // Four inner walls, drawn from the floor up to the surface, each facing the bowl interior.
        quad(ctx, new float[] { minX, minY, minZ, minX, minY, maxZ, minX, topY, maxZ, minX, topY, minZ }, 1.0F, 0.0F, 0.0F);
        quad(ctx, new float[] { maxX, minY, maxZ, maxX, minY, minZ, maxX, topY, minZ, maxX, topY, maxZ }, -1.0F, 0.0F, 0.0F);
        quad(ctx, new float[] { maxX, minY, minZ, minX, minY, minZ, minX, topY, minZ, maxX, topY, minZ }, 0.0F, 0.0F, 1.0F);
        quad(ctx, new float[] { minX, minY, maxZ, maxX, minY, maxZ, maxX, topY, maxZ, minX, topY, maxZ }, 0.0F, 0.0F, -1.0F);
    }

    /** Emits one four-vertex quad whose corners are the twelve floats of {@code corners}. */
    private static void quad(QuadContext ctx, float[] corners, float nx, float ny, float nz) {
        vertex(ctx, corners[0], corners[1], corners[2], ctx.u0(), ctx.v1(), nx, ny, nz);
        vertex(ctx, corners[3], corners[4], corners[5], ctx.u1(), ctx.v1(), nx, ny, nz);
        vertex(ctx, corners[6], corners[7], corners[8], ctx.u1(), ctx.v0(), nx, ny, nz);
        vertex(ctx, corners[9], corners[10], corners[11], ctx.u0(), ctx.v0(), nx, ny, nz);
    }

    /** Emits one quad vertex in the {@code BLOCK} vertex format used by the translucent layer. */
    private static void vertex(QuadContext ctx, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        ctx.consumer().addVertex(ctx.matrix(), x, y, z).setColor(ctx.color()).setUv(u, v).setLight(ctx.light()).setNormal(nx, ny, nz);
    }
}
