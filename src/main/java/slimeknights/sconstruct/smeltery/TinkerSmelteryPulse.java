package slimeknights.sconstruct.smeltery;

import java.util.function.Consumer;

import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import slimeknights.sconstruct.common.SmithiesParticles;
import slimeknights.sconstruct.common.pulse.Pulse;
import slimeknights.sconstruct.shared.SharedTabs;
import slimeknights.sconstruct.smeltery.client.SmelteryBlockEntityRenderers;
import slimeknights.sconstruct.smeltery.client.SmelteryClientFluidTypes;
import slimeknights.sconstruct.smeltery.client.SmelteryClientMenus;
import slimeknights.sconstruct.smeltery.client.SmelteryParticles;
import slimeknights.sconstruct.smeltery.recipe.SmelteryRecipes;

/**
 * Phase-5 smeltery subsystem. Owns the molten-metal fluids, the seared construction blocks, the
 * six smeltery component blocks (controller + tanks + drain + chute), the casting table and
 * basin, the melting / casting / alloy recipe types, the disassembly listener, and the
 * client-side fluid / menu / block-entity-renderer wiring.
 *
 * <p>The pulse is gated by the {@code smeltery} flag in the STARTUP config (default-enabled).
 * Every registration below moved here out of {@code SConstruct} so disabling the flag skips the
 * whole of Phase 5 cleanly — no molten fluids, no seared blocks, no casting, no smeltery recipe
 * types — rather than the registrations running unconditionally during mod construction.
 * Operators who disable the pulse on a server must also disable it on every connecting client
 * or face a missing-registry-entry desync; STARTUP flags are not network-synced.
 *
 * <p>{@link #register} touches each content hub's {@code init()} so the {@code DeferredHolder}
 * field initialisers run before their registry events fire, subscribes the smeltery's
 * {@code RegisterCapabilitiesEvent} handler so the capability bindings are skipped when the
 * pulse is off (they reference block-entity types that only exist while the pulse is enabled),
 * registers the disassembly listener on the game bus, and — on a physical client — wires the
 * molten-fluid client extensions, the controller menu screen, and the controller's fluid
 * renderer. The client helpers each subscribe a mod-bus event that fires after construction, so
 * they live in {@link #register} behind a {@code Dist.CLIENT} guard rather than in
 * {@link #clientSetup}.
 *
 * <p>The smeltery's server→client sync payloads stay registered by the central
 * {@code TinkerNetwork} dispatcher: their {@code CustomPacketPayload.Type} constants resolve no
 * registry holders, so registering them while the pulse is disabled is inert — a smeltery that
 * never assembles never sends them.
 */
public final class TinkerSmelteryPulse implements Pulse {

    @Override
    public String id() {
        return "smeltery";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void register(IEventBus modBus) {
        // Force each content hub's static initialiser to run so every DeferredHolder field is
        // registered before TinkerRegistries' registry events fire.
        SmelteryFluids.init();
        SearedBlocks.init();
        SmelteryComponents.init();
        CastingBlocks.init();
        SmelteryRecipes.init();
        SmithiesParticles.init();

        // The capability bindings query the casting / controller / tank block-entity types, which
        // only exist once the hubs above have registered them — so the listener is subscribed
        // here, inside the pulse, rather than from the central TinkerCapabilities dispatcher.
        // Disabling the pulse then skips the bindings instead of resolving unregistered holders.
        modBus.addListener(SmelteryCapabilities::register);

        // SMTCON-117: breaking a seared or component block re-validates any nearby controller.
        SmelteryEvents.register(NeoForge.EVENT_BUS);

        // SMTCON-201: surface every smeltery block-item and molten bucket in the creative menu.
        // Subscribed inside the pulse so disabling the smeltery flag skips the tab population
        // along with the registrations themselves.
        modBus.addListener(TinkerSmelteryPulse::populateCreativeTab);

        // Client-only handlers. Each subscribes a mod-bus event (RegisterClientExtensionsEvent,
        // RegisterMenuScreensEvent, EntityRenderersEvent) that fires after construction, so they
        // must subscribe here during register(). Guarded by FMLEnvironment.dist so the
        // client-only classes never resolve on a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SmelteryClientFluidTypes.register(modBus);
            SmelteryClientMenus.register(modBus);
            SmelteryBlockEntityRenderers.register(modBus);
            SmelteryParticles.register(modBus);
        }
    }

    /**
     * Append every smeltery block-item and molten-metal bucket to the {@link SharedTabs#GENERAL}
     * creative tab. Without this the smeltery content registers but never surfaces in the
     * creative menu — and, since JEI builds its ingredient list from the creative tabs, it is
     * absent from JEI too. Covers the seared construction blocks, the six component blocks, the
     * casting table and basin, and the molten-metal buckets.
     */
    private static void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (SharedTabs.GENERAL.getKey().equals(event.getTabKey())) {
            acceptAll(event::accept);
        }
    }

    /**
     * Visit every smeltery block-item and molten-metal bucket with {@code accept}. The
     * {@link #populateCreativeTab} listener delegates here, so a unit test can drive the same
     * traversal with a collecting visitor instead of a live {@link BuildCreativeModeTabContentsEvent}.
     */
    static void acceptAll(Consumer<ItemLike> accept) {
        SearedBlocks.ALL.forEach(block -> accept.accept(block.get()));
        SmelteryComponents.ALL.forEach(block -> accept.accept(block.get()));
        CastingBlocks.ALL.forEach(block -> accept.accept(block.get()));
        SmelteryFluids.ALL.forEach(set -> accept.accept(set.bucket().get()));
    }
}
