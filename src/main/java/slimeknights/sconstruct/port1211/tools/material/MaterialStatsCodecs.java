package slimeknights.sconstruct.port1211.tools.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

/**
 * Codec holder for the {@link MaterialStats} sealed dispatch. Sits in a final class — not on
 * the {@link MaterialStats} interface — because PMD's {@code ConstantsInInterface} rule (per
 * SMTCON-68 CI feedback) discourages public-static-final fields on interfaces.
 *
 * <p>{@link #CODEC} dispatches on the {@code type} field every {@link MaterialStats}
 * implementor advertises through {@link MaterialStats#type()}. The dispatch is closed against
 * the five permits of {@link MaterialStats}; an unknown {@code type} value fails the parse
 * with a DataResult error rather than returning a default value.
 */
public final class MaterialStatsCodecs {

    /**
     * Polymorphic dispatch codec keyed on the {@code type} discriminator. Each permit's
     * {@code MAP_CODEC} carries its own field set; the {@code type} field is appended by the
     * dispatch and is the only key the encoder writes that comes from outside the implementer's
     * map codec.
     */
    public static final Codec<MaterialStats> CODEC = MaterialStats.Type.CODEC.dispatch("type", MaterialStats::type, MaterialStatsCodecs::codecFor);

    private MaterialStatsCodecs() {
    }

    /** Map-codec lookup keyed on the {@link MaterialStats.Type} discriminator. */
    private static MapCodec<? extends MaterialStats> codecFor(MaterialStats.Type type) {
        return switch (type) {
        case HEAD -> HeadStats.MAP_CODEC;
        case HANDLE -> HandleStats.MAP_CODEC;
        case EXTRA -> ExtraStats.MAP_CODEC;
        case BOW -> BowStats.MAP_CODEC;
        case ARROW -> ArrowStats.MAP_CODEC;
        };
    }
}
