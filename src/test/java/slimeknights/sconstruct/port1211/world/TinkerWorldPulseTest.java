package slimeknights.sconstruct.port1211.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import net.neoforged.bus.api.IEventBus;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.common.pulse.Pulse;

/**
 * Pinned-behaviour tests for {@link TinkerWorldPulse}. Verifies the identity contract the
 * {@link slimeknights.sconstruct.port1211.common.pulse.PulseLoader} reads, and confirms that
 * {@link TinkerWorldPulse#register} populates the {@link WorldFluids} content list so the
 * pulse's DeferredRegisters fire with entries when the registry events run.
 */
class TinkerWorldPulseTest {

    @Test
    void pulseIsRegisteredUnderTheWorldId() {
        // The id must match the key in Config.PULSE_DECLARATIONS for the world pulse, else the
        // gate falls back to the declared default and ignores the operator's TOML override.
        assertEquals("world", new TinkerWorldPulse().id());
    }

    @Test
    void pulseIsEnabledByDefault() {
        assertTrue(new TinkerWorldPulse().defaultEnabled());
    }

    @Test
    void registerTouchesWorldFluidsSoItsDeferredRegistersHaveEntries() {
        // Build a fresh pulse and call register() with a mock event bus. After the call,
        // WorldFluids.ALL must be populated — failure here means register() forgot to touch
        // the content class and its DeferredRegisters would fire empty.
        IEventBus bus = mock(IEventBus.class);
        Pulse pulse = new TinkerWorldPulse();
        pulse.register(bus);
        assertEquals(4, WorldFluids.ALL.size(), "WorldFluids.ALL must hold all four slime fluids after register");
        assertNotNull(WorldFluids.SLIMEBLUE.source().getId(), "SLIMEBLUE source must be initialised");
    }
}
