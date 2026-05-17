package slimeknights.sconstruct.port1211.common;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.tools.ToolCapabilities;

/**
 * Central dispatcher for {@link RegisterCapabilitiesEvent}. Concentrates the capability
 * registration of the always-on subsystems into a single auditable location, invoking each
 * per-pulse helper from one place here.
 *
 * <p>{@code @EventBusSubscriber(modid = MOD_ID, bus = MOD)} wires the static handler to the
 * mod bus during mod construction; no manual {@code addListener} call needed from
 * {@link SConstruct}. The dispatcher logs the delegation so a server boot log confirms the
 * hook fired.
 *
 * <p>The smeltery's and the gadgets' capability bindings are <em>not</em> dispatched here: they
 * reference block-entity types that exist only while their pulse is enabled, so
 * {@code TinkerSmelteryPulse} and {@code TinkerGadgetsPulse} each subscribe their own
 * capability helper — disabling a pulse then skips its bindings instead of resolving
 * unregistered holders. Only {@link ToolCapabilities} remains centrally dispatched.
 */
@EventBusSubscriber(modid = SConstruct.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
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
     * Dispatch the supplied capability-registration event into the centrally-managed helpers.
     * Package-private for tests; production code reaches this via {@link #onRegisterCapabilities}.
     *
     * <p>Pulse-gated subsystems (smeltery, gadgets) subscribe their own capability helpers from
     * their pulse instead — see this class's javadoc.
     */
    static void configure(RegisterCapabilitiesEvent event) {
        LOGGER.info("SConstruct: dispatching RegisterCapabilitiesEvent to the tool capability helper");
        ToolCapabilities.register(event);
    }
}
