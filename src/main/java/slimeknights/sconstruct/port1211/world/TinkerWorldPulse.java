package slimeknights.sconstruct.port1211.world;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import slimeknights.sconstruct.port1211.common.pulse.Pulse;
import slimeknights.sconstruct.port1211.shared.SharedTabs;
import slimeknights.sconstruct.port1211.world.client.WorldClientFluidTypes;

/**
 * Phase-3 world subsystem. Owns the slime-island content stack — Phase-3 first pass registers
 * the four slime fluids via {@link WorldFluids}; later tasks add slime entities, slime dirt /
 * leaves / saplings, and the slime-island worldgen feature.
 *
 * <p>The pulse is gated by the {@code world} flag in the COMMON config (default-enabled);
 * disabling it skips every {@link WorldFluids} registration, leaving the mod with no slime-fluid
 * surfaces. Operators who disable the pulse on a server must also disable it on every connecting
 * client or face a missing-registry-entry desync — STARTUP config flags are not network-synced
 * (see {@code Config} javadoc).
 *
 * <p>{@link #register} touches {@link WorldFluids} so its static field initialisers run before
 * the registry events fire, subscribes the creative-tab content listener so the four slime
 * buckets show up in the shared {@link SharedTabs#GENERAL} tab, and — on a physical client —
 * wires the {@link WorldClientFluidTypes} registration for per-fluid tints and fog colours.
 */
public final class TinkerWorldPulse implements Pulse {

    @Override
    public String id() {
        return "world";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void register(IEventBus modBus) {
        // Touch the content class so its field initialisers run and TinkerRegistries.{FLUID_TYPES,
        // FLUIDS, BLOCKS, ITEMS} see every entry before their registry events fire.
        WorldFluids.init();

        modBus.addListener(TinkerWorldPulse::populateCreativeTab);

        // Client-side fluid rendering metadata (textures, tints, fog) must subscribe before the
        // common-setup phase, so it goes in register() guarded by dist. Routing through a static
        // factory means the WorldClientFluidTypes class never loads on a dedicated server — the
        // JVM doesn't resolve types inside an unreached branch.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            WorldClientFluidTypes.register(modBus);
        }
    }

    @SubscribeEvent
    private static void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // World pulse content goes into the same GENERAL tab as the shared pulse — Phase-3+
        // pulses subscribe their own listeners against this tab's key per the contract noted in
        // SharedTabs's javadoc.
        if (!SharedTabs.GENERAL.getKey().equals(event.getTabKey())) {
            return;
        }
        WorldFluids.acceptBuckets(event::accept);
    }
}
