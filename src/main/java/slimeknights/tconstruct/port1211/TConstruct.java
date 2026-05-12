package slimeknights.tconstruct.port1211;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
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

    public TConstruct(IEventBus modBus) {
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(DataGenerators::onGather);
        NeoForge.EVENT_BUS.register(this);

        // TODO(port): wire DeferredRegisters here.
        //   - Items, Blocks, BlockEntities, Fluids, Entities, Recipes, DataComponents, etc.
        //   - Replace the legacy Pulse system (TinkerPulseManager) with config-gated init calls.
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("TConstruct 1.21.1 port: common setup (stub)");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("TConstruct 1.21.1 port: server starting (stub)");
    }
}
