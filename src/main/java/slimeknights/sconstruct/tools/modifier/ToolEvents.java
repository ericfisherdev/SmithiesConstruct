package slimeknights.sconstruct.tools.modifier;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Neutral context-record holder behind {@link Modifier}'s lifecycle hooks. The records here
 * wrap the bag of parameters vanilla event paths hand off — {@link
 * net.neoforged.neoforge.event.entity.living.LivingDamageEvent} for attacks, {@link
 * net.neoforged.neoforge.event.level.BlockEvent.BreakEvent} / mining tick for mines — so a
 * {@link Modifier} implementation can read the same fields regardless of which exact event
 * fired. The wrapping lets SMTCON-83's hooks dispatcher swap in a fake context for unit tests
 * without standing up the full vanilla event hierarchy.
 *
 * <p>All records are immutable value carriers; no methods beyond accessors. The hook
 * implementations are responsible for any mutation against the held entities / levels.
 */
// PMD's MissingStaticMethodInNonInstantiatableClass flags holder classes that exist purely
// to namespace nested types — ToolEvents owns the two context records and never needs a
// callable static surface beyond that. Suppress at the class level rather than spreading a
// no-op factory method that would add noise without value.
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class ToolEvents {

    private ToolEvents() {
    }

    /**
     * Attack-side context delivered to {@link Modifier#onAttack}. Carries the attacking
     * {@link Player}, the {@link LivingEntity} target, the {@link Level} the swing happened in,
     * and the {@code amount} of damage the attribute-modifier roll produced — useful for
     * modifiers that scale a side effect (fiery duration, knockback boost) against the base
     * damage figure.
     */
    public record OnHitContext(Player attacker, LivingEntity target, Level level, float baseDamage) {

        /** Compact constructor: every field is required; null is a calling-code bug we want to
         *  surface at the dispatch site rather than letting the null propagate into a modifier
         *  hook that would NPE on a less obvious dereference. */
        public OnHitContext {
            Objects.requireNonNull(attacker, "attacker");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(level, "level");
        }
    }

    /**
     * Mine-side context delivered to {@link Modifier#onMine}. Carries the mining {@link Player},
     * the {@link BlockState} being broken, the {@link BlockPos} the block sat at, and the
     * {@link Level} the mine happened in. Modifiers that drop alternate loot, smelt the drop,
     * or fortify the harvest read off these fields.
     */
    public record OnMineContext(Player miner, BlockState state, BlockPos pos, Level level) {

        public OnMineContext {
            Objects.requireNonNull(miner, "miner");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(pos, "pos");
            Objects.requireNonNull(level, "level");
        }
    }
}
