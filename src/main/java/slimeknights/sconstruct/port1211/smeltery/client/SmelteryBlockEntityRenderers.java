package slimeknights.sconstruct.port1211.smeltery.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;

/**
 * Client-only registrar that binds the smeltery controller block-entity to its
 * {@link SmelteryRenderer} (SMTCON-126). Subscribes a single
 * {@link EntityRenderersEvent.RegisterRenderers} listener which fires once during client setup.
 *
 * <p>{@link #register(IEventBus)} is invoked from {@code SConstruct} under a {@code Dist.CLIENT}
 * guard so this class never resolves on a dedicated server; the call relocates into the
 * smeltery pulse's client setup at SMTCON-131.
 */
public final class SmelteryBlockEntityRenderers {

    private SmelteryBlockEntityRenderers() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SmelteryBlockEntityRenderers::onRegisterRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SmelteryComponents.SMELTERY_CONTROLLER_BE.get(), SmelteryRenderer::new);
    }
}
