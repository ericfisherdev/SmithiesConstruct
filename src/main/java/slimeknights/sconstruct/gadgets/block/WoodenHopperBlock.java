package slimeknights.sconstruct.gadgets.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.gadgets.GadgetBlocks;
import slimeknights.sconstruct.gadgets.block.entity.WoodenHopperBlockEntity;

/**
 * The wooden hopper (SMTCON-138) — a pre-iron-tier hopper. A thin {@link HopperBlock} subclass:
 * the funnel shape, facing, redstone-disable behaviour, and container-drop-on-break are all
 * inherited unchanged; only the block entity differs, swapping in the slower-transferring
 * {@link WoodenHopperBlockEntity}.
 */
public class WoodenHopperBlock extends HopperBlock {

    /**
     * Block-state codec. {@link HopperBlock#codec()} narrows its return to
     * {@code MapCodec<HopperBlock>}, so the codec is typed as {@code MapCodec<HopperBlock>}
     * here too — the cast is safe because {@link #simpleCodec} only ever constructs
     * {@code WoodenHopperBlock} instances, which are {@code HopperBlock}s.
     */
    @SuppressWarnings("unchecked")
    public static final MapCodec<HopperBlock> CODEC = (MapCodec<HopperBlock>) (MapCodec<?>) simpleCodec(WoodenHopperBlock::new);

    public WoodenHopperBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<HopperBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodenHopperBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // Server-side only — reuse vanilla's pushItemsTick; the wooden hopper's slowdown lives
        // entirely in WoodenHopperBlockEntity#setCooldown, not in the tick loop itself.
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, GadgetBlocks.WOODEN_HOPPER_BE.get(), HopperBlockEntity::pushItemsTick);
    }
}
