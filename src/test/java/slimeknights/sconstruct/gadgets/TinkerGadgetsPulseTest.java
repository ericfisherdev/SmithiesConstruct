package slimeknights.sconstruct.gadgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import slimeknights.sconstruct.gadgets.entity.GadgetEntities;
import slimeknights.sconstruct.gadgets.recipe.GadgetRecipes;

/**
 * Pinned-behaviour tests for {@link TinkerGadgetsPulse}. Verifies the identity contract the
 * {@link slimeknights.sconstruct.common.pulse.PulseLoader} reads, and confirms that
 * {@link TinkerGadgetsPulse#register} pumps every content hub's {@code init()}, wires the
 * gadget game-bus listeners, and subscribes the capability handler — using {@link MockedStatic}
 * so the assertions fail if {@code register()} stops doing any of these.
 *
 * <p>{@code GadgetItems} and {@code GadgetBlocks} are mocked with the default stubbing so their
 * {@code registerCreativeTabContents} no-ops — that keeps the only {@code modBus.addListener}
 * call the capability handler, so it can be captured precisely.
 */
class TinkerGadgetsPulseTest {

    @Test
    void pulseIsRegisteredUnderTheGadgetsId() {
        // The id must match the key in Config.PULSE_FLAGS for the gadgets pulse, else the gate
        // falls back to the declared default and ignores the operator's TOML override.
        assertEquals("gadgets", new TinkerGadgetsPulse().id());
    }

    @Test
    void pulseIsEnabledByDefault() {
        assertTrue(new TinkerGadgetsPulse().defaultEnabled());
    }

    @Test
    void registerSeedsEveryContentHubAndWiresTheListeners() {
        IEventBus bus = mock(IEventBus.class);
        try (MockedStatic<GadgetItems> items = mockStatic(GadgetItems.class);
                MockedStatic<GadgetBlocks> blocks = mockStatic(GadgetBlocks.class);
                MockedStatic<GadgetEntities> entities = mockStatic(GadgetEntities.class, CALLS_REAL_METHODS);
                MockedStatic<GadgetAttachments> attachments = mockStatic(GadgetAttachments.class, CALLS_REAL_METHODS);
                MockedStatic<GadgetArmorMaterials> armor = mockStatic(GadgetArmorMaterials.class, CALLS_REAL_METHODS);
                MockedStatic<GadgetRecipes> recipes = mockStatic(GadgetRecipes.class, CALLS_REAL_METHODS);
                MockedStatic<GadgetEvents> events = mockStatic(GadgetEvents.class)) {

            new TinkerGadgetsPulse().register(bus);

            items.verify(GadgetItems::init);
            blocks.verify(GadgetBlocks::init);
            entities.verify(GadgetEntities::init);
            attachments.verify(GadgetAttachments::init);
            armor.verify(GadgetArmorMaterials::init);
            recipes.verify(GadgetRecipes::init);
            // register() must wire the gadget gameplay listeners onto the NeoForge game bus.
            events.verify(() -> GadgetEvents.register(NeoForge.EVENT_BUS));
            assertRegisterWiresTheCapabilityListener(bus);
        }
    }

    /**
     * Confirms {@code register()} wired {@link GadgetCapabilities#register} onto the mod bus.
     * The handler is added as a method reference — the bus invokes it later, not during
     * {@code register()} — so the listener {@link Consumer} is captured and then driven with a
     * mock {@link RegisterCapabilitiesEvent}; only the real {@code GadgetCapabilities::register}
     * reference forwards that event into the mocked-static helper.
     */
    private static void assertRegisterWiresTheCapabilityListener(IEventBus bus) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<RegisterCapabilitiesEvent>> listener = ArgumentCaptor.forClass(Consumer.class);
        verify(bus).addListener(listener.capture());
        RegisterCapabilitiesEvent event = mock(RegisterCapabilitiesEvent.class);
        try (MockedStatic<GadgetCapabilities> capabilities = mockStatic(GadgetCapabilities.class)) {
            listener.getValue().accept(event);
            capabilities.verify(() -> GadgetCapabilities.register(event));
        }
    }
}
