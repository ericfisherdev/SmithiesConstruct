package slimeknights.tconstruct.port1211.common;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import slimeknights.tconstruct.port1211.TConstruct;
import slimeknights.tconstruct.port1211.gadgets.GadgetCapabilities;
import slimeknights.tconstruct.port1211.smeltery.SmelteryCapabilities;
import slimeknights.tconstruct.port1211.tools.ToolCapabilities;

/**
 * Central dispatcher for {@link RegisterCapabilitiesEvent}. Concentrates every capability
 * registration the mod performs into a single auditable location — instead of three or four
 * {@code @SubscribeEvent} annotations scattered across pulse classes, every per-pulse helper
 * lives in its pulse package and is invoked from one place here. A reviewer reading this file
 * sees the entire capability surface area at a glance.
 *
 * <p>{@code @EventBusSubscriber(modid = MOD_ID, bus = MOD)} wires the static handler to the
 * mod bus during mod construction; no manual {@code addListener} call needed from
 * {@link TConstruct}. The per-pulse helpers — {@link SmelteryCapabilities},
 * {@link GadgetCapabilities}, {@link ToolCapabilities} — currently ship empty stubs; Phase 2+
 * fills them in as content lands. The dispatcher logs each delegation so a server boot log
 * confirms all three pulse hooks fired (per the ticket AC).
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class TinkerCapabilities {

    private static final Logger LOGGER = LogUtils.getLogger();

    private TinkerCapabilities() {
    }

    /**
     * Mod-bus handler for {@link RegisterCapabilitiesEvent}. Delegates to {@link #configure}
     * so unit tests can drive the same logic with a mocked event without bringing up FML.
     */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        configure(event);
    }

    /**
     * Dispatch the supplied capability-registration event into every per-pulse helper.
     * Package-private for tests; production code reaches this via {@link #onRegisterCapabilities}.
     *
     * <p>Adding a new pulse: append its helper class to its own pulse package and add a
     * delegation line here. Removing a pulse's capability surface: drop its line. The order
     * shouldn't matter (each helper registers disjoint capabilities), but the logged sequence
     * provides a stable reading order for the boot log.
     */
    static void configure(RegisterCapabilitiesEvent event) {
        LOGGER.info("TConstruct: dispatching RegisterCapabilitiesEvent to per-pulse helpers");
        SmelteryCapabilities.register(event);
        GadgetCapabilities.register(event);
        ToolCapabilities.register(event);
    }
}
