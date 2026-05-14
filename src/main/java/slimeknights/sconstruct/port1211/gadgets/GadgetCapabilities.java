package slimeknights.sconstruct.port1211.gadgets;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Gadget-pulse capability registrations. Phase 1 ships an empty stub so the central
 * {@code TinkerCapabilities} dispatcher has a real callsite to invoke; Phase 2+ pulses
 * append per-item capability bindings (energy storage on the drying racks' fuel slot,
 * sling-state on slime slings, etc.) here as the gadget content comes online.
 */
public final class GadgetCapabilities {

    private GadgetCapabilities() {
    }

    /**
     * Hook for {@code TinkerCapabilities} to delegate the gadgets' capability registrations
     * into. Empty in Phase 1 — the dispatcher's call into this method is itself the AC.
     */
    public static void register(RegisterCapabilitiesEvent event) {
        // Phase 2+: event.registerItem(Capabilities.EnergyStorage.ITEM,
        //              (stack, ctx) -> stack.get(...), TinkerGadgets.DRYING_RACK.get());
    }
}
