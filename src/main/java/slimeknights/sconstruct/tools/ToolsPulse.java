package slimeknights.sconstruct.tools;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

import slimeknights.sconstruct.common.pulse.Pulse;
import slimeknights.sconstruct.tools.client.PartBuilderClient;
import slimeknights.sconstruct.tools.client.PatternChestClient;
import slimeknights.sconstruct.tools.client.StencilTableClient;
import slimeknights.sconstruct.tools.client.ToolColorHandlers;
import slimeknights.sconstruct.tools.client.ToolStationClient;
import slimeknights.sconstruct.tools.client.model.ToolModelEvents;
import slimeknights.sconstruct.tools.entity.ToolEntities;
import slimeknights.sconstruct.tools.item.ToolItems;
import slimeknights.sconstruct.tools.item.ToolParts;
import slimeknights.sconstruct.tools.material.MaterialRegistry;
import slimeknights.sconstruct.tools.modifier.ModifierRegistry;

/**
 * Phase-4 tools subsystem. Owns the tinkered-tool content stack: the sixteen {@code MaterialItem}
 * tool parts, the seventeen {@link slimeknights.sconstruct.tools.item.ToolCore} tools
 * (plus the thrown shuriken), the four station blocks (pattern chest, stencil table, part
 * builder, tool station / forge), the {@code sconstruct:material} and {@code sconstruct:modifier}
 * datapack registries, and the client-side tinting / model / screen wiring.
 *
 * <p>The pulse is gated by the {@code tools} flag in the STARTUP config (default-enabled).
 * Every registration below moved here out of {@code SConstruct} so disabling the flag skips the
 * whole of Phase 4 cleanly — no tool items, no station blocks, no material registry — rather
 * than the registrations running unconditionally during mod construction. Operators who disable
 * the pulse on a server must also disable it on every connecting client or face a
 * missing-registry-entry desync; STARTUP flags are not network-synced.
 *
 * <p>{@link #register} touches each content hub's {@code init()} so the {@code DeferredHolder}
 * field initialisers run before their registry events fire, subscribes the per-hub creative-tab
 * listeners against {@link slimeknights.sconstruct.shared.SharedTabs#GENERAL}, wires the
 * material / modifier datapack registries, and — on a physical client — subscribes the tint,
 * baked-model and menu-screen handlers. The client handlers all subscribe mod-bus events that
 * must be added during construction, so they live in {@link #register} behind a
 * {@code Dist.CLIENT} guard rather than in {@link #clientSetup}.
 */
public final class ToolsPulse implements Pulse {

    @Override
    public String id() {
        return "tools";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void register(IEventBus modBus) {
        // Force each content hub's static initialiser to run so every DeferredHolder field is
        // registered before TinkerRegistries' registry events fire, and subscribe the per-hub
        // creative-tab listeners that push items into SharedTabs.GENERAL.
        ToolParts.init();
        ToolParts.registerCreativeTabContents(modBus);
        ToolItems.init();
        ToolItems.registerCreativeTabContents(modBus);

        // SMTCON-101: tool projectile entity types (shuriken). init() forces the entity-type
        // DeferredHolder fields to register before NewRegistryEvent fires.
        ToolEntities.init();

        // The four station blocks — pattern chest, stencil table, part builder, tool station /
        // forge. Each init() forces the block / item / BE / menu DeferredHolder fields to run;
        // registerCreativeTabContents subscribes the BlockItem creative-tab listener.
        PatternChestRegistry.init();
        PatternChestRegistry.registerCreativeTabContents(modBus);
        StencilTableRegistry.init();
        StencilTableRegistry.registerCreativeTabContents(modBus);
        PartBuilderRegistry.init();
        PartBuilderRegistry.registerCreativeTabContents(modBus);
        ToolStationRegistry.init();
        ToolStationRegistry.registerCreativeTabContents(modBus);

        // sconstruct:material and sconstruct:modifier datapack registries — registered on
        // DataPackRegistryEvent.NewRegistry (mod bus) with the server-side cache rebuild on
        // OnDatapackSyncEvent (NeoForge bus).
        MaterialRegistry.register(modBus, NeoForge.EVENT_BUS);
        ModifierRegistry.register(modBus, NeoForge.EVENT_BUS);

        // Client-only handlers. Each subscribes a mod-bus event (RegisterColorHandlersEvent,
        // RegisterMenuScreensEvent, ModelEvent.ModifyBakingResult) that fires after construction,
        // so they must subscribe here during register(). Guarded by FMLEnvironment.dist so the
        // client-only classes never resolve on a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ToolColorHandlers.register(modBus);
            PatternChestClient.register(modBus);
            StencilTableClient.register(modBus);
            PartBuilderClient.register(modBus);
            ToolStationClient.register(modBus);
            ToolModelEvents.register(modBus);
        }
    }
}
