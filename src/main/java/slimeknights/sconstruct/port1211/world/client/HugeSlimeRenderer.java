package slimeknights.sconstruct.port1211.world.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import slimeknights.sconstruct.port1211.world.entity.EntityHugeSlime;

/**
 * Renderer for {@link EntityHugeSlime}. Inherits the vanilla slime model + outer-gel layer +
 * squish-aware scale from {@link TinkerSlimeRenderer} and pins the texture to
 * {@code sconstruct:textures/entity/slime/hugeslime.png}. The boss "scales by 4" requirement
 * from the SMTCON-57 implementation plan is met by the entity's {@code DEFAULT_SIZE = 4} —
 * the inherited {@link TinkerSlimeRenderer#scale} reads {@code entity.getSize()} and applies
 * the same multiplier vanilla slimes use, so no renderer-side scale override is needed.
 */
public class HugeSlimeRenderer extends TinkerSlimeRenderer<EntityHugeSlime> {

    public HugeSlimeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "hugeslime");
    }
}
