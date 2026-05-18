package slimeknights.sconstruct.gadgets;

import java.util.UUID;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import slimeknights.sconstruct.gadgets.item.SlimeArmorItem;

/**
 * Game-bus event handling for the Phase-6 gadgets.
 *
 * <p><strong>Piggyback ride (SMTCON-134).</strong> The {@link PiggybackData} attachment is the
 * persistent source of truth for a ride; the vanilla passenger link is re-derived from it each
 * server tick. A player is handled by the side their attachment marks them as:
 * <ul>
 *   <li><strong>rider</strong> ({@code mount} present) — sneaking ends the piggyback (the rider
 *       climbs off and both attachments clear); otherwise the vanilla passenger link is
 *       re-established whenever it is missing (lost to a logout or a respawn), but only while
 *       the carrier still records this player back.</li>
 *   <li><strong>carrier</strong> ({@code rider} present) — if the recorded rider is online but
 *       no longer riding this carrier, the carrier's stale attachment is cleared. While the
 *       rider is offline the record is kept, so a suspended ride resumes on their return.</li>
 * </ul>
 * Re-deriving the link from the attachment is what carries a piggyback through a logout /
 * login: the vanilla {@code Passengers} relationship does not survive a player leaving the
 * world, but the serialized attachment does.
 *
 * <p><strong>Slime armor (SMTCON-136).</strong> Slime boots cushion landings — a fall taken in
 * slime boots deals {@link #FALL_DAMAGE_MULTIPLIER} of its normal damage. Wearing the full
 * four-piece set adds a knockback-softening bonus: incoming knockback is scaled down by
 * {@link #SET_BONUS_KNOCKBACK_FACTOR}.
 */
public final class GadgetEvents {

    /** Fraction of fall damage taken while wearing slime boots — the rest is cushioned away. */
    private static final float FALL_DAMAGE_MULTIPLIER = 0.4F;

    /** Fraction of incoming knockback strength kept while wearing the full slime armor set. */
    private static final float SET_BONUS_KNOCKBACK_FACTOR = 0.5F;

    private GadgetEvents() {
    }

    /** Subscribes the gadget game-bus listeners. Called from {@code SConstruct} with the NeoForge bus. */
    public static void register(IEventBus neoForgeBus) {
        neoForgeBus.addListener(GadgetEvents::onPlayerTick);
        neoForgeBus.addListener(GadgetEvents::onLivingFall);
        neoForgeBus.addListener(GadgetEvents::onLivingKnockBack);
    }

    /** Slime boots cushion a fall — the fall's damage multiplier is scaled down. */
    private static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity().getItemBySlot(EquipmentSlot.FEET).getItem() instanceof SlimeArmorItem) {
            event.setDamageMultiplier(event.getDamageMultiplier() * FALL_DAMAGE_MULTIPLIER);
        }
    }

    /** The full slime armor set softens incoming knockback. */
    private static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (wearsFullSlimeSet(event.getEntity())) {
            event.setStrength(event.getStrength() * SET_BONUS_KNOCKBACK_FACTOR);
        }
    }

    /** Whether {@code entity} is wearing slime armor in all four armor slots. */
    private static boolean wearsFullSlimeSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof SlimeArmorItem && entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof SlimeArmorItem
                && entity.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof SlimeArmorItem && entity.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof SlimeArmorItem;
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        PiggybackData data = player.getData(GadgetAttachments.PIGGYBACK.get());
        if (data.mount().isPresent()) {
            handleRider(player, data.mount().get());
        }
        else if (data.rider().isPresent()) {
            handleCarrier(player, data.rider().get());
        }
    }

    /** Per-tick handling for a rider — dismount on sneak, otherwise keep the ride established. */
    private static void handleRider(Player rider, UUID mountId) {
        // Sneaking while riding is the dismount input — the rider climbs off.
        if (rider.isShiftKeyDown()) {
            endPiggyback(rider, mountId);
            return;
        }
        // Already riding the recorded carrier — the passenger link is intact.
        if (rider.getVehicle() instanceof Player vehicle && vehicle.getUUID().equals(mountId)) {
            return;
        }
        // The passenger link is missing — re-establish it once the carrier is back in the world.
        // While the carrier is offline this simply retries on a later tick.
        Player mount = rider.level().getPlayerByUUID(mountId);
        if (mount == null) {
            return;
        }
        // Only re-mount if the carrier still records this player as its rider. A carrier whose
        // attachment no longer points back (the ride was ended while this rider was offline)
        // leaves a stale mount UUID here — clear it rather than forcing an asymmetric ride.
        PiggybackData mountData = mount.getData(GadgetAttachments.PIGGYBACK.get());
        if (mountData.rider().isPresent() && mountData.rider().get().equals(rider.getUUID())) {
            if (mount.isAlive() && !mount.isVehicle()) {
                rider.startRiding(mount, true);
            }
        }
        else {
            rider.setData(GadgetAttachments.PIGGYBACK.get(), PiggybackData.EMPTY);
        }
    }

    /** Per-tick handling for a carrier — drop a stale rider record once that rider is verifiably gone. */
    private static void handleCarrier(Player carrier, UUID riderId) {
        Player rider = carrier.level().getPlayerByUUID(riderId);
        if (rider == null) {
            // The rider is offline — keep the record so the suspended ride resumes on their return.
            return;
        }
        // The rider is online: the record is valid only while the rider still rides this carrier.
        PiggybackData riderData = rider.getData(GadgetAttachments.PIGGYBACK.get());
        if (riderData.mount().isEmpty() || !riderData.mount().get().equals(carrier.getUUID())) {
            carrier.setData(GadgetAttachments.PIGGYBACK.get(), PiggybackData.EMPTY);
        }
    }

    /** Ends a piggyback: the rider dismounts and both players' attachments are cleared. */
    private static void endPiggyback(Player rider, UUID mountId) {
        if (rider.isPassenger()) {
            rider.stopRiding();
        }
        rider.setData(GadgetAttachments.PIGGYBACK.get(), PiggybackData.EMPTY);
        Player mount = rider.level().getPlayerByUUID(mountId);
        if (mount != null) {
            mount.setData(GadgetAttachments.PIGGYBACK.get(), PiggybackData.EMPTY);
        }
    }
}
