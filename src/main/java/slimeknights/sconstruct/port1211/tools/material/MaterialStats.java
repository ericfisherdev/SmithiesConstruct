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
 * subtypes. The {@link #CODEC} placeholder rejects every value with a stable error message; the
 * actual dispatch arrives in SMTCON-69 and replaces this stub without touching {@link Material}.
 */
public sealed interface MaterialStats permits MaterialStats.Placeholder {

    /**
     * Placeholder dispatch codec — fails until SMTCON-69 lands the real per-part-type subtypes.
     * Returning a failure codec rather than throwing at class-load keeps the {@link Material}
     * record loadable for unit tests that never carry stats values.
     */
    Codec<MaterialStats> CODEC = Codec.unit(Placeholder.INSTANCE).flatXmap(unit -> com.mojang.serialization.DataResult.success((MaterialStats) unit),
            value -> com.mojang.serialization.DataResult.success(Placeholder.INSTANCE));

    /** Network codec; matches the JSON codec's placeholder semantics. */
    StreamCodec<ByteBuf, MaterialStats> STREAM_CODEC = ByteBufCodecs.BOOL.map(b -> Placeholder.INSTANCE, m -> Boolean.TRUE);

    /**
     * Singleton no-op stats value used while {@link MaterialStats} is a placeholder. Once
     * SMTCON-69 lands the real per-part-type records this permit goes away — any callers
     * relying on it must migrate to the real subtype that matches their part.
     */
    final class Placeholder implements MaterialStats {

        /** Singleton instance; the placeholder carries no state. */
        public static final Placeholder INSTANCE = new Placeholder();

        private Placeholder() {
        }
    }
}
