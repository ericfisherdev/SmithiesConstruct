package slimeknights.sconstruct.smeltery.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.smeltery.CastingBlocks;
import slimeknights.sconstruct.smeltery.block.entity.AbstractCastingBlockEntity;
import slimeknights.sconstruct.smeltery.block.entity.CastingBasinBlockEntity;

/**
 * The casting basin (SMTCON-113) — casts block-sized items from a 2592 mB pour. A thin
 * {@link AbstractCastingBlock} subclass: it only supplies the {@link MapCodec} and the
 * basin-specific {@link CastingBasinBlockEntity}; all cast-slot interaction lives in the base.
 */
public class CastingBasinBlock extends AbstractCastingBlock {

    public static final MapCodec<CastingBasinBlock> CODEC = simpleCodec(CastingBasinBlock::new);

    public CastingBasinBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CastingBasinBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractCastingBlockEntity> beType() {
        return CastingBlocks.CASTING_BASIN_BE.get();
    }
}
