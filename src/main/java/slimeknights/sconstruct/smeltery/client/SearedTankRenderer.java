package slimeknights.sconstruct.smeltery.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import slimeknights.sconstruct.smeltery.block.entity.SearedTankBE;

/**
 * {@link BlockEntityRenderer} for the seared tank blocks (SMTCON-233) — draws the tank's fluid
 * level as a panel over the window region painted on each of the four side faces.
 *
 * <p>The seared tank block model is an opaque cube, so the fluid cannot be drawn inside it the
 * way the smeltery interior renderer fills its open bowl. Instead this renderer emits one flat
 * fluid quad per horizontal face, inset to the window rectangle the tank texture paints and
 * pushed a hair proud of the face so it draws cleanly over the block surface. The panel rises
 * from the bottom of the window to a height proportional to the tank's fill fraction; an empty
 * tank renders nothing and the painted window shows through as before.
 *
 * <p>Fluid contents reach the client through {@link SearedTankBE}'s update-tag / update-packet
 * sync — also added by SMTCON-233 — so the rendered level follows the server in real time.
 */
public class SearedTankRenderer implements BlockEntityRenderer<SearedTankBE> {

    /** Horizontal inset of the window panel from each face edge, in block units (5 px). */
    private static final float WINDOW_INSET = 5.0F / 16.0F;

    /** Bottom of the window panel above the block floor, in block units (2 px). */
    private static final float WINDOW_BOTTOM = 2.0F / 16.0F;

    /** Top of the window panel below the block ceiling — leaves a 2 px brick lip (14 px). */
    private static final float WINDOW_TOP = 14.0F / 16.0F;

    /** Distance the fluid panel sits proud of the block face so it draws over the painted window. */
    private static final float FACE_OFFSET = 0.002F;

    /** Opaque-alpha bits OR-ed onto a fluid tint so a tint with no alpha channel still draws. */
    private static final int OPAQUE_ALPHA = 0xFF000000;

    public SearedTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SearedTankBE tank, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        IFluidHandler handler = tank.getFluidHandler();
        if (handler.getTanks() == 0) {
            return;
        }
        FluidStack fluid = handler.getFluidInTank(0);
        if (fluid.isEmpty()) {
            return;
        }
        int capacity = handler.getTankCapacity(0);
        if (capacity <= 0) {
            return;
        }
        float fraction = Math.clamp((float) fluid.getAmount() / capacity, 0.0F, 1.0F);
        float fluidTop = WINDOW_BOTTOM + (WINDOW_TOP - WINDOW_BOTTOM) * fraction;
        if (fluidTop <= WINDOW_BOTTOM) {
            return;
        }

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
        int color = OPAQUE_ALPHA | extensions.getTintColor(fluid);
        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();

        float lo = WINDOW_INSET;
        float hi = 1.0F - WINDOW_INSET;
        // V grows downward in atlas space: sprite.getV0() is the texture's top edge, getV1() its
        // bottom. The quad's bottom corner must therefore take getV1() and its top corner an
        // interpolated value, so a partly-filled tank shows the bottom slice of the still texture
        // rising toward the full sprite as the tank fills.
        float vBottom = sprite.getV1();
        float vTop = sprite.getV1() - (sprite.getV1() - sprite.getV0()) * (fluidTop - WINDOW_BOTTOM) / (WINDOW_TOP - WINDOW_BOTTOM);
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();

        // The four side panels. Each quad's two horizontal corners are wound so the front face —
        // pointing outward along the given normal — is counter-clockwise, so RenderType.translucent's
        // back-face cull keeps the panel visible from outside the tank.
        // North face (z = 0), panel pushed out to z = -FACE_OFFSET, facing -Z.
        quad(consumer, matrix, color, packedLight, hi, WINDOW_BOTTOM, -FACE_OFFSET, lo, fluidTop, -FACE_OFFSET, u0, u1, vBottom, vTop, 0.0F, 0.0F, -1.0F);
        // South face (z = 1), panel at z = 1 + FACE_OFFSET, facing +Z.
        quad(consumer, matrix, color, packedLight, lo, WINDOW_BOTTOM, 1.0F + FACE_OFFSET, hi, fluidTop, 1.0F + FACE_OFFSET, u0, u1, vBottom, vTop, 0.0F, 0.0F, 1.0F);
        // West face (x = 0), panel at x = -FACE_OFFSET, facing -X.
        quad(consumer, matrix, color, packedLight, -FACE_OFFSET, WINDOW_BOTTOM, hi, -FACE_OFFSET, fluidTop, lo, u0, u1, vBottom, vTop, -1.0F, 0.0F, 0.0F);
        // East face (x = 1), panel at x = 1 + FACE_OFFSET, facing +X.
        quad(consumer, matrix, color, packedLight, 1.0F + FACE_OFFSET, WINDOW_BOTTOM, lo, 1.0F + FACE_OFFSET, fluidTop, hi, u0, u1, vBottom, vTop, 1.0F, 0.0F, 0.0F);
    }

    /**
     * Emits a single upright fluid panel quad. The panel spans {@code (x0, yBottom)} to
     * {@code (x1, yTop)} in the plane shared by both corner triples — the caller supplies the two
     * horizontal corners and the renderer fills the rectangle between {@code yBottom} and
     * {@code yTop}. {@code vBottom} maps to the bottom edge, {@code vTop} to the top. Wound so the
     * front face points along {@code (nx, ny, nz)}.
     */
    private static void quad(VertexConsumer consumer, Matrix4f matrix, int color, int light, float x0, float yBottom, float z0, float x1, float yTop, float z1, float u0, float u1, float vBottom,
            float vTop, float nx, float ny, float nz) {
        vertex(consumer, matrix, color, light, x0, yBottom, z0, u0, vBottom, nx, ny, nz);
        vertex(consumer, matrix, color, light, x1, yBottom, z1, u1, vBottom, nx, ny, nz);
        vertex(consumer, matrix, color, light, x1, yTop, z1, u1, vTop, nx, ny, nz);
        vertex(consumer, matrix, color, light, x0, yTop, z0, u0, vTop, nx, ny, nz);
    }

    /** Emits one quad vertex in the {@code BLOCK} vertex format used by the translucent layer. */
    private static void vertex(VertexConsumer consumer, Matrix4f matrix, int color, int light, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        consumer.addVertex(matrix, x, y, z).setColor(color).setUv(u, v).setLight(light).setNormal(nx, ny, nz);
    }
}
