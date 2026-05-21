package slimeknights.sconstruct.smeltery.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import slimeknights.sconstruct.smeltery.SmelteryComponents;

/**
 * Client-only registrar that binds the smeltery block-entities to their renderers — the
 * controller to {@link SmelteryRenderer} (SMTCON-126) and the two seared tanks to
 * {@link SearedTankRenderer} (SMTCON-233). Subscribes a single
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
        // SMTCON-233: both seared tank variants draw their fluid level via SearedTankRenderer.
        event.registerBlockEntityRenderer(SmelteryComponents.TANK_IO_BE.get(), SearedTankRenderer::new);
        event.registerBlockEntityRenderer(SmelteryComponents.TANK_IN_BE.get(), SearedTankRenderer::new);
    }
}
