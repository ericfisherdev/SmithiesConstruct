package slimeknights.sconstruct.port1211.tools.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.port1211.tools.block.entity.ToolForgeBlockEntity;

/**
 * Tool Forge block (SMTCON-93). Extension of {@link ToolStationBlock} that swaps in the
 * {@link ToolForgeBlockEntity} subclass — opens the same 6-slot menu but lets the BE accept the
 * advanced {@link slimeknights.sconstruct.port1211.tools.ToolDefinition#ALL_ADVANCED} roster
 * (hammer / lumberaxe / shortbow / crossbow / arrow on top of the basic harvest / melee tools).
 *
 * <p>The block-level shape (drop-on-break, menu provider, useWithoutItem) is identical to the
 * Tool Station's, so this class only overrides {@link #newBlockEntity} and the
 * {@link BaseEntityBlock#codec} hook. Everything else is inherited.
 */
public class ToolForgeBlock extends ToolStationBlock {

    /** Save-format codec required by {@link net.minecraft.world.level.block.BaseEntityBlock}. */
    public static final MapCodec<ToolForgeBlock> CODEC = simpleCodec(ToolForgeBlock::new);

    public ToolForgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ToolStationBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ToolForgeBlockEntity(pos, state);
    }
}
