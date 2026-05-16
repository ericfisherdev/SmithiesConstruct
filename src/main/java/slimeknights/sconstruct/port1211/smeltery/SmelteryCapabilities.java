package slimeknights.sconstruct.port1211.smeltery;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import slimeknights.sconstruct.port1211.smeltery.block.entity.AbstractCastingBlockEntity;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryComponentBlockEntity;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryControllerBlockEntity;

/**
 * Smeltery-pulse capability registrations. Phase 1 shipped an empty stub so the central
 * {@code TinkerCapabilities} dispatcher had a real callsite to invoke; SMTCON-113 fills in the
 * first real bindings — exposing the casting table and casting basin fluid tanks to the
 * {@code Capabilities.FluidHandler.BLOCK} channel so a fluid source can pour molten metal into
 * them. SMTCON-114 adds the smeltery controller's tank and melting-slot handlers. SMTCON-116
 * adds the component proxies — the seared drain and the two seared tanks forward the fluid
 * handler, and the seared chute forwards the item handler, of whatever controller has claimed
 * them, so a smeltery behaves as one connected machine to neighbouring pipes and hoppers.
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
     *
     * <p>The component proxies resolve their target lazily on every query via
     * {@link SmelteryComponentBlockEntity#getControllerOpt()}: a loose component, or one whose
     * controller has been broken, resolves to {@code null} so the capability is simply absent
     * until the smeltery is (re)assembled.
     */
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CastingBlocks.CASTING_TABLE_BE.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CastingBlocks.CASTING_BASIN_BE.get(), (be, side) -> be.getFluidHandler());
        // SMTCON-114: the smeltery controller exposes its molten-metal tank for drains to pull
        // from and its melting-slot inventory for chutes to push items into.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SmelteryComponents.SMELTERY_CONTROLLER_BE.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SmelteryComponents.SMELTERY_CONTROLLER_BE.get(), (be, side) -> be.getItemHandler());
        // SMTCON-116: the seared drain and the two seared tanks proxy the controller's tank, so
        // a bucket or pipe against any of them pulls from (or fills) the one smeltery tank.
        registerFluidProxy(event, SmelteryComponents.DRAIN_BE.get());
        registerFluidProxy(event, SmelteryComponents.TANK_IO_BE.get());
        registerFluidProxy(event, SmelteryComponents.TANK_IN_BE.get());
        // The seared chute proxies the controller's melting-slot inventory, so a hopper feeding
        // the chute drops items straight into the smeltery's melting slots.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SmelteryComponents.CHUTE_BE.get(),
                (be, side) -> be.getControllerOpt().map(SmelteryControllerBlockEntity::getItemHandler).orElse(null));
    }

    /** Bind a component block-entity type's fluid handler proxy to its claimed controller's tank. */
    private static void registerFluidProxy(RegisterCapabilitiesEvent event, net.minecraft.world.level.block.entity.BlockEntityType<SmelteryComponentBlockEntity> type) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> be.getControllerOpt().map(SmelteryControllerBlockEntity::getFluidHandler).orElse(null));
    }
}
