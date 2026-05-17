package slimeknights.sconstruct.port1211.gadgets;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Persistent piggyback ride state, attached to a player via {@code GadgetAttachments#PIGGYBACK}.
 * A player can be on either side of a piggyback — or neither:
 *
 * <ul>
 *   <li>{@link #rider()} present — this player is a <em>carrier</em>; the UUID is the player
 *       riding on their shoulders.</li>
 *   <li>{@link #mount()} present — this player is a <em>rider</em>; the UUID is the carrier
 *       they are riding.</li>
 *   <li>both empty ({@link #EMPTY}) — this player is not part of any piggyback.</li>
 * </ul>
 *
 * <p>The record is the source of truth for the ride, not the vanilla passenger relationship:
 * the passenger link is lost when either player logs out, so {@code GadgetEvents} re-establishes
 * it each tick from the {@link #mount()} UUID. {@code copyOnDeath} on the attachment carries the
 * record through a respawn.
 */
public record PiggybackData(Optional<UUID> rider, Optional<UUID> mount) {

    /**
     * Enforces the invariant that a player is on at most one side of a piggyback: either a
     * carrier ({@code rider} present) or a rider ({@code mount} present), or neither — never
     * both. The {@code withRider} / {@code withMount} mutators clear the opposing slot, so the
     * only way to reach a both-present state is hand-edited NBT, which this rejects loudly.
     */
    public PiggybackData {
        if (rider.isPresent() && mount.isPresent()) {
            throw new IllegalArgumentException("a player cannot be both a piggyback carrier and a rider at once");
        }
    }

    /** Shared empty instance — a player not involved in any piggyback. */
    public static final PiggybackData EMPTY = new PiggybackData(Optional.empty(), Optional.empty());

    /** Codec persisting both UUID slots as optional fields in the player's attachment NBT. */
    public static final Codec<PiggybackData> CODEC = RecordCodecBuilder
            .create(builder -> builder.group(UUIDUtil.CODEC.optionalFieldOf("rider").forGetter(PiggybackData::rider), UUIDUtil.CODEC.optionalFieldOf("mount").forGetter(PiggybackData::mount))
                    .apply(builder, PiggybackData::new));

    /**
     * Marks this player as a carrier with {@code riderId} on their shoulders. The {@code mount}
     * slot is cleared so the result never holds both sides of a piggyback at once.
     */
    public PiggybackData withRider(UUID riderId) {
        return new PiggybackData(Optional.of(riderId), Optional.empty());
    }

    /**
     * Marks this player as a rider on the carrier {@code mountId}. The {@code rider} slot is
     * cleared so the result never holds both sides of a piggyback at once.
     */
    public PiggybackData withMount(UUID mountId) {
        return new PiggybackData(Optional.empty(), Optional.of(mountId));
    }

    /** Whether this player is part of no piggyback at all. */
    public boolean isEmpty() {
        return rider.isEmpty() && mount.isEmpty();
    }
}
