package slimeknights.sconstruct.common.data;

import java.util.List;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

/**
 * Ordered list of material identifiers — one entry per part slot — that defines what a built
 * tool is made of. First of the five tool data components ported in Phase 1 (SMTCON-18 …
 * SMTCON-22) replacing the legacy 1.12 "Material" capability + ItemStack NBT plumbing with
 * vanilla 1.20.5+ {@code DataComponentType}-typed storage.
 *
 * <p>Persistent (saved to disk via {@link #CODEC}) and network-synchronised (streamed via
 * {@link #STREAM_CODEC}) so the same materials list survives a save-load cycle, a server
 * restart, and a client/server boundary.
 *
 * <p>The record's canonical constructor wraps the incoming list with {@link List#copyOf} so the
 * record cannot be aliased to a caller-mutable list — a downstream pulse that builds the
 * materials list in a local {@code ArrayList} and passes it through can keep mutating that list
 * without leaking the changes into the component snapshot.
 */
public record ToolMaterials(List<ResourceLocation> parts) {

    /** JSON / NBT round-trip codec. Used by {@code DataComponentType.Builder#persistent}. */
    public static final Codec<ToolMaterials> CODEC = ResourceLocation.CODEC.listOf().xmap(ToolMaterials::new, ToolMaterials::parts);

    /**
     * Network codec. Used by {@code DataComponentType.Builder#networkSynchronized}. Typed
     * against the raw {@link ByteBuf} supertype (rather than {@code RegistryFriendlyByteBuf})
     * because {@link ResourceLocation#STREAM_CODEC} does not need registry access — keeps the
     * type assignable to the broader {@code ? super RegistryFriendlyByteBuf} contract on the
     * builder while still working on any narrower buf.
     */
    public static final StreamCodec<ByteBuf, ToolMaterials> STREAM_CODEC = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).map(ToolMaterials::new, ToolMaterials::parts);

    private static final ToolMaterials EMPTY = new ToolMaterials(List.of());

    /** Compact constructor: defensively copy so the record is truly immutable. */
    public ToolMaterials {
        parts = List.copyOf(parts);
    }

    /**
     * Canonical empty {@code ToolMaterials} — a tool with no material slots filled. Returns
     * the same instance every call (the underlying empty list is shared and immutable) so
     * code that asks for "no materials" never allocates.
     */
    public static ToolMaterials empty() {
        return EMPTY;
    }
}
