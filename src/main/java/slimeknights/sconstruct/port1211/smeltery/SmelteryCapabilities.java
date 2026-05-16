package slimeknights.sconstruct.port1211.smeltery;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import slimeknights.sconstruct.port1211.smeltery.block.entity.AbstractCastingBlockEntity;

/**
 * Smeltery-pulse capability registrations. Phase 1 shipped an empty stub so the central
 * {@code TinkerCapabilities} dispatcher had a real callsite to invoke; SMTCON-113 fills in the
 * first real bindings — exposing the casting table and casting basin fluid tanks to the
 * {@code Capabilities.FluidHandler.BLOCK} channel so a fluid source can pour molten metal into
 * them. Further smeltery fluid handlers (seared tanks, the smeltery controller) append here as
 * that content lands.
 */
public final class SmelteryCapabilities {

    private SmelteryCapabilities() {
    }

    /**
     * Hook for {@code TinkerCapabilities} to delegate the smeltery's capability registrations
     * into. Binds the {@link AbstractCastingBlockEntity} fluid tanks of the casting table and
     * basin to {@link Capabilities.FluidHandler#BLOCK}.
     *
     * <p>The side parameter is intentionally ignored — the tank is exposed on every face. In
     * practice a casting block is filled by a fluid source above it, but a side-agnostic
     * handler also lets a piped setup feed it from any direction without inventing per-face
     * rules the casting design does not need.
     */
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CastingBlocks.CASTING_TABLE_BE.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CastingBlocks.CASTING_BASIN_BE.get(), (be, side) -> be.getFluidHandler());
    }
}
