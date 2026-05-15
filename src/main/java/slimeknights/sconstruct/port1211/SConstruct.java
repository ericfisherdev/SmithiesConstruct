package slimeknights.sconstruct.port1211;

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

import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.common.config.Config;
import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.common.pulse.Pulse;
import slimeknights.sconstruct.port1211.common.pulse.PulseLoader;
import slimeknights.sconstruct.port1211.data.DataGenerators;
import slimeknights.sconstruct.port1211.shared.TinkerSharedPulse;
import slimeknights.sconstruct.port1211.tools.client.ToolColorHandlers;
import slimeknights.sconstruct.port1211.tools.item.ToolParts;
import slimeknights.sconstruct.port1211.tools.material.MaterialRegistry;
import slimeknights.sconstruct.port1211.world.TinkerWorldPulse;

/**
 * Mod entry point. Composes the Phase 1 foundation infrastructure during mod construction:
 * registers the COMMON {@link Config} spec, attaches every {@link TinkerRegistries}
 * DeferredRegister, forces the {@link TinkerDataComponents} static initialiser, wires the
 * datagen hook, and boots the {@link PulseLoader} against the (currently empty) list of pulses
 * that Phase 2+ will populate.
 *
 * <p>The legacy 1.12 code under {@code slimeknights.tconstruct.*} is excluded from the source
 * set in {@code build.gradle} and will not compile against 1.21.1 mappings — that tree is frozen
 * upstream reference, not migration target. Port packages incrementally into
 * {@code slimeknights.sconstruct.port1211.*} and widen the {@code sourceSets.main.java.include}
 * pattern as you go.
 */
@Mod(SConstruct.MOD_ID)
public final class SConstruct {
    public static final String MOD_ID = "sconstruct";
    private static final Logger LOGGER = LogUtils.getLogger();

    public SConstruct(IEventBus modBus, ModContainer container) {
        // Register the STARTUP spec. STARTUP configs are read immediately when registerConfig
        // returns, so PulseLoader.boot a few lines below can read the actual TOML flags rather
        // than falling back to the declared defaults. (COMMON would only load just before
        // FMLCommonSetupEvent — too late to gate DeferredRegister attachment in this ctor.)
        // The NeoForge desync warning for STARTUP applies when a content-disable flag differs
        // between client and server; a mismatched pulse roster is a deliberate config choice
        // by the operator and outside this layer's contract.
        container.registerConfig(ModConfig.Type.STARTUP, Config.SPEC);

        // Attach every DeferredRegister the mod owns to the bus before any pulse runs — pulses
        // pull from these and need them to have already subscribed their registry listeners.
        TinkerRegistries.registerAll(modBus);

        // Touch each data-component holder class so its static initialiser runs and registers
        // its DataComponentType against TinkerRegistries.DATA_COMPONENTS. Without an explicit
        // reference the class might never be loaded — DeferredHolder fields are normally
        // accessed lazily by pulses, but the *register* call inside the static block has to
        // run before the registry event fires.
        TinkerDataComponents.init();

        // Force ToolParts to register its 16 MaterialItem entries before the ITEMS registry
        // event fires, and subscribe its BuildCreativeModeTabContentsEvent listener so every
        // part shows up under the shared GENERAL tab. This wiring will relocate into a future
        // TinkerToolsPulse#register once that pulse lands; until then it lives here alongside
        // the other "foundation" touches above.
        ToolParts.init();
        ToolParts.registerCreativeTabContents(modBus);

        // Register the sconstruct:material datapack registry on DataPackRegistryEvent.NewRegistry
        // (mod bus) and the OnDatapackSyncEvent cache rebuild (NeoForge bus). Same temporary
        // home as the ToolParts wiring above — moves into TinkerToolsPulse when that lands.
        MaterialRegistry.register(modBus, NeoForge.EVENT_BUS);

        // Client-only: ItemColors handler that tints MaterialItem part icons by the material's
        // packed ARGB colour, plus a ClientPlayerNetworkEvent.LoggingIn refresh that rebuilds the
        // MaterialClientCache from the freshly-synced datapack registry (SMTCON-72). Guarded by
        // FMLEnvironment.dist so the client-only class never resolves on a dedicated server —
        // the JVM does not load types inside an unreached branch.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ToolColorHandlers.register(modBus);
        }

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(DataGenerators::onGather);
        NeoForge.EVENT_BUS.register(this);

        // Boot the pulse loader with every pulse the mod knows about. The loader honours the
        // {@link Config#pulseGate} so each pulse's per-id flag in the COMMON TOML controls
        // whether its register/setup hooks run — disabling "shared" here skips every Phase-2
        // registration cleanly, leaving the mod with only foundation infrastructure.
        List<Pulse> pulses = List.of(new TinkerSharedPulse(), new TinkerWorldPulse());
        PulseLoader.boot(modBus, pulses, Config.pulseGate());
        LOGGER.info("SConstruct 1.21.1 port: foundation infrastructure wired ({} pulses)", pulses.size());
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("SConstruct 1.21.1 port: common setup (stub)");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("SConstruct 1.21.1 port: server starting (stub)");
    }
}
