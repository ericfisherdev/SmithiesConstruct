package slimeknights.sconstruct.port1211.world.block;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;

/**
 * Per-colour identity and bounce-time behaviour for the four {@link TinkerSlimeBlock} variants.
 * The vanilla bounce/slow-walk physics live in {@code SlimeBlock} and are shared verbatim; this
 * enum only encodes what differs <em>per colour</em>: the registry id segment, the
 * {@link MapColor} surface tint, and an optional side-effect applied to the falling entity
 * before {@code SlimeBlock} runs its bounce mechanics.
 *
 * <p>{@link #BLUE} / {@link #PURPLE} use the default no-op {@link #applyFallEffect}; matches the
 * vanilla slime block exactly. {@link #MAGMA} applies {@code HOT_FLOOR} damage on landing —
 * thematically lifted from {@code MagmaBlock#stepOn}, but triggered on bounce because the
 * block's slipperiness makes a step-on rule fire effectively every tick. {@link #BLOOD} heals
 * one half-heart on landing when the falling entity is a {@link LivingEntity}.
 *
 * <p>Effects fire only on the server side and only when {@code fallDistance >=
 * MIN_FALL_DISTANCE}; this guards against (a) double-applied effects across the client/server
 * branch, and (b) "bounce in place" triggers from low-distance landings that would otherwise
 * make the player invincible (blood) or instantly-killable (magma) by sneaking on the block.
 */
public enum SlimeColor {

    BLUE("blue", MapColor.COLOR_LIGHT_BLUE), PURPLE("purple", MapColor.COLOR_PURPLE), MAGMA("magma", MapColor.COLOR_ORANGE) {
        @Override
        public void applyFallEffect(Level level, Entity entity, float fallDistance) {
            if (!level.isClientSide() && fallDistance >= MIN_FALL_DISTANCE) {
                entity.hurt(level.damageSources().hotFloor(), FIRE_DAMAGE_PER_BOUNCE);
            }
        }
    },
    BLOOD("blood", MapColor.COLOR_RED) {
        @Override
        public void applyFallEffect(Level level, Entity entity, float fallDistance) {
            if (!level.isClientSide() && entity instanceof LivingEntity living && fallDistance >= MIN_FALL_DISTANCE) {
                living.heal(HEAL_PER_BOUNCE);
            }
        }
    };

    /** Minimum {@code fallDistance} that arms the per-colour effect; suppresses tick-rate retriggers. */
    static final float MIN_FALL_DISTANCE = 1.0F;

    /** Damage applied by the magma variant on each bounce trigger; 1.0F = one half-heart per landing. */
    static final float FIRE_DAMAGE_PER_BOUNCE = 1.0F;

    /** Health restored by the blood variant on each bounce trigger; 1.0F = one half-heart per landing. */
    static final float HEAL_PER_BOUNCE = 1.0F;

    private final String id;
    // SuppressWarnings: PMD flags MapColor as non-serializable, but enum serialization writes
    // the constant name only — the runtime fields are never streamed.
    @SuppressWarnings("PMD.NonSerializableClass")
    private final MapColor mapColor;

    SlimeColor(String id, MapColor mapColor) {
        this.id = id;
        this.mapColor = mapColor;
    }

    /** Lower-snake-case colour segment used in the block registry path {@code slime_<id>_block}. */
    public String id() {
        return id;
    }

    /** Map colour shown on cartography tables and cargo terminals. */
    public MapColor mapColor() {
        return mapColor;
    }

    /**
     * Apply this colour's bounce-time side effect to the falling entity. Default is a no-op for
     * {@link #BLUE} / {@link #PURPLE}; {@link #MAGMA} and {@link #BLOOD} override.
     *
     * <p>Called from {@link TinkerSlimeBlock#fallOn(Level, net.minecraft.world.level.block.state.BlockState,
     * net.minecraft.core.BlockPos, Entity, float)} <em>before</em> the super-class bounce
     * mechanics so the side effect lands on the entity in the same tick the bounce kicks off.
     */
    public void applyFallEffect(Level level, Entity entity, float fallDistance) {
        // No-op for BLUE / PURPLE — vanilla slime behaviour is exactly the bounce mechanics in
        // SlimeBlock#fallOn, which {@link TinkerSlimeBlock} delegates to via super.
    }
}
