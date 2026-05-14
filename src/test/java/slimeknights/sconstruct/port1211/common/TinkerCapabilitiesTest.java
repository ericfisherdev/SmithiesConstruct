package slimeknights.sconstruct.port1211.common;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import slimeknights.sconstruct.port1211.gadgets.GadgetCapabilities;
import slimeknights.sconstruct.port1211.smeltery.SmelteryCapabilities;
import slimeknights.sconstruct.port1211.tools.ToolCapabilities;

/**
 * Pinned-behaviour tests for {@link TinkerCapabilities}. Uses Mockito's {@code mockStatic}
 * over the three per-pulse helper classes to verify the dispatcher fans the event out to
 * each one exactly once — a future refactor that accidentally drops a pulse from
 * {@link TinkerCapabilities#configure} fires here loudly.
 */
class TinkerCapabilitiesTest {

    @Test
    void configureDispatchesToEveryPulseHelper() {
        RegisterCapabilitiesEvent event = mock(RegisterCapabilitiesEvent.class);

        try (MockedStatic<SmelteryCapabilities> smeltery = mockStatic(SmelteryCapabilities.class);
                MockedStatic<GadgetCapabilities> gadget = mockStatic(GadgetCapabilities.class);
                MockedStatic<ToolCapabilities> tool = mockStatic(ToolCapabilities.class)) {

            TinkerCapabilities.configure(event);

            smeltery.verify(() -> SmelteryCapabilities.register(event), times(1));
            gadget.verify(() -> GadgetCapabilities.register(event), times(1));
            tool.verify(() -> ToolCapabilities.register(event), times(1));
        }
    }

    @Test
    void configurePassesTheExactEventInstanceToEachHelper() {
        // Defends against a future refactor that wraps the event before delegating — if any
        // helper gets a different reference than the one the dispatcher received, a per-pulse
        // capability registration would fire against a stale view.
        RegisterCapabilitiesEvent event = mock(RegisterCapabilitiesEvent.class);

        try (MockedStatic<SmelteryCapabilities> smeltery = mockStatic(SmelteryCapabilities.class);
                MockedStatic<GadgetCapabilities> gadget = mockStatic(GadgetCapabilities.class);
                MockedStatic<ToolCapabilities> tool = mockStatic(ToolCapabilities.class)) {

            TinkerCapabilities.configure(event);

            // Each verify checks both arity AND argument identity — Mockito's verify with an
            // exact arg matcher fails if a wrapped/derived event reaches the helper.
            smeltery.verify(() -> SmelteryCapabilities.register(event));
            gadget.verify(() -> GadgetCapabilities.register(event));
            tool.verify(() -> ToolCapabilities.register(event));
        }
    }
}
