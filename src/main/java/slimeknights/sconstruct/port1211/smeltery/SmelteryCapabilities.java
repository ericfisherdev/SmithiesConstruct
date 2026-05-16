package slimeknights.sconstruct.port1211.smeltery;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import slimeknights.sconstruct.port1211.smeltery.block.entity.AbstractCastingBlockEntity;

/**
 * Smeltery-pulse capability registrations. Phase 1 shipped an empty stub so the central
 * {@code TinkerCapabilities} dispatcher had a real callsite to invoke; SMTCON-113 fills in the
 * first real bindings — exposing the casting table and casting basin fluid tanks to the
 * {@code Capabilities.FluidHandler.BLOCK} channel so a fluid source can pour molten metal into
 * them. SMTCON-114 adds the smeltery controller's tank and melting-slot handlers. Further
 * smeltery fluid handlers (seared tanks) append here as that content lands.
 */
public final class SmelteryCapabilities {

    private SmelteryCapabilities() {
    }

    /**
     * Hook for {@code TinkerCapabilities} to delegate the smeltery's capability registrations
     * into. Binds the {@link AbstractCastingBlockEntity} fluid tanks of the casting table and
     * basin, and the smeltery controller's tank and melting-slot inventory, to the standard
     * NeoForge capability channels.
     *
     * <p>The side parameter is intentionally ignored throughout — the handlers are exposed on
     * every face. In practice a casting block is filled from above and the smeltery controller
     * is reached through its drains and chutes, but a side-agnostic binding also lets a piped
     * setup connect from any direction without inventing per-face rules the design does not
     * need.
     */
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CastingBlocks.CASTING_TABLE_BE.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CastingBlocks.CASTING_BASIN_BE.get(), (be, side) -> be.getFluidHandler());
        // SMTCON-114: the smeltery controller exposes its molten-metal tank for drains to pull
        // from and its melting-slot inventory for chutes to push items into.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SmelteryComponents.SMELTERY_CONTROLLER_BE.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SmelteryComponents.SMELTERY_CONTROLLER_BE.get(), (be, side) -> be.getItemHandler());
    }
}
