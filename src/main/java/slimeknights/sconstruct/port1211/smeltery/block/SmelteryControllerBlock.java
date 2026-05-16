package slimeknights.sconstruct.port1211.smeltery.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryControllerBlockEntity;

/**
 * Smeltery controller block (SMTCON-112) — the brain block of the multiblock smeltery. It is a
 * full-cube, horizontally-directional {@link BaseEntityBlock} carrying a
 * {@link SmelteryControllerBlockEntity}. At this registration layer it has no behaviour beyond
 * being placeable and right-clickable; the smeltery state machine, structure validation, and the
 * controller GUI all arrive in later tickets (SMTCON-114 / SMTCON-125).
 *
 * <p>{@link #useWithoutItem} is wired now as the structural right-click hook so the interaction
 * surface is in place — but it deliberately opens nothing yet: the controller menu type and its
 * screen do not exist until SMTCON-125, and the BE will not implement
 * {@link net.minecraft.world.MenuProvider} until SMTCON-114. The hook returns the correct sided
 * results (a client-side {@code SUCCESS} to complete the swing, a server-side {@code CONSUME})
 * so that wiring the actual {@code player.openMenu(...)} call later is a one-line change with no
 * behavioural surprise. No placeholder menu type is invented here.
 */
public class SmelteryControllerBlock extends BaseEntityBlock {

    /** Save-format codec required by {@link BaseEntityBlock}; the properties-only form suffices. */
    public static final MapCodec<SmelteryControllerBlock> CODEC = simpleCodec(SmelteryControllerBlock::new);

    /**
     * Horizontal facing of the controller. Reused from vanilla (same property
     * {@code HorizontalDirectionalBlock.FACING} exposes) so structure / world-edit tooling
     * recognises it — declared directly because {@link BaseEntityBlock} does not extend
     * {@code HorizontalDirectionalBlock}.
     */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SmelteryControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Face the controller toward the player — opposite of where they look, the standard
        // furnace-style placement orientation.
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // MODEL — vanilla blockstate-driven rendering of the full-cube controller model.
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmelteryControllerBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Held-item path: defer to vanilla so block placement and other item interactions still
        // work. The empty-hand path below is the GUI hook.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            // Menu open is a server-side decision — return SUCCESS on the client to complete the
            // swing animation without firing a duplicate request.
            return InteractionResult.SUCCESS;
        }
        // SMTCON-114/125: once SmelteryControllerBlockEntity implements MenuProvider and the
        // controller MenuType + screen exist, this is where the server calls
        // player.openMenu(getMenuProvider(state, level, pos)). The structural hook is wired here
        // now; CONSUME is returned so the click is swallowed (no placeholder menu is opened).
        return InteractionResult.CONSUME;
    }
}
