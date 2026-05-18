package slimeknights.sconstruct.common.pulse;

import java.util.List;
import java.util.Objects;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Wires a {@link List} of {@link Pulse}s into the mod event bus, honouring a {@link PulseGate}
 * so that disabled pulses are skipped entirely — no register call, no setup subscription, no
 * resource allocation.
 *
 * <p>This is the in-house replacement for Mantle's Pulsar (no 1.21.x port). Production callers
 * use {@link #boot(IEventBus, List, PulseGate)}; tests use {@link #boot(IEventBus, List,
 * PulseGate, boolean)} to control the client/server branch without involving
 * {@link FMLEnvironment}.
 */
public final class PulseLoader {

    private PulseLoader() {
    }

    /**
     * Production entry point. Determines the client/server branch from
     * {@link FMLEnvironment#dist} so client-only setup is never subscribed on a dedicated
     * server, then delegates to the explicit overload.
     */
    public static void boot(IEventBus modBus, List<Pulse> pulses, PulseGate gate) {
        boot(modBus, pulses, gate, FMLEnvironment.dist == Dist.CLIENT);
    }

    /**
     * Test-and-production-shared overload that takes the {@code isClient} branch as an explicit
     * parameter. Package-private to keep the production contract narrow while still letting
     * unit tests in the same package drive both branches deterministically.
     */
    static void boot(IEventBus modBus, List<Pulse> pulses, PulseGate gate, boolean isClient) {
        Objects.requireNonNull(modBus, "modBus");
        Objects.requireNonNull(pulses, "pulses");
        Objects.requireNonNull(gate, "gate");

        for (Pulse pulse : pulses) {
            Objects.requireNonNull(pulse, "pulses contains a null entry");
            if (!gate.isEnabled(pulse.id(), pulse.defaultEnabled())) {
                continue;
            }
            pulse.register(modBus);
            modBus.addListener(pulse::setup);
            if (isClient) {
                modBus.addListener(pulse::clientSetup);
            }
        }
    }
}
