package slimeknights.sconstruct.gadgets.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import slimeknights.sconstruct.gadgets.GadgetAttachments;
import slimeknights.sconstruct.gadgets.PiggybackData;

/**
 * The piggyback item — right-click another player to put them on your shoulders. The clicked
 * player becomes the <em>rider</em>, riding the player holding the item (the <em>carrier</em>)
 * via the vanilla passenger system.
 *
 * <p>The ride is recorded in a {@link PiggybackData} attachment on both players: the carrier's
 * {@code rider} slot and the rider's {@code mount} slot. {@code GadgetEvents} reads that
 * attachment to re-establish the passenger link after a logout and to handle dismounting, so
 * the ride survives logout / login and (via the attachment's {@code copyOnDeath}) a respawn.
 *
 * <p>A piggyback only forms when neither player is already part of one — a carrier can hold a
 * single rider, and a rider rides a single carrier.
 */
public class PiggybackItem extends Item {

    public PiggybackItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!(interactionTarget instanceof Player rider)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            // Let the client play the arm-swing; the mount itself is applied server-side.
            return InteractionResult.SUCCESS;
        }
        // A piggyback only forms between two unencumbered players: neither may already be
        // carrying or riding someone, and a player cannot piggyback themselves. Check both the
        // vanilla passenger state and the piggyback attachment, so a stale ride record (left by
        // a carrier who logged out) cannot be silently overwritten by a fresh mount.
        if (player.equals(rider) || player.isPassenger() || player.isVehicle() || rider.isPassenger() || rider.isVehicle()) {
            return InteractionResult.PASS;
        }
        if (!player.getData(GadgetAttachments.PIGGYBACK.get()).isEmpty() || !rider.getData(GadgetAttachments.PIGGYBACK.get()).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!rider.startRiding(player, true)) {
            return InteractionResult.PASS;
        }
        // withRider / withMount build a fresh single-sided record, so there is no need to read
        // the players' current attachment first — it is already known empty by the guard above.
        player.setData(GadgetAttachments.PIGGYBACK.get(), PiggybackData.EMPTY.withRider(rider.getUUID()));
        rider.setData(GadgetAttachments.PIGGYBACK.get(), PiggybackData.EMPTY.withMount(player.getUUID()));
        return InteractionResult.SUCCESS;
    }
}
