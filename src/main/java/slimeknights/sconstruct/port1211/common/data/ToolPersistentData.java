package slimeknights.sconstruct.port1211.common.data;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

/**
 * Per-modifier persistent state map keyed by modifier id. Fourth of the five tool data
 * components (SMTCON-21). Carries the bag-of-state each modifier owns on the ItemStack — for
 * example a "lifesteal absorbed health" buffer, a tick counter, or a stack of charges — without
 * forcing every modifier to declare its own DataComponentType.
 *
 * <p>The Map is unordered by intent (each entry is independent state for one modifier and
 * iteration order has no meaning), so the persistence codec is the simple
 * {@code Codec.unboundedMap} shape — there's no NBT round-trip order constraint to worry about
 * like there is on {@link ToolModifiers}. The compact constructor still defensively copies
 * through {@link Map#copyOf} so a caller that mutates its build buffer later can't corrupt the
 * snapshot stored on the stack.
 *
 * <p>Persistent via {@link #CODEC} (drives {@code DataComponentType.Builder#persistent}) and
 * network-synchronised via {@link #STREAM_CODEC}. {@link #empty()} returns a singleton — the
 * common "no modifier state" case never allocates.
 */
public record ToolPersistentData(Map<ResourceLocation, CompoundTag> data) {

    /** JSON / NBT round-trip codec. */
    public static final Codec<ToolPersistentData> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, CompoundTag.CODEC).xmap(ToolPersistentData::new, ToolPersistentData::data);

    /** Network codec. {@code COMPOUND_TAG} handles the per-value NBT serialisation on the wire. */
    public static final StreamCodec<ByteBuf, ToolPersistentData> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, ResourceLocation, CompoundTag, Map<ResourceLocation, CompoundTag>> map(LinkedHashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.COMPOUND_TAG)
            .map(ToolPersistentData::new, ToolPersistentData::data);

    private static final ToolPersistentData EMPTY = new ToolPersistentData(Map.of());

    /** Compact constructor — defensively copies so the record's data map cannot be aliased. */
    public ToolPersistentData {
        data = Map.copyOf(data);
    }

    /** Canonical empty {@code ToolPersistentData} — singleton so "no state" never allocates. */
    public static ToolPersistentData empty() {
        return EMPTY;
    }
}
