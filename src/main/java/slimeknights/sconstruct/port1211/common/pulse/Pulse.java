package slimeknights.sconstruct.port1211.common.pulse;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * A self-contained subsystem of the mod. Replaces the legacy Pulsar/Pulse model upstream Tinkers'
 * Construct used in 1.12, removed in 1.21.x because Mantle has no port. Each gameplay subsystem
 * (tools, smeltery, world, gadgets, ...) implements this interface so the
 * {@link PulseLoader} can wire its registrations and setup hooks behind a single config flag.
 *
 * <p>Implementations should be cheap to construct — the heavy work happens inside the lifecycle
 * methods below, which only run when the pulse is enabled by the gate passed to the loader.
 */
public interface Pulse {

    /**
     * Stable, hyphen-or-underscore-free identifier used as the config key for the per-pulse
     * enable flag. Conventionally matches the package name fragment (e.g. {@code "tools"},
     * {@code "smeltery"}, {@code "world"}).
     */
    String id();

    /**
     * Whether the pulse should be on by default when no user configuration is present. Most
     * pulses return {@code true}; pulses gating experimental or addon-only features should
     * return {@code false}.
     */
    default boolean defaultEnabled() {
        return true;
    }

    /**
     * Called once during mod construction for each enabled pulse. The pulse subscribes its
     * registry listeners, mod-bus events, and {@code DeferredRegister}s to the supplied bus.
     */
    default void register(IEventBus modBus) {
        // no-op by default — many pulses only need lifecycle hooks
    }

    /**
     * Common (client + dedicated server) setup hook. Subscribed to the mod event bus by
     * {@link PulseLoader} only when the pulse is enabled.
     */
    default void setup(FMLCommonSetupEvent event) {
        // no-op by default
    }

    /**
     * Client-only setup hook. Subscribed to the mod event bus by {@link PulseLoader} only when
     * the pulse is enabled <em>and</em> the runtime is the physical client; the loader never
     * adds this listener on a dedicated server, so the implementation may safely reference
     * client-only classes (renderers, key mappings, etc.).
     */
    default void clientSetup(FMLClientSetupEvent event) {
        // no-op by default
    }
}
