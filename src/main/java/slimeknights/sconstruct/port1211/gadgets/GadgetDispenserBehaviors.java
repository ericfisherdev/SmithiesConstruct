package slimeknights.sconstruct.port1211.gadgets;

import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.gadgets.item.ThrowballItem;

/**
 * Wires dispenser support for the gadget projectiles. Every {@link ThrowballItem} implements
 * {@link net.minecraft.world.item.ProjectileItem}, so {@link DispenserBlock#registerProjectileBehavior}
 * gives it the standard projectile-dispense behaviour — a dispenser facing outward fires the
 * throwball just as it would a snowball or egg.
 *
 * <p>{@code DispenserBlock}'s behaviour registry is not thread-safe, so {@link #register()}
 * must run on the main thread — {@code SConstruct} calls it inside
 * {@code FMLCommonSetupEvent#enqueueWork}.
 */
public final class GadgetDispenserBehaviors {

    private GadgetDispenserBehaviors() {
    }

    /** Registers the projectile-dispense behaviour for every throwball colour. */
    public static void register() {
        for (DeferredItem<ThrowballItem> throwball : GadgetItems.THROWBALLS) {
            DispenserBlock.registerProjectileBehavior(throwball.get());
        }
    }
}
