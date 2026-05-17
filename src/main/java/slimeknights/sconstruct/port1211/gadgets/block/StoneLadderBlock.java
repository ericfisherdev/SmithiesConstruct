package slimeknights.sconstruct.port1211.gadgets.block;

import net.minecraft.world.level.block.LadderBlock;

import com.mojang.serialization.MapCodec;

/**
 * The stone ladder (SMTCON-139) — a stone-built climbable ladder. A thin {@link LadderBlock}
 * subclass: the climbing collision shape, facing, and waterlogging are all inherited; the stone
 * material, sound, and pickaxe tool requirement come from the block properties.
 *
 * <p>Like any ladder, climbing is granted by membership of the {@code minecraft:climbable}
 * block tag rather than by the block class — that tag entry is supplied by the gadget tag
 * data (SMTCON-143).
 */
public class StoneLadderBlock extends LadderBlock {

    /**
     * Block-state codec. {@link LadderBlock#codec()} narrows its return to
     * {@code MapCodec<LadderBlock>}, so the codec is typed as {@code MapCodec<LadderBlock>}
     * here too — the cast is safe because {@link #simpleCodec} only ever constructs
     * {@code StoneLadderBlock} instances, which are {@code LadderBlock}s.
     */
    @SuppressWarnings("unchecked")
    public static final MapCodec<LadderBlock> CODEC = (MapCodec<LadderBlock>) (MapCodec<?>) simpleCodec(StoneLadderBlock::new);

    public StoneLadderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<LadderBlock> codec() {
        return CODEC;
    }
}
