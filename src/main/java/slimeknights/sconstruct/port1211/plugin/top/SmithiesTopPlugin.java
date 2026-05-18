package slimeknights.sconstruct.port1211.plugin.top;

import java.util.function.Function;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import slimeknights.sconstruct.port1211.SConstruct;

import mcjty.theoneprobe.api.ITheOneProbe;

/**
 * The mod's The One Probe integration entry point (SMTCON-165). The One Probe has no annotation
 * plugin API — integration is requested by sending it the {@code getTheOneProbe} inter-mod
 * message, which hands back the {@link ITheOneProbe} API for provider registration.
 *
 * <p>The message is enqueued only when {@code theoneprobe} is actually loaded, so the
 * provider classes — which reference The One Probe API types — are never class-loaded when the
 * mod is absent.
 */
@EventBusSubscriber(modid = SConstruct.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class SmithiesTopPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The One Probe's mod id — both the load gate and the IMC message target. */
    private static final String TOP_MOD_ID = "theoneprobe";

    private SmithiesTopPlugin() {
    }

    /**
     * Mod-bus IMC handler. When The One Probe is present, sends it the {@code getTheOneProbe}
     * message; The One Probe invokes the supplied function with its API so the smeltery and
     * tank probe providers can register.
     */
    @SubscribeEvent
    public static void enqueueImc(InterModEnqueueEvent event) {
        if (!ModList.get().isLoaded(TOP_MOD_ID)) {
            return;
        }
        InterModComms.sendTo(TOP_MOD_ID, "getTheOneProbe", () -> (Function<ITheOneProbe, Void>) probe -> {
            probe.registerProvider(new SmelteryProbeProvider());
            probe.registerProvider(new TankProbeProvider());
            LOGGER.info("Registered 2 Smithies' Construct The One Probe providers (smeltery, seared tank)");
            return null;
        });
    }
}
