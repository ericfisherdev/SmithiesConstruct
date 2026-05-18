package slimeknights.sconstruct.tools.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Stats for a bow limb — drives the bow's draw curve. {@code drawSpeed} ticks the projectile
 * power up linearly; {@code rangeMultiplier} scales the maximum drawn-velocity multiplier;
 * {@code damageBonus} adds flat damage on top of the arrow's base contribution.
 */
public record BowStats(int drawSpeed, float rangeMultiplier, float damageBonus) implements MaterialStats {

    /** Map-codec form used by the {@link MaterialStatsCodecs} dispatch. */
    public static final MapCodec<BowStats> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.INT.fieldOf("draw_speed").forGetter(BowStats::drawSpeed),
            Codec.FLOAT.fieldOf("range_multiplier").forGetter(BowStats::rangeMultiplier), Codec.FLOAT.fieldOf("damage_bonus").forGetter(BowStats::damageBonus)).apply(instance, BowStats::new));

    @Override
    public Type type() {
        return Type.BOW;
    }
}
