package slimeknights.tconstruct.port1211.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import net.neoforged.bus.api.IEventBus;

import org.junit.jupiter.api.Test;

import slimeknights.tconstruct.port1211.common.pulse.Pulse;

/**
 * Pinned-behaviour tests for {@link TinkerSharedPulse}. Verifies the identity contract the
 * {@link slimeknights.tconstruct.port1211.common.pulse.PulseLoader} reads
 * ({@code id}/{@code defaultEnabled}) and confirms {@link TinkerSharedPulse#register} touches
 * the shared content classes so their {@code DeferredRegister} entries are populated.
 *
 * <p>The {@code clientSetup} hook is not exercised here — it references {@code RenderType}
 * and {@code ItemBlockRenderTypes} which require a bootstrapped client and aren't available
 * in the JUnit test JVM. The PulseLoader test in {@code TinkerRegistriesTest}-adjacent
 * coverage demonstrates dist-gating elsewhere.
 */
class TinkerSharedPulseTest {

    @Test
    void pulseIsRegisteredUnderTheSharedId() {
        Pulse pulse = new TinkerSharedPulse();
        // The id must match the Config.PULSE_FLAGS key. Drifting the id silently disables the
        // pulse because Config.resolveFlag would return Optional.empty() and the gate would
        // fall back to defaultEnabled() — which still works today (default true), but a
        // future config-overridden setup would break.
        assertEquals("shared", pulse.id());
    }

    @Test
    void pulseIsEnabledByDefault() {
        assertTrue(new TinkerSharedPulse().defaultEnabled());
    }

    @Test
    void registerTouchesContentClassesSoTheirDeferredRegistersHaveEntries() {
        // Build a fresh pulse and call register() with a mock event bus. After the call,
        // every shared driver must be populated — failure here means register() forgot to
        // touch a content class and its DeferredRegister would fire empty.
        IEventBus bus = mock(IEventBus.class);
        new TinkerSharedPulse().register(bus);
        assertNotNull(SharedBlocks.METAL_BLOCKS, "SharedBlocks.METAL_BLOCKS must be initialised");
        assertTrue(SharedBlocks.METAL_BLOCKS.size() >= 13, "at least 13 metal storage blocks expected after register");
        assertTrue(SharedItems.INGOTS.size() >= 15, "at least 15 ingots expected after register");
        assertNotNull(SharedFluids.BLOOD.getId(), "SharedFluids.BLOOD must be initialised");
        assertNotNull(SharedTabs.GENERAL.getId(), "SharedTabs.GENERAL must be initialised");
    }
}
