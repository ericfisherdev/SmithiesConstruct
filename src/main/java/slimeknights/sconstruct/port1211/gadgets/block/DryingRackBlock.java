package slimeknights.sconstruct.port1211.gadgets.block;

import javax.annotation.Nullable;

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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.port1211.gadgets.GadgetBlocks;
import slimeknights.sconstruct.port1211.gadgets.block.entity.DryingRackBlockEntity;

/**
 * The drying rack (SMTCON-137) — a {@link BaseEntityBlock} that holds a single item and dries
 * it into another over time (see {@link DryingRackBlockEntity}).
 *
 * <p>Right-click with an item in hand and the rack empty deposits one item; right-click with an
 * empty hand and the rack occupied withdraws the item back to the player. Both edits are
 * server-authoritative — the client path returns {@link InteractionResult#SUCCESS} to finish
 * the swing animation without issuing a duplicate edit. Breaking the rack drops its held item.
 *
 * <p>The {@link #DRYING_STATE} block-state property tracks whether the rack is empty, drying,
 * or done so the model can swap; the block entity keeps it in step with the rack's contents.
 */
public class DryingRackBlock extends BaseEntityBlock {

    public static final MapCodec<DryingRackBlock> CODEC = simpleCodec(DryingRackBlock::new);

    /** The rack's visible state — empty, drying, or done — driving the model swap. */
    public static final EnumProperty<DryingState> DRYING_STATE = EnumProperty.create("drying_state", DryingState.class);

    public DryingRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DRYING_STATE, DryingState.EMPTY));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DRYING_STATE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DryingRackBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Server-side only — drying progress advances on the server; the client renders the
        // synced block-state.
        return level.isClientSide() ? null : createTickerHelper(type, GadgetBlocks.DRYING_RACK_BE.get(), DryingRackBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            // Empty hand routes to useWithoutItem, which handles item withdrawal.
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemStackHandler inputHandler = rack.getInputHandler();
        if (!inputHandler.getStackInSlot(0).isEmpty()) {
            // Rack already occupied — let vanilla handle the click (e.g. block placement).
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        // Deposit a single item onto the rack; the player keeps the rest of the stack.
        inputHandler.setStackInSlot(0, stack.copyWithCount(1));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        ItemStackHandler inputHandler = rack.getInputHandler();
        ItemStack onRack = inputHandler.getStackInSlot(0);
        if (onRack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // Withdraw the rack's item back to the player; ItemHandlerHelper drops any overflow at
        // the player's feet if their inventory is full.
        ItemHandlerHelper.giveItemToPlayer(player, inputHandler.extractItem(0, onRack.getCount(), false));
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack) {
            Block.popResource(level, pos, rack.getInputHandler().getStackInSlot(0));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
