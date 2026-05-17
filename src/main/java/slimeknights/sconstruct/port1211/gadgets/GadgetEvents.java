package slimeknights.sconstruct.port1211.gadgets;

import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Game-bus event handling for the Phase-6 gadgets. SMTCON-134 wires the piggyback ride logic:
 * the {@link PiggybackData} attachment is the persistent source of truth for a ride, and the
 * vanilla passenger link is re-derived from it each server tick.
 *
 * <p>Each server tick a player is handled by the side their attachment marks them as:
 * <ul>
 *   <li><strong>rider</strong> ({@code mount} present) — sneaking ends the piggyback (the rider
 *       climbs off and both attachments clear); otherwise the vanilla passenger link is
 *       re-established whenever it is missing (lost to a logout or a respawn), but only while
 *       the carrier still records this player back.</li>
 *   <li><strong>carrier</strong> ({@code rider} present) — if the recorded rider is online but
 *       no longer riding this carrier, the carrier's stale attachment is cleared. While the
 *       rider is offline the record is kept, so a suspended ride resumes on their return.</li>
 * </ul>
 *
 * <p>Re-deriving the link from the attachment is what carries a piggyback through a logout /
 * login: the vanilla {@code Passengers} relationship does not survive a player leaving the
 * world, but the serialized attachment does. The two sides validate each other so a ride ended
 * while one player was offline cannot leave a half-cleared, stuck attachment behind.
 */
public final class GadgetEvents {

    private GadgetEvents() {
    }

    /** Subscribes the gadget game-bus listeners. Called from {@code SConstruct} with the NeoForge bus. */
    public static void register(IEventBus neoForgeBus) {
        neoForgeBus.addListener(GadgetEvents::onPlayerTick);
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
