package slimeknights.sconstruct.port1211.smeltery;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Smeltery-pulse capability registrations. Phase 1 ships an empty stub so the central
 * {@code TinkerCapabilities} dispatcher has a real callsite to invoke; Phase 2+ pulses
 * append per-block-entity capability bindings (fluid handlers on tanks, alloy controllers,
 * etc.) here as the smeltery content comes online.
 */
public final class SmelteryCapabilities {

    private SmelteryCapabilities() {
    }

    /**
     * Hook for {@code TinkerCapabilities} to delegate the smeltery's capability registrations
     * into. Empty in Phase 1 — the dispatcher's call into this method is itself the AC.
     */
    public static void register(RegisterCapabilitiesEvent event) {
        // Phase 2+: event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
        //              TinkerSmeltery.TANK_BE.get(), (be, side) -> be.getTank());
    }
}
