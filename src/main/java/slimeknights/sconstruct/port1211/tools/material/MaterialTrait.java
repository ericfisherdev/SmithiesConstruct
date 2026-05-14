package slimeknights.sconstruct.port1211.tools.material;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import slimeknights.sconstruct.port1211.tools.PartType;

import io.netty.buffer.ByteBuf;

/**
 * Binds a trait id to a specific {@link PartType} slot on a {@link Material}. When a tool is
 * built and the matching part slot is filled with a material that ships this trait, the trait
 * is auto-applied to the resulting tool.
 *
 * <p>Slot-scoped (not material-wide) so a single material can grant a different trait depending
 * on which part it occupies — e.g. wood grants {@code ecological} on a head but
 * {@code splintering} on a binding. The legacy 1.12 mod modelled this with per-material-per-part
 * trait lists; the record collapses that to a single bound entry the {@link Material#traits}
 * list iterates over.
 */
public record MaterialTrait(ResourceLocation traitId, PartType slot) {

    /** Datapack-readable JSON / NBT round-trip codec. */
    public static final Codec<MaterialTrait> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(ResourceLocation.CODEC.fieldOf("trait").forGetter(MaterialTrait::traitId), PartType.CODEC.fieldOf("slot").forGetter(MaterialTrait::slot)).apply(instance, MaterialTrait::new));

    /** Network codec; trait grants stream with their material rather than separately. */
    public static final StreamCodec<ByteBuf, MaterialTrait> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, MaterialTrait::traitId, PartType.STREAM_CODEC, MaterialTrait::slot,
            MaterialTrait::new);
}
