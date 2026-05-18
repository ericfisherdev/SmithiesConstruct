package slimeknights.sconstruct.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import net.neoforged.bus.api.IEventBus;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Pinned-behaviour tests for {@link TinkerWorldPulse}. Verifies the identity contract the
 * {@link slimeknights.sconstruct.common.pulse.PulseLoader} reads, and confirms that
 * {@link TinkerWorldPulse#register} actively invokes {@link WorldFluids#init()} and
 * {@link WorldBlocks#init()} — using {@link MockedStatic} so the assertion fails if
 * {@code register()} stops calling either method, even though touching the static state would
 * otherwise mask the regression by triggering class-init independently.
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
    void registerCallsWorldFluidsInitSoDeferredRegistersAreSeededBeforeTheirEvents() {
        // MockedStatic with CALLS_REAL_METHODS lets WorldFluids.init() still run its real body
        // (a no-op) while letting us verify the call. Reading a static field directly would
        // pass even if register() stopped calling init() because the field access itself
        // triggers class initialisation — the verify call below is the actual contract test.
        IEventBus bus = mock(IEventBus.class);
        try (MockedStatic<WorldFluids> worldFluids = mockStatic(WorldFluids.class, CALLS_REAL_METHODS)) {
            new TinkerWorldPulse().register(bus);
            worldFluids.verify(WorldFluids::init);
        }
    }

    @Test
    void registerCallsWorldBlocksInitSoDeferredRegistersAreSeededBeforeTheirEvents() {
        // Same MockedStatic contract for WorldBlocks — proves register() actively pumps the
        // class load, not just that some other code path has already initialised it.
        IEventBus bus = mock(IEventBus.class);
        try (MockedStatic<WorldBlocks> worldBlocks = mockStatic(WorldBlocks.class, CALLS_REAL_METHODS)) {
            new TinkerWorldPulse().register(bus);
            worldBlocks.verify(WorldBlocks::init);
        }
    }
}
