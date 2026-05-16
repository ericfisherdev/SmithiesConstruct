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
import slimeknights.sconstruct.port1211.smeltery.SearedBlocks;
import slimeknights.sconstruct.port1211.smeltery.SmelteryFluids;
import slimeknights.sconstruct.port1211.smeltery.client.SmelteryClientFluidTypes;
import slimeknights.sconstruct.port1211.tools.ToolsPulse;
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

        // The whole Phase-4 tools content stack — tool parts, tool items, station blocks, the
        // material / modifier datapack registries, and the client-side tint / model / screen
        // wiring — is registered by ToolsPulse#register, gated by the "tools" config flag (see
        // the PulseLoader.boot call below). It lived inline here through Phases 1-4 until the
        // pulse landed (SMTCON-107).

        // SMTCON-109: force SmelteryFluids to load so its static block registers all 20
        // molten-metal fluid sets (FluidType + source/flowing fluids + LiquidBlock + bucket)
        // before the registry events fire. Lives here inline for now; relocates into the
        // smeltery pulse's register() when SMTCON-131 wires that pulse.
        SmelteryFluids.init();

        // SMTCON-111: force SearedBlocks to load so its static block registers the 16 seared
        // construction blocks (+ block items) before the registry events fire. Lives here
        // inline for now; relocates into the smeltery pulse's register() at SMTCON-131.
        SearedBlocks.init();

        // SMTCON-110: on a physical client, bind each molten-metal fluid type to its
        // IClientFluidTypeExtensions (shared texture pair + per-metal tint + warm fog). Guarded
        // by FMLEnvironment.dist so the client-only class never resolves on a dedicated server.
        // Relocates into the smeltery pulse's register() alongside SmelteryFluids at SMTCON-131.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SmelteryClientFluidTypes.register(modBus);
        }

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(DataGenerators::onGather);
        NeoForge.EVENT_BUS.register(this);

        // Boot the pulse loader with every pulse the mod knows about. The loader honours the
        // {@link Config#pulseGate} so each pulse's per-id flag in the COMMON TOML controls
        // whether its register/setup hooks run — disabling "shared" here skips every Phase-2
        // registration cleanly, leaving the mod with only foundation infrastructure.
        List<Pulse> pulses = List.of(new TinkerSharedPulse(), new TinkerWorldPulse(), new ToolsPulse());
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
