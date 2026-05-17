package slimeknights.sconstruct.port1211.gadgets;

import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.gadgets.item.ThrowballItem;

/**
 * Wires dispenser support for the gadget projectiles. Every throwball and the glow ball
 * implement {@link net.minecraft.world.item.ProjectileItem}, so
 * {@link DispenserBlock#registerProjectileBehavior} gives each the standard projectile-dispense
 * behaviour — a dispenser facing outward fires them just as it would a snowball or egg.
 *
 * <p>{@code DispenserBlock}'s behaviour registry is not thread-safe, so {@link #register()}
 * must run on the main thread — {@code SConstruct} calls it inside
 * {@code FMLCommonSetupEvent#enqueueWork}.
 */
public final class GadgetDispenserBehaviors {

    private GadgetDispenserBehaviors() {
    }

    /** Registers the projectile-dispense behaviour for every throwball colour and the glow ball. */
    public static void register() {
        for (DeferredItem<ThrowballItem> throwball : GadgetItems.THROWBALLS) {
            DispenserBlock.registerProjectileBehavior(throwball.get());
        }
        DispenserBlock.registerProjectileBehavior(GadgetItems.GLOW_BALL.get());
    }
}
