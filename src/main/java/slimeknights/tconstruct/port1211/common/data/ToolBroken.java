package slimeknights.tconstruct.port1211.common.data;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

/**
 * Single-boolean broken-state flag for a tool. Fifth of the five tool data components
 * (SMTCON-21). Kept as its own DataComponentType — separate from {@link ToolStats} or
 * {@link ToolMaterials} — so the renderer / tooltip layer can swap on the broken bit without
 * forcing every co-located stat to be re-serialised when the tool breaks. The flag flips far
 * more often than any other tool component (every durability-zero hit), so the network sync
 * payload stays tiny.
 *
 * <p>{@link #intact()} returns the canonical {@code ToolBroken(false)} singleton — the common
 * "tool is usable" case never allocates. The broken counterpart cannot be exposed as a
 * matching {@code broken()} factory because that name is already taken by the record
 * accessor; callers that need a broken instance allocate {@code new ToolBroken(true)} directly
 * or reuse the {@link #BROKEN} constant.
 */
public record ToolBroken(boolean broken) {

    /** JSON / NBT round-trip codec. */
    public static final Codec<ToolBroken> CODEC = Codec.BOOL.xmap(ToolBroken::new, ToolBroken::broken);

    /** Network codec. */
    public static final StreamCodec<ByteBuf, ToolBroken> STREAM_CODEC = ByteBufCodecs.BOOL.map(ToolBroken::new, ToolBroken::broken);

    /** Canonical broken-state singleton. Public so callers that need it skip the allocation. */
    public static final ToolBroken BROKEN = new ToolBroken(true);

    private static final ToolBroken INTACT = new ToolBroken(false);

    /** Canonical {@code ToolBroken(false)} — singleton for the not-broken state. */
    public static ToolBroken intact() {
        return INTACT;
    }
}
