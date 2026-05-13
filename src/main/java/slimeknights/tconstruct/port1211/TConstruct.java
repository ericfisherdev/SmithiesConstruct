package slimeknights.tconstruct.port1211;

import java.util.List;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import slimeknights.tconstruct.port1211.common.TinkerRegistries;
import slimeknights.tconstruct.port1211.common.config.Config;
import slimeknights.tconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.tconstruct.port1211.common.pulse.Pulse;
import slimeknights.tconstruct.port1211.common.pulse.PulseLoader;
import slimeknights.tconstruct.port1211.data.DataGenerators;
import slimeknights.tconstruct.port1211.shared.SharedBlocks;
import slimeknights.tconstruct.port1211.shared.SharedFluids;
import slimeknights.tconstruct.port1211.shared.SharedItems;
import slimeknights.tconstruct.port1211.shared.SharedTabs;

/**
 * Mod entry point. Composes the Phase 1 foundation infrastructure during mod construction:
 * registers the COMMON {@link Config} spec, attaches every {@link TinkerRegistries}
 * DeferredRegister, forces the {@link TinkerDataComponents} static initialiser, wires the
 * datagen hook, and boots the {@link PulseLoader} against the (currently empty) list of pulses
 * that Phase 2+ will populate.
 *
 * <p>The legacy 1.12 code under {@code slimeknights.tconstruct.*} is excluded from the source
 * set in {@code build.gradle} and will not compile against 1.21.1 mappings. Port packages
 * incrementally and widen the {@code sourceSets.main.java.include} pattern as you go.
 */
@Mod(TConstruct.MOD_ID)
public final class TConstruct {
    public static final String MOD_ID = "tconstruct";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TConstruct(IEventBus modBus, ModContainer container) {
        // Register the COMMON spec before any subsystem reads its flags. NeoForge guarantees the
        // TOML is loaded before FMLCommonSetupEvent fires, so a PulseLoader.boot called during
        // common setup sees the resolved values rather than the declared defaults.
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Attach every DeferredRegister the mod owns to the bus before any pulse runs — pulses
        // pull from these and need them to have already subscribed their registry listeners.
        TinkerRegistries.registerAll(modBus);

        // Touch each data-component holder class so its static initialiser runs and registers
        // its DataComponentType against TinkerRegistries.DATA_COMPONENTS. Without an explicit
        // reference the class might never be loaded — DeferredHolder fields are normally
        // accessed lazily by pulses, but the *register* call inside the static block has to
        // run before the registry event fires.
        TinkerDataComponents.init();

        // Touch the shared content classes so their DeferredHolder/DeferredBlock/DeferredItem
        // field initialisers run during mod construction, before the registry events fire.
        // Order matters: SharedFluids must load before SharedItems so SharedItems.BUCKET_BLOOD's
        // initialiser can resolve SharedFluids.BLOOD; SharedTabs must load after the content
        // classes so its tab population listener has every field available.
        SharedBlocks.init();
        SharedFluids.init();
        SharedItems.init();
        SharedTabs.init();
        SharedTabs.registerCreativeTabContents(modBus);

        // Client-side fluid rendering metadata (textures, tint, fog) — must be guarded since the
        // class transitively references client-only types (Camera, ClientLevel) that don't exist
        // on a dedicated server. Routing through a static factory means the server JVM never
        // loads SharedClientFluidTypes at all.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            slimeknights.tconstruct.port1211.shared.client.SharedClientFluidTypes.register(modBus);
        }

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(DataGenerators::onGather);
        NeoForge.EVENT_BUS.register(this);

        // Boot the pulse loader with an empty pulse list — Phase 1 ships no pulses yet, but
        // wiring the call now means Phase 2+ tickets that add the first Pulse implementation
        // only have to append to this list, not change the constructor shape. Gating is bound
        // to Config.pulseGate() so the COMMON config TOML's [pulses] table actually controls
        // registration the moment any pulse is added.
        List<Pulse> pulses = List.of();
        PulseLoader.boot(modBus, pulses, Config.pulseGate());
        LOGGER.info("TConstruct 1.21.1 port: foundation infrastructure wired ({} pulses)", pulses.size());
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("TConstruct 1.21.1 port: common setup (stub)");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("TConstruct 1.21.1 port: server starting (stub)");
    }
}
