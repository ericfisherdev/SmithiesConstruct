package slimeknights.sconstruct.common.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

/**
 * Insertion-ordered map of modifier identifier → level. Second of the five tool data components
 * (SMTCON-19) carrying the data the legacy 1.12 port used to keep on per-tool capabilities and
 * raw NBT lists; now lives as a typed component on the ItemStack's {@code DataComponentMap}.
 *
 * <p>Order matters: tooltips render the modifier list in the same order they were applied, and
 * any modifier system that does "first-match wins" / "priority is application order" depends on
 * iteration order being stable. The compact constructor wraps the incoming map with
 * {@code Collections.unmodifiableMap(new LinkedHashMap<>(input))} so the record is both
 * immutable and order-preserving regardless of what shape the caller passed in.
 *
 * <p>Persistent via {@link #CODEC} (drives the {@code DataComponentType.Builder#persistent}
 * codec) and network-synchronised via {@link #STREAM_CODEC}. {@link #with(ResourceLocation, int)}
 * returns a structurally-new {@code ToolModifiers} so the record stays usable as a
 * persistent-data-structure-style immutable value: an existing key keeps its insertion slot, a
 * new key appends to the end, and the original instance is never mutated.
 */
public record ToolModifiers(Map<ResourceLocation, Integer> levels) {

    /**
     * Persistence codec. Used by {@code DataComponentType.Builder#persistent}, which serialises
     * via {@code NbtOps} for ItemStack disk storage.
     *
     * <p>Encoded as a list of {@code {id, level}} records rather than as a free-form
     * {@code Codec.unboundedMap} because Minecraft's {@code CompoundTag} is contractually
     * unordered (HashMap-backed in vanilla); a map-shaped codec drops insertion order through
     * the NBT round-trip even though the JSON round-trip would preserve it. A list shape is
     * ordered on every {@code DynamicOps} including {@code NbtOps}, so this round-trips through
     * the save format without losing the order semantics this component exists to guarantee.
     */
    public static final Codec<ToolModifiers> CODEC = Entry.CODEC.listOf().xmap(ToolModifiers::fromEntries, ToolModifiers::toEntries);

    /**
     * Network codec. {@link ByteBufCodecs#map} writes entries in iteration order and reconstructs
     * them into the supplied {@link LinkedHashMap} factory — guaranteeing the receiving side sees
     * the same insertion order as the sender.
     */
    public static final StreamCodec<ByteBuf, ToolModifiers> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, ResourceLocation, Integer, Map<ResourceLocation, Integer>> map(LinkedHashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT)
            .map(ToolModifiers::new, ToolModifiers::levels);

    private static final ToolModifiers EMPTY = new ToolModifiers(Map.of());

    /**
     * Compact constructor — defensively copies the input through a {@link LinkedHashMap} so the
     * record stays both immutable (the returned view rejects mutation) and order-preserving (the
     * iteration order is fixed at construction time, independent of the input map's
     * implementation type).
     */
    public ToolModifiers {
        levels = Collections.unmodifiableMap(new LinkedHashMap<>(levels));
    }

    /**
     * Canonical empty {@code ToolModifiers} — a tool with no modifiers applied. Returns the same
     * instance every call so "no modifiers" never allocates.
     */
    public static ToolModifiers empty() {
        return EMPTY;
    }

    /**
     * Return a new {@code ToolModifiers} with the {@code id → level} entry added (or its level
     * updated if {@code id} is already present). The original {@code this} instance is never
     * mutated. Existing keys retain their original insertion slot; new keys append to the end of
     * the iteration order.
     */
    public ToolModifiers with(ResourceLocation id, int level) {
        // Declared as Map (not LinkedHashMap) per PMD's LooseCoupling — the LinkedHashMap
        // runtime type still gives us insertion-order semantics on put().
        Map<ResourceLocation, Integer> copy = new LinkedHashMap<>(levels);
        copy.put(id, level);
        return new ToolModifiers(copy);
    }

    private static ToolModifiers fromEntries(List<Entry> entries) {
        Map<ResourceLocation, Integer> assembled = new LinkedHashMap<>();
        for (Entry entry : entries) {
            assembled.put(entry.id(), entry.level());
        }
        return new ToolModifiers(assembled);
    }

    private static List<Entry> toEntries(ToolModifiers modifiers) {
        return modifiers.levels().entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Wire shape for a single modifier entry inside {@link #CODEC}. Encoded as a JSON/NBT
     * object {@code {"id": "<resource-location>", "level": N}}; the surrounding list ordering
     * is the property we depend on for stable per-entry placement.
     */
    private record Entry(ResourceLocation id, int level) {
        static final Codec<Entry> CODEC = RecordCodecBuilder
                .create(instance -> instance.group(ResourceLocation.CODEC.fieldOf("id").forGetter(Entry::id), Codec.INT.fieldOf("level").forGetter(Entry::level)).apply(instance, Entry::new));
    }
}
