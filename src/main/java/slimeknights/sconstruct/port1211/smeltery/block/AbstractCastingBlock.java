package slimeknights.sconstruct.port1211.smeltery.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import slimeknights.sconstruct.port1211.smeltery.block.entity.AbstractCastingBlockEntity;

/**
 * Shared block for the two cast-handling smeltery blocks — the casting table and the casting
 * basin (SMTCON-113). Both behave identically at the block layer: a {@link BaseEntityBlock}
 * whose right-click manages the backing {@link AbstractCastingBlockEntity}'s single cast slot.
 *
 * <p>Right-click with an item in hand and the cast slot empty deposits one item into the slot
 * ({@link #useItemOn}); right-click with an empty hand and the slot occupied withdraws the cast
 * back to the player ({@link #useWithoutItem}). Both edits are server-authoritative — the
 * client path returns {@link InteractionResult#SUCCESS} to finish the swing animation without
 * issuing a duplicate edit. Breaking the block scatters the cast slot's contents via
 * {@link #onRemove}; the fluid tank's contents are intentionally not dropped (molten metal does
 * not survive the block being mined).
 *
 * <p>The fluid side — accepting a pour, matching it to a {@code CastingRecipe}, and completing
 * the cast — is owned by {@link AbstractCastingBlockEntity} and the casting recipe
 * implementation (SMTCON-123); this block only wires the cast-slot interaction.
 */
public abstract class AbstractCastingBlock extends BaseEntityBlock {

    protected AbstractCastingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            // Empty hand routes to useWithoutItem, which handles cast withdrawal.
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AbstractCastingBlockEntity casting)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemStackHandler castHandler = casting.getCastHandler();
        if (!castHandler.getStackInSlot(0).isEmpty()) {
            // Slot already occupied — let vanilla handle the click (e.g. block placement).
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        // Deposit a single item into the cast slot; the player keeps the rest of the stack.
        ItemStack single = stack.copyWithCount(1);
        castHandler.setStackInSlot(0, single);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AbstractCastingBlockEntity casting)) {
            return InteractionResult.PASS;
        }
        ItemStackHandler castHandler = casting.getCastHandler();
        ItemStack inSlot = castHandler.getStackInSlot(0);
        if (inSlot.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // Withdraw the cast back to the player; ItemHandlerHelper drops any overflow at the
        // player's feet if their inventory is full.
        ItemStack cast = castHandler.extractItem(0, inSlot.getCount(), false);
        ItemHandlerHelper.giveItemToPlayer(player, cast);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractCastingBlockEntity casting) {
                // The cast inventory is a fixed single slot, so scatter slot 0 directly rather
                // than wrapping the handler in a container to walk it.
                Block.popResource(level, pos, casting.getCastHandler().getStackInSlot(0));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
