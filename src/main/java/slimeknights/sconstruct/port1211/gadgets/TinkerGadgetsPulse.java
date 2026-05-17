package slimeknights.sconstruct.port1211.gadgets;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

import slimeknights.sconstruct.port1211.common.pulse.Pulse;
import slimeknights.sconstruct.port1211.gadgets.client.GadgetEntityRenderers;
import slimeknights.sconstruct.port1211.gadgets.entity.GadgetEntities;
import slimeknights.sconstruct.port1211.gadgets.recipe.GadgetRecipes;

/**
 * Phase-6 gadgets subsystem. Owns the slimesling / throwball / glow-ball / piggyback items, the
 * slime armor set and its armor material, the wither head, the drying rack / wooden hopper /
 * stone ladder / dried clay blocks, the gadget projectile entity types, the piggyback
 * attachment type, the drying recipe type, the slime-boots fall-cushion and full-set knockback
 * listeners, the throwball/glow-ball dispenser behaviours, and the client-side projectile
 * renderers.
 *
 * <p>The pulse is gated by the {@code gadgets} flag in the STARTUP config (default-enabled).
 * Every registration below moved here out of {@code SConstruct} so disabling the flag skips the
 * whole of Phase 6 cleanly rather than registering it unconditionally during mod construction.
 * Operators who disable the pulse on a server must also disable it on every connecting client
 * or face a missing-registry-entry desync; STARTUP flags are not network-synced.
 *
 * <p>{@link #register} touches each content hub's {@code init()} so the {@code DeferredHolder}
 * field initialisers run before their registry events fire, subscribes the creative-tab
 * listeners and the {@code RegisterCapabilitiesEvent} handler (kept inside the pulse so the
 * capability bindings — which reference gadget block-entity types — are skipped when the pulse
 * is off), registers the game-bus gadget listeners, and — on a physical client — wires the
 * projectile renderers. {@link #setup} registers the dispenser behaviours on the main thread.
 */
public final class TinkerGadgetsPulse implements Pulse {

    @Override
    public String id() {
        return "gadgets";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void register(IEventBus modBus) {
        // Force each content hub's static initialiser to run so every DeferredHolder field is
        // registered before TinkerRegistries' registry events fire.
        GadgetItems.init();
        GadgetBlocks.init();
        GadgetEntities.init();
        GadgetAttachments.init();
        GadgetArmorMaterials.init();
        GadgetRecipes.init();

        // Creative-tab listeners that push the gadget items and blocks into SharedTabs.GENERAL.
        GadgetItems.registerCreativeTabContents(modBus);
        GadgetBlocks.registerCreativeTabContents(modBus);

        // The capability bindings query the drying-rack / wooden-hopper block-entity types, which
        // only exist once the hubs above have registered them — so the listener is subscribed
        // here, inside the pulse, rather than from the central TinkerCapabilities dispatcher.
        // Disabling the pulse then skips the bindings instead of resolving unregistered holders.
        modBus.addListener(GadgetCapabilities::register);

        // SMTCON-134/136: the piggyback ride loop and the slime-armor fall / knockback listeners.
        GadgetEvents.register(NeoForge.EVENT_BUS);

        // Client-only handler. EntityRenderersEvent fires after construction, so the renderer
        // registrar must subscribe here during register(). Guarded by FMLEnvironment.dist so the
        // client-only renderer class never resolves on a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            GadgetEntityRenderers.register(modBus);
        }
    }

    @Override
    public void setup(FMLCommonSetupEvent event) {
        // DispenserBlock's behaviour registry is not thread-safe, so the throwball / glow-ball
        // dispenser registration is enqueued onto the main thread.
        event.enqueueWork(GadgetDispenserBehaviors::register);
    }
}
