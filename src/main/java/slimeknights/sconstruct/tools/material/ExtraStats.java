package slimeknights.sconstruct.tools.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Stats for a tool binding — flat extra durability added on top of the head durability by
 * {@code StatsBuilder} (SMTCON-75). The binding has no other contribution beyond extra
 * durability; modifiers and traits are attached separately through the modifier system.
 */
public record ExtraStats(int extraDurability) implements MaterialStats {

    /** Map-codec form used by the {@link MaterialStatsCodecs} dispatch. */
    public static final MapCodec<ExtraStats> MAP_CODEC = RecordCodecBuilder
            .mapCodec(instance -> instance.group(Codec.INT.fieldOf("extra_durability").forGetter(ExtraStats::extraDurability)).apply(instance, ExtraStats::new));

    @Override
    public Type type() {
        return Type.EXTRA;
    }
}
