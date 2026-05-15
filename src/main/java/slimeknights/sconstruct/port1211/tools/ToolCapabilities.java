package slimeknights.sconstruct.port1211.tools;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import slimeknights.sconstruct.port1211.tools.block.entity.PatternChestBlockEntity;

/**
 * Tool-pulse capability registrations. Phase 1 shipped an empty stub so the central
 * {@code TinkerCapabilities} dispatcher had a real callsite to invoke; SMTCON-90 fills in the
 * first real registration here — exposing the 32-slot pattern chest's {@link ItemStackHandler}
 * to the {@code Capabilities.ItemHandler.BLOCK} channel so hoppers, droppers, and any other
 * neighbour that queries the chest's inventory through the standard NeoForge capability API
 * get back the same handler the player sees through the GUI.
 *
 * <p>The registration is side-agnostic — the supplied side parameter is ignored, returning the
 * single backing handler regardless of which face the neighbour queries from. This matches
 * vanilla chest behaviour (insert from any face) and is the simplest hopper-compatible shape.
 */
public final class ToolCapabilities {

    private ToolCapabilities() {
    }

    /**
     * Hook for {@code TinkerCapabilities} to delegate the tools' capability registrations
     * into. Registers the {@link PatternChestBlockEntity}'s item handler against
     * {@link Capabilities.ItemHandler#BLOCK} so neighbours can read/write the chest's
     * inventory via the standard capability lookup.
     */
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PatternChestRegistry.PATTERN_CHEST_BE.get(),
                // Side parameter is intentionally unused — the chest exposes one handler on
                // every face. Per-face filtering is a follow-up if/when the design wants the
                // bottom face to only emit and the top to only receive.
                (be, side) -> be.getHandler());
    }
}
