package slimeknights.tconstruct.port1211.common.pulse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.function.Consumer;

import net.neoforged.bus.api.IEventBus;

import org.junit.jupiter.api.Test;

class PulseLoaderTest {

    @Test
    void disabledPulseIsSkippedEntirely() {
        IEventBus bus = mock(IEventBus.class);
        Pulse enabled = spy(new NamedPulse("enabled"));
        Pulse disabled = spy(new NamedPulse("disabled"));
        PulseGate gate = (id, def) -> "enabled".equals(id);

        PulseLoader.boot(bus, List.of(enabled, disabled), gate, true);

        verify(enabled).register(bus);
        verify(disabled, never()).register(any());
    }

    @Test
    void enabledClientPulseAddsBothSetupAndClientSetupListeners() {
        IEventBus bus = mock(IEventBus.class);

        PulseLoader.boot(bus, List.of(new NamedPulse("p")), PulseGate.allowAllDefaults(), true);

        verify(bus, times(2)).addListener(any(Consumer.class));
    }

    @Test
    void enabledDedicatedServerPulseSkipsClientSetupSubscription() {
        IEventBus bus = mock(IEventBus.class);

        PulseLoader.boot(bus, List.of(new NamedPulse("p")), PulseGate.allowAllDefaults(), false);

        verify(bus, times(1)).addListener(any(Consumer.class));
    }

    @Test
    void disabledByDefaultPulseStaysOffUnderAllowAllDefaultsGate() {
        IEventBus bus = mock(IEventBus.class);
        Pulse optInOnly = spy(new NamedPulse("opt-in", false));

        PulseLoader.boot(bus, List.of(optInOnly), PulseGate.allowAllDefaults(), true);

        verify(optInOnly, never()).register(any());
        verify(bus, never()).addListener(any(Consumer.class));
    }

    /** Minimal {@link Pulse} that varies only the id and default-enabled flag. */
    static final class NamedPulse implements Pulse {
        private final String id;
        private final boolean defaultEnabled;

        NamedPulse(String id) {
            this(id, true);
        }

        NamedPulse(String id, boolean defaultEnabled) {
            this.id = id;
            this.defaultEnabled = defaultEnabled;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean defaultEnabled() {
            return defaultEnabled;
        }
    }
}
