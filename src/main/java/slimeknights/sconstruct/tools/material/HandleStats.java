package slimeknights.sconstruct.tools.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Stats for a tool's rod / handle — pure multipliers applied to the head's baseline by
 * {@code StatsBuilder} (SMTCON-75). {@code 1.0f} multipliers leave the tool unchanged; legacy
 * Tinkers' Construct shipped values around 0.8..1.3 across the material roster.
 */
public record HandleStats(float durabilityModifier, float miningSpeedModifier, float attackSpeedModifier) implements MaterialStats {

    /** Map-codec form used by the {@link MaterialStatsCodecs} dispatch. */
    public static final MapCodec<HandleStats> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.FLOAT.fieldOf("durability_modifier").forGetter(HandleStats::durabilityModifier),
            Codec.FLOAT.fieldOf("mining_speed_modifier").forGetter(HandleStats::miningSpeedModifier), Codec.FLOAT.fieldOf("attack_speed_modifier").forGetter(HandleStats::attackSpeedModifier))
            .apply(instance, HandleStats::new));

    @Override
    public Type type() {
        return Type.HANDLE;
    }
}
