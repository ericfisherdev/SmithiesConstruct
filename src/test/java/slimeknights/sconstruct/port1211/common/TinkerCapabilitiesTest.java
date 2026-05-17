package slimeknights.sconstruct.port1211.common;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import slimeknights.sconstruct.port1211.tools.ToolCapabilities;

/**
 * Pinned-behaviour tests for {@link TinkerCapabilities}. Uses Mockito's {@code mockStatic}
 * over the tool capability helper to verify the dispatcher fans the event out to it — a future
 * refactor that accidentally drops the delegation from {@link TinkerCapabilities#configure}
 * fires here loudly.
 *
 * <p>The smeltery and gadget helpers are intentionally absent: {@code TinkerSmelteryPulse} and
 * {@code TinkerGadgetsPulse} subscribe their own capability helpers so the bindings are skipped
 * when the pulse is disabled, so this dispatcher only fans out to the tool helper.
 */
class TinkerCapabilitiesTest {

    @Test
    void configureDispatchesToTheToolHelper() {
        RegisterCapabilitiesEvent event = mock(RegisterCapabilitiesEvent.class);

        try (MockedStatic<ToolCapabilities> tool = mockStatic(ToolCapabilities.class)) {
            TinkerCapabilities.configure(event);

            tool.verify(() -> ToolCapabilities.register(event), times(1));
        }
    }

    @Test
    void configurePassesTheExactEventInstanceToTheHelper() {
        // Defends against a future refactor that wraps the event before delegating — if the
        // helper gets a different reference than the one the dispatcher received, a capability
        // registration would fire against a stale view.
        RegisterCapabilitiesEvent event = mock(RegisterCapabilitiesEvent.class);

        try (MockedStatic<ToolCapabilities> tool = mockStatic(ToolCapabilities.class)) {
            TinkerCapabilities.configure(event);

            // verify with an exact arg matcher fails if a wrapped/derived event reaches the helper.
            tool.verify(() -> ToolCapabilities.register(event));
        }
    }
}
