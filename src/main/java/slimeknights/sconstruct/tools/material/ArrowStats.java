package slimeknights.sconstruct.tools.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Stats for arrow components (fletching, shaft). {@code weight} affects flight characteristics
 * (gravity and range scaling) and {@code extraDurability} adds to the arrow's break-after-use
 * threshold the same way {@link ExtraStats} does for melee tools.
 */
public record ArrowStats(float weight, int extraDurability) implements MaterialStats {

    /** Map-codec form used by the {@link MaterialStatsCodecs} dispatch. */
    public static final MapCodec<ArrowStats> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.FLOAT.fieldOf("weight").forGetter(ArrowStats::weight), Codec.INT.fieldOf("extra_durability").forGetter(ArrowStats::extraDurability)).apply(instance, ArrowStats::new));

    @Override
    public Type type() {
        return Type.ARROW;
    }
}
