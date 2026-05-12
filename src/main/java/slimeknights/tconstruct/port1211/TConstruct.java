package slimeknights.tconstruct.port1211;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import slimeknights.tconstruct.port1211.common.TinkerRegistries;
import slimeknights.tconstruct.port1211.common.config.Config;
import slimeknights.tconstruct.port1211.data.DataGenerators;

/**
 * Scaffolding entry point for the 1.21.1 port.
 * <p>
 * The legacy 1.12 code under {@code slimeknights.tconstruct.*} is excluded from the
 * source set in build.gradle and will not compile against 1.21.1 mappings. Port packages
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

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(DataGenerators::onGather);
        NeoForge.EVENT_BUS.register(this);

        // TODO(port): replace the legacy Pulse system (TinkerPulseManager) with PulseLoader.boot
        // once the Phase 1 pulses (shared, world, tools, smeltery, gadgets, debug) are ported.
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("TConstruct 1.21.1 port: common setup (stub)");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("TConstruct 1.21.1 port: server starting (stub)");
    }
}
