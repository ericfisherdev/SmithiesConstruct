package slimeknights.sconstruct.common.client;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

/**
 * Client-side wiring for the {@code neoforge:fluid_container} bucket models (SMTCON-203).
 *
 * <p>{@link DynamicFluidContainerModel} masks the contained fluid's still texture into the
 * bucket cavity and stamps that fluid layer with tint index 1. Without a registered
 * {@link net.minecraft.client.color.item.ItemColor} the renderer treats tint index 1 as
 * untinted white, so fluids whose {@code IClientFluidTypeExtensions} return a non-white tint
 * (the molten metals) would render with the wrong colour. NeoForge ships the matching handler
 * as {@link DynamicFluidContainerModel.Colors} — it resolves the fluid contained in the stack
 * and returns its fluid-type tint — but mods must register it themselves against their bucket
 * items.
 *
 * <p>Each owning pulse (shared, world, smeltery) calls {@link #register(IEventBus, List)} with
 * its own fluid buckets behind a {@code Dist.CLIENT} guard, so a pulse disabled in config never
 * touches items that were never registered.
 */
public final class FluidContainerColorHandler {

    /** Shared, stateless handler — {@link DynamicFluidContainerModel.Colors} reads the stack. */
    private static final DynamicFluidContainerModel.Colors FLUID_COLORS = new DynamicFluidContainerModel.Colors();

    private FluidContainerColorHandler() {
    }

    /**
     * Subscribe a {@link RegisterColorHandlersEvent.Item} listener (mod bus) that binds the
     * NeoForge fluid-container {@link net.minecraft.client.color.item.ItemColor} to each of the
     * given bucket items. The item suppliers are resolved inside the event, by which point the
     * item registry has been populated.
     */
    public static void register(IEventBus modBus, List<? extends Supplier<? extends Item>> buckets) {
        modBus.addListener(RegisterColorHandlersEvent.Item.class, event -> {
            for (Supplier<? extends Item> bucket : buckets) {
                event.register(FLUID_COLORS, bucket.get());
            }
        });
    }
}
