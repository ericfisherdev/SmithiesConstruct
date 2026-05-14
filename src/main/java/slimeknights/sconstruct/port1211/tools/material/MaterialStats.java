package slimeknights.sconstruct.port1211.tools.material;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

/**
 * Sealed taxonomy of per-{@link slimeknights.sconstruct.port1211.tools.PartType part-type} stat
 * blocks a {@link Material} can ship — durability/attack for heads, modifier-bonus for handles,
 * draw-speed for limbs, and so on. SMTCON-68 needs the type to exist (the {@link Material}
 * record holds a {@code Map<PartType, MaterialStats>}); SMTCON-69 ports the concrete subtypes
 * and replaces the placeholder permits list with the real one.
 *
 * <p>Sealed rather than open so the codec can register a closed dispatch over the known
 * subtypes. The dispatch codec lives on {@link Placeholder} (PMD discourages constants on
 * interfaces); SMTCON-69 will hoist a real dispatch codec onto the interface via a wrapper
 * holder class.
 */
public sealed interface MaterialStats permits MaterialStats.Placeholder {

    /**
     * Singleton no-op stats value used while {@link MaterialStats} is a placeholder. Once
     * SMTCON-69 lands the real per-part-type records this permit goes away — any callers
     * relying on it must migrate to the real subtype that matches their part.
     */
    final class Placeholder implements MaterialStats {

        /** Singleton instance; the placeholder carries no state. */
        public static final Placeholder INSTANCE = new Placeholder();

        /**
         * Placeholder dispatch codec — fails until SMTCON-69 lands the real per-part-type
         * subtypes. Returns the singleton on every decode so the {@link Material} codec stays
         * loadable for unit tests that never carry stats values.
         */
        public static final Codec<MaterialStats> CODEC = Codec.unit(INSTANCE);

        /** Network codec; matches the JSON codec's placeholder semantics. */
        public static final StreamCodec<ByteBuf, MaterialStats> STREAM_CODEC = ByteBufCodecs.BOOL.map(b -> INSTANCE, m -> Boolean.TRUE);

        private Placeholder() {
        }
    }
}
