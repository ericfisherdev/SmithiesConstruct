package slimeknights.sconstruct.world.client;

import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Slime;

import com.mojang.blaze3d.vertex.PoseStack;

import slimeknights.sconstruct.SConstruct;

/**
 * Shared base for slime-mob renderers. Mirrors vanilla {@code SlimeRenderer} verbatim — the
 * slime {@link SlimeModel} baked from {@link ModelLayers#SLIME}, the outer translucent gel
 * {@link SlimeOuterLayer}, the size-aware shadow radius, and the squish-driven scale
 * computation — but is parameterised by the slime subtype so each registered entity type
 * resolves to a {@link MobRenderer} of the correct generic shape (Java's invariant generics
 * refuse {@code SlimeRenderer extends MobRenderer<Slime, SlimeModel<Slime>>} where
 * {@code MobRenderer<EntityBlueslime, ...>} is wanted by the renderer registration API).
 *
 * <p>Subclasses pin the texture by passing the bare filename (no namespace, no path prefix)
 * to {@link #SmithiesSlimeRenderer(EntityRendererProvider.Context, String)}. The base resolves
 * it to {@code sconstruct:textures/entity/slime/<name>.png} so adding a new slime variant is
 * one new subclass plus one new PNG.
 */
public abstract class SmithiesSlimeRenderer<T extends Slime> extends MobRenderer<T, SlimeModel<T>> {

    /** Base shadow radius for a size-1 slime — vanilla {@code SlimeRenderer} ships {@code 0.25F}. */
    private static final float BASE_SHADOW_RADIUS = 0.25F;

    /** Reference slime scale used to suppress z-fighting against the outer-gel layer; vanilla value. */
    private static final float SCALE_REFERENCE = 0.999F;

    /** Y-axis translation that pairs with {@link #SCALE_REFERENCE} to keep the slime's shadow flush with the ground. */
    private static final float SCALE_VERTICAL_NUDGE = 0.001F;

    /** Half-size offset used by the squish-driven scale; matches vanilla {@code 0.5F}. */
    private static final float SCALE_HALF_SIZE = 0.5F;

    private final ResourceLocation texture;

    protected SmithiesSlimeRenderer(EntityRendererProvider.Context ctx, String textureName) {
        super(ctx, new SlimeModel<>(ctx.bakeLayer(ModelLayers.SLIME)), BASE_SHADOW_RADIUS);
        this.addLayer(new SlimeOuterLayer<>(this, ctx.getModelSet()));
        this.texture = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "textures/entity/slime/" + textureName + ".png");
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        // Shadow scales with the entity's runtime size — a size-4 huge slime drops a 1.0 block
        // shadow, a size-1 blueslime drops a 0.25 block shadow. Matches vanilla SlimeRenderer.
        this.shadowRadius = BASE_SHADOW_RADIUS * entity.getSize();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        // Pull the slime towards 1.0 by 0.001 so its outer gel layer doesn't z-fight with the
        // inner model — same trick vanilla uses, copied verbatim because the constants are
        // tuned to the SlimeModel geometry.
        pose.scale(SCALE_REFERENCE, SCALE_REFERENCE, SCALE_REFERENCE);
        pose.translate(0.0F, SCALE_VERTICAL_NUDGE, 0.0F);
        float size = entity.getSize();
        float squish = Mth.lerp(partialTick, entity.oSquish, entity.squish) / (size * SCALE_HALF_SIZE + 1.0F);
        float scale = 1.0F / (squish + 1.0F);
        pose.scale(scale * size, 1.0F / scale * size, scale * size);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
