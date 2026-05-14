package slimeknights.sconstruct.port1211.tools.material;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

/**
 * A trait grant attached to a {@link Material} — an id (resolves to a registered trait at
 * apply-time) and an integer level. SMTCON-70 grows this into the full trait-record surface
 * (priority, conditional gating, tooltip metadata); for SMTCON-68 only the id+level pair is
 * needed so the {@link Material} record can declare its trait list.
 */
public record MaterialTrait(ResourceLocation id, int level) {

    /** JSON / NBT round-trip codec, datapack-readable. */
    public static final Codec<MaterialTrait> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(ResourceLocation.CODEC.fieldOf("id").forGetter(MaterialTrait::id), Codec.INT.optionalFieldOf("level", 1).forGetter(MaterialTrait::level)).apply(instance, MaterialTrait::new));

    /** Network codec; trait grants stream with their material rather than separately. */
    public static final StreamCodec<ByteBuf, MaterialTrait> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, MaterialTrait::id, ByteBufCodecs.VAR_INT, MaterialTrait::level,
            MaterialTrait::new);
}
