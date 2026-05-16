package slimeknights.sconstruct.port1211.smeltery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import slimeknights.sconstruct.port1211.smeltery.CastingBlocks;

/**
 * Block entity for the casting basin (SMTCON-113). The basin casts blocks and large items — its
 * {@link AbstractCastingBlockEntity} fluid tank holds 2592 mB, one block's worth of molten
 * metal (nine ingots), so it can cast a full storage block in a single fill.
 */
public class CastingBasinBlockEntity extends AbstractCastingBlockEntity {

    /** Tank capacity in mB — 2592 mB is one block (nine ingots), the basin's casting unit. */
    public static final int CAPACITY = 2592;

    public CastingBasinBlockEntity(BlockPos pos, BlockState state) {
        super(CastingBlocks.CASTING_BASIN_BE.get(), pos, state, CAPACITY);
    }
}
