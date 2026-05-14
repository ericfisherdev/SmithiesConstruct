package slimeknights.sconstruct.port1211.world.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import slimeknights.sconstruct.port1211.world.WorldEntities;

/**
 * Client-only registrar that binds the two {@link slimeknights.sconstruct.port1211.world.WorldEntities}
 * slime mob types to their per-entity {@link SmithiesSlimeRenderer} subclass. Subscribes a single
 * {@link EntityRenderersEvent.RegisterRenderers} listener which fires once during client setup
 * with the active {@code EntityRendererProvider.Context}.
 *
 * <p>Loaded only on the physical client — the dedicated server has no rendering pipeline and
 * would crash on the client-only {@code EntityRenderersEvent} reference. The world pulse guards
 * the {@link #register(IEventBus)} call with a {@code Dist.CLIENT} check; this class never
 * needs to be loaded on the server.
 */
public final class WorldEntityRenderers {

    private WorldEntityRenderers() {
    }

    /**
     * Subscribe the {@link EntityRenderersEvent.RegisterRenderers} listener that binds each
     * slime mob entity type to its renderer.
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(WorldEntityRenderers::onRegisterRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WorldEntities.BLUESLIME.get(), BlueslimeRenderer::new);
        event.registerEntityRenderer(WorldEntities.HUGESLIME.get(), HugeSlimeRenderer::new);
    }
}
