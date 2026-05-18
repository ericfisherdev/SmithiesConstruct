package slimeknights.sconstruct.tools.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

/**
 * Sealed taxonomy of per-{@link slimeknights.sconstruct.tools.PartType part-type} stat
 * blocks a {@link Material} can ship. Five permits, one per part role:
 *
 * <ul>
 *   <li>{@link HeadStats} — tool heads (pickaxe head, axe head, sword blade, …); durability +
 *       harvest level + mining speed + attack damage.</li>
 *   <li>{@link HandleStats} — modifier-style multipliers contributed by a tool's rod.</li>
 *   <li>{@link ExtraStats} — flat extra durability contributed by a binding.</li>
 *   <li>{@link BowStats} — bow-limb draw speed + range + damage bonus.</li>
 *   <li>{@link ArrowStats} — fletching/shaft weight + extra durability for ranged ammo.</li>
 * </ul>
 *
 * <p>Each permit ships its own {@link MapCodec} keyed against {@link Type}. The polymorphic
 * dispatch codec lives in {@link MaterialStatsCodecs} (PMD's {@code ConstantsInInterface} rule
 * discourages constants on sealed interfaces); production callers reference
 * {@link MaterialStatsCodecs#CODEC}, which dispatches against the {@code type} field every stat
 * record advertises through {@link #type()}.
 */
public sealed interface MaterialStats permits HeadStats, HandleStats, ExtraStats, BowStats, ArrowStats {

    /** Stable discriminator used by the dispatch codec's {@code type} field. */
    Type type();

    /**
     * Closed enumeration of stat record types. The serialised form is the lowercase enum name
     * (see {@link #id()}); the {@link MaterialStatsCodecs} dispatch maps these strings to the
     * matching {@link MapCodec}. Adding a new value is non-breaking; reordering changes
     * {@link Enum#ordinal()} (forbidden by contract — persistence goes through the string
     * form).
     */
    enum Type {

        HEAD("head"), HANDLE("handle"), EXTRA("extra"), BOW("bow"), ARROW("arrow");

        /**
         * Codec routing the dispatch field through {@link #id()}. Decode goes via
         * {@code flatXmap} so an unknown id surfaces as a {@code DataResult.error} rather than
         * throwing — Mojang's dispatch infrastructure relies on the error path to fail
         * gracefully when a datapack ships a stat type that the mod does not know.
         */
        public static final Codec<Type> CODEC = Codec.STRING.flatXmap(id -> {
            for (Type type : values()) {
                if (type.id().equals(id)) {
                    return com.mojang.serialization.DataResult.success(type);
                }
            }
            return com.mojang.serialization.DataResult.error(() -> "Unknown MaterialStats.Type id: " + id);
        }, type -> com.mojang.serialization.DataResult.success(type.id()));

        private final String id;

        Type(String id) {
            this.id = id;
        }

        /** Explicit lowercase identifier used in JSON — decoupled from {@link #name()} so a
         *  future constant rename does not break shipped datapacks. */
        public String id() {
            return id;
        }

        /** Lookup helper used by the dispatch codec; throws on unknown ids. */
        public static Type byId(String id) {
            for (Type type : values()) {
                if (type.id().equals(id)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown MaterialStats.Type id: " + id);
        }
    }

}
