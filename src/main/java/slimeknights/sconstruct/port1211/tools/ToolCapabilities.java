package slimeknights.sconstruct.port1211.tools;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Tool-pulse capability registrations. Phase 1 ships an empty stub so the central
 * {@code TinkerCapabilities} dispatcher has a real callsite to invoke; Phase 2+ pulses
 * append per-tool capability bindings (item-handler views on tool inventories, energy on
 * drained-modifier tools, etc.) here as the tool content comes online.
 */
public final class ToolCapabilities {

    private ToolCapabilities() {
    }

    /**
     * Hook for {@code TinkerCapabilities} to delegate the tools' capability registrations
     * into. Empty in Phase 1 — the dispatcher's call into this method is itself the AC.
     */
    public static void register(RegisterCapabilitiesEvent event) {
        // Phase 2+: event.registerItem(Capabilities.ItemHandler.ITEM,
        //              ToolInventoryAccess::new, TinkerTools.HARVEST_TOOLS);
    }
}
