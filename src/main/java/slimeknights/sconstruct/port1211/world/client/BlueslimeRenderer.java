package slimeknights.sconstruct.port1211.world.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import slimeknights.sconstruct.port1211.world.entity.EntityBlueslime;

/**
 * Renderer for {@link EntityBlueslime}. Inherits the vanilla slime model + outer-gel layer +
 * squish-aware scale from {@link SmithiesSlimeRenderer} and pins the texture to
 * {@code sconstruct:textures/entity/slime/blueslime.png}.
 */
public class BlueslimeRenderer extends SmithiesSlimeRenderer<EntityBlueslime> {

    public BlueslimeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "blueslime");
    }
}
