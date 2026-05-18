package slimeknights.sconstruct.smeltery.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

import slimeknights.sconstruct.smeltery.block.entity.SearedTankBE;

/**
 * Abstract base for the two seared tank blocks (SMTCON-118) — the standalone fluid containers
 * of the smeltery. Splits the tank-specific behaviour off {@link SmelteryComponentBlock}: a tank
 * carries a {@link SearedTankBE} (which owns a real {@link net.neoforged.neoforge.fluids.capability.templates.FluidTank})
 * rather than the bare controller-proxy block entity, accepts bucket interactions, and keeps its
 * contents when broken.
 *
 * <p><strong>Bucket interaction.</strong> {@link #useItemOn} routes a held bucket through
 * {@link FluidUtil#interactWithFluidHandler}, which fills the tank from a full bucket or empties
 * the tank into an empty one against the tank's {@code FluidHandler.BLOCK} capability.
 *
 * <p><strong>Contents on break.</strong> {@link #onRemove} drops a tank item carrying the block
 * entity's data when the tank holds fluid, so the <em>exact</em> stored amount survives mining
 * and is restored when the tank is placed again — molten metal is stored in 144&nbsp;mB ingot
 * units, so a bucket-quantised drop would routinely lose a remainder. An empty tank drops
 * nothing here; its plain-block drop is the loot table's job (SMTCON-129), which will own the
 * full break-drop once it lands.
 */
public abstract class SearedTankBlock extends SmelteryComponentBlock {

    protected SearedTankBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SearedTankBE(beType(), pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // interactWithFluidHandler handles both directions — full bucket fills the tank, empty
        // bucket drains it — against the tank's FluidHandler.BLOCK capability.
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Guard on an actual block change (not a state-only update) so a blockstate flip does
        // not drop the tank.
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SearedTankBE tank) {
            dropFilledTank(level, pos, state, tank);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Drops a tank item carrying the block entity's saved data when the tank holds fluid, so the
     * exact contents survive the break and are restored on placement. A {@code BLOCK_ENTITY_DATA}
     * component tagged with the block-entity id is what vanilla's {@code BlockItem} placement
     * reads back into the freshly placed {@link SearedTankBE}.
     */
    private static void dropFilledTank(Level level, BlockPos pos, BlockState state, SearedTankBE tank) {
        if (tank.getFluidHandler().getFluidInTank(0).isEmpty()) {
            return;
        }
        CompoundTag blockEntityData = tank.saveWithId(level.registryAccess());
        ItemStack drop = new ItemStack(state.getBlock());
        drop.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityData));
        Block.popResource(level, pos, drop);
    }
}
