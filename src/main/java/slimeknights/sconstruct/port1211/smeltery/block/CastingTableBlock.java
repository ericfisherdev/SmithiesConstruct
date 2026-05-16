package slimeknights.sconstruct.port1211.smeltery.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.port1211.smeltery.CastingBlocks;
import slimeknights.sconstruct.port1211.smeltery.block.entity.AbstractCastingBlockEntity;
import slimeknights.sconstruct.port1211.smeltery.block.entity.CastingTableBlockEntity;

/**
 * The casting table (SMTCON-113) — casts ingot-sized items from a 288 mB pour. A thin
 * {@link AbstractCastingBlock} subclass: it only supplies the {@link MapCodec} and the
 * table-specific {@link CastingTableBlockEntity}; all cast-slot interaction lives in the base.
 */
public class CastingTableBlock extends AbstractCastingBlock {

    public static final MapCodec<CastingTableBlock> CODEC = simpleCodec(CastingTableBlock::new);

    public CastingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CastingTableBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractCastingBlockEntity> beType() {
        return CastingBlocks.CASTING_TABLE_BE.get();
    }
}
