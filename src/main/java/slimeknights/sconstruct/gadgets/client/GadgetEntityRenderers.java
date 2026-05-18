package slimeknights.sconstruct.gadgets.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import slimeknights.sconstruct.gadgets.entity.GadgetEntities;
import slimeknights.sconstruct.tools.entity.ToolEntities;

/**
 * Client-only registrar that binds the gadget projectile entities to their renderers
 * (SMTCON-141). All three are simple item-billboard projectiles, so each uses vanilla's
 * {@link ThrownItemRenderer} — it draws the projectile's carried item facing the camera, which
 * automatically gives every throwball colour its own variant texture (each colour is a
 * distinct item with its own model).
 *
 * <p>Covers the throwball and glow ball from the gadgets subsystem plus the Phase-4 shuriken,
 * which had no renderer until now — the ticket groups all three projectile renderers here.
 *
 * <p>Loaded only on the physical client — the dedicated server has no rendering pipeline and
 * would crash on the client-only {@link EntityRenderersEvent} reference. {@code SConstruct}
 * guards the {@link #register(IEventBus)} call with a {@code Dist.CLIENT} check.
 */
public final class GadgetEntityRenderers {

    private GadgetEntityRenderers() {
    }

    /** Subscribe the {@link EntityRenderersEvent.RegisterRenderers} listener for the projectiles. */
    public static void register(IEventBus modBus) {
        modBus.addListener(GadgetEntityRenderers::onRegisterRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GadgetEntities.THROWBALL.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(GadgetEntities.GLOW_BALL.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ToolEntities.SHURIKEN.get(), ThrownItemRenderer::new);
    }
}
