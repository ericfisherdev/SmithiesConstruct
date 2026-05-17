package slimeknights.sconstruct.port1211.smeltery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import slimeknights.sconstruct.port1211.smeltery.recipe.SmelteryRecipes;

/**
 * Pinned-behaviour tests for {@link TinkerSmelteryPulse}. Verifies the identity contract the
 * {@link slimeknights.sconstruct.port1211.common.pulse.PulseLoader} reads, and confirms that
 * {@link TinkerSmelteryPulse#register} actively pumps every content hub's {@code init()} —
 * using {@link MockedStatic} so the assertion fails if {@code register()} stops calling one,
 * even though touching the static state would otherwise mask the regression by triggering
 * class-init independently.
 */
class TinkerSmelteryPulseTest {

    @Test
    void pulseIsRegisteredUnderTheSmelteryId() {
        // The id must match the key in Config.PULSE_FLAGS for the smeltery pulse, else the gate
        // falls back to the declared default and ignores the operator's TOML override.
        assertEquals("smeltery", new TinkerSmelteryPulse().id());
    }

    @Test
    void pulseIsEnabledByDefault() {
        assertTrue(new TinkerSmelteryPulse().defaultEnabled());
    }

    @Test
    void registerSeedsEveryContentHubBeforeTheirRegistryEvents() {
        // MockedStatic with CALLS_REAL_METHODS lets each init() still run its real (no-op) body
        // while letting us verify the call. Reading a static field directly would pass even if
        // register() stopped calling init() because the field access itself triggers class
        // initialisation — the verify calls below are the actual contract test. SmelteryEvents
        // is mocked so register() does not subscribe a real listener on the NeoForge bus.
        IEventBus bus = mock(IEventBus.class);
        try (MockedStatic<SmelteryFluids> fluids = mockStatic(SmelteryFluids.class, CALLS_REAL_METHODS);
                MockedStatic<SearedBlocks> seared = mockStatic(SearedBlocks.class, CALLS_REAL_METHODS);
                MockedStatic<SmelteryComponents> components = mockStatic(SmelteryComponents.class, CALLS_REAL_METHODS);
                MockedStatic<CastingBlocks> casting = mockStatic(CastingBlocks.class, CALLS_REAL_METHODS);
                MockedStatic<SmelteryRecipes> recipes = mockStatic(SmelteryRecipes.class, CALLS_REAL_METHODS);
                MockedStatic<SmelteryEvents> events = mockStatic(SmelteryEvents.class)) {

            new TinkerSmelteryPulse().register(bus);

            fluids.verify(SmelteryFluids::init);
            seared.verify(SearedBlocks::init);
            components.verify(SmelteryComponents::init);
            casting.verify(CastingBlocks::init);
            recipes.verify(SmelteryRecipes::init);
            // register() must also subscribe the disassembly listener on the NeoForge game bus.
            events.verify(() -> SmelteryEvents.register(NeoForge.EVENT_BUS));
        }
    }
}
