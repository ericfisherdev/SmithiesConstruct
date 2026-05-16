package slimeknights.sconstruct.port1211.smeltery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import slimeknights.sconstruct.port1211.smeltery.CastingBlocks;

/**
 * Block entity for the casting table (SMTCON-113). The table casts small items — its
 * {@link AbstractCastingBlockEntity} fluid tank holds 288 mB, exactly one ingot's worth of
 * molten metal, so a single pour fills it for an ingot-sized cast.
 */
public class CastingTableBlockEntity extends AbstractCastingBlockEntity {

    /** Tank capacity in mB — 288 mB is one ingot, the table's casting unit. */
    public static final int CAPACITY = 288;

    public CastingTableBlockEntity(BlockPos pos, BlockState state) {
        super(CastingBlocks.CASTING_TABLE_BE.get(), pos, state, CAPACITY);
    }

    @Override
    protected boolean isBasin() {
        return false;
    }
}
