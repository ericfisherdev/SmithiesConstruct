package slimeknights.sconstruct.tools;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import slimeknights.sconstruct.tools.block.entity.PatternChestBlockEntity;

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
        // SMTCON-91: stencil table's combined 2-slot handler. The handler enforces blank-only
        // insertion on the input slot and rejects all insertion on the output slot at the
        // handler layer (see StencilTableBlockEntity#isItemValid), so hoppers can feed blank
        // patterns into the table but cannot bypass the cycle button by pre-stamping the
        // output slot.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, StencilTableRegistry.STENCIL_TABLE_BE.get(), (be, side) -> be.getHandler());
        // SMTCON-92: part builder's 3-slot handler. The handler enforces typed-pattern-only on
        // the pattern slot and rejects all insertion on the output slot at the handler layer
        // (see PartBuilderBlockEntity#isItemValid), so hoppers can feed typed patterns +
        // material stacks but cannot bypass the resolve step by pre-stamping the output slot.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PartBuilderRegistry.PART_BUILDER_BE.get(), (be, side) -> be.getHandler());
        // SMTCON-93: tool station + tool forge. Both BEs share the same 7-slot handler shape
        // (6 inputs + 1 output). The handler rejects insertion to the output slot at the
        // handler layer (see ToolStationBlockEntity#isItemValid), so hoppers feeding the
        // station cannot pre-stamp the output and bypass the build / modify resolution.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ToolStationRegistry.TOOL_STATION_BE.get(), (be, side) -> be.getHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ToolStationRegistry.TOOL_FORGE_BE.get(), (be, side) -> be.getHandler());
    }
}
