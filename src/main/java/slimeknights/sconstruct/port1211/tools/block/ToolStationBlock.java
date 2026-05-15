package slimeknights.sconstruct.port1211.tools.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.port1211.tools.block.entity.ToolStationBlockEntity;

/**
 * Tool Station block (SMTCON-93). Six-input + one-output crafting station that assembles a
 * built {@link slimeknights.sconstruct.port1211.tools.item.ToolCore} stack from positional
 * {@link slimeknights.sconstruct.port1211.tools.item.MaterialItem} parts. Same block / BE / menu
 * shape as {@link PartBuilderBlock} — the only differences are the slot count and the
 * {@link ToolStationBlockEntity} that drives the build logic.
 *
 * <p>Right-clicking with an empty hand server-side opens the menu via {@link #useWithoutItem};
 * held-item interactions fall back to vanilla so placement and other held-item behaviours keep
 * working. Block destruction routes through {@link #onRemove}, which scatters every populated
 * input slot via {@link Containers#dropContents}.
 *
 * <p>{@link ToolForgeBlock} extends this block; the Tool Forge differs only in the BE type
 * (which widens the candidate roster from {@link slimeknights.sconstruct.port1211.tools.ToolDefinition#ALL_BASIC}
 * to {@link slimeknights.sconstruct.port1211.tools.ToolDefinition#ALL_ADVANCED}).
 */
public class ToolStationBlock extends BaseEntityBlock {

    /** Save-format codec required by {@link BaseEntityBlock}. */
    public static final MapCodec<ToolStationBlock> CODEC = simpleCodec(ToolStationBlock::new);

    public ToolStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ToolStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        // Defensive null on BE loss (chunk unload race, /setblock without BE, etc.) so the
        // caller's Player#openMenu wouldn't NPE.
        return be instanceof ToolStationBlockEntity ? (ToolStationBlockEntity) be : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Held-item path: defer to vanilla. Empty-hand path below opens the menu.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            // Menu open is a server-side decision — SUCCESS completes the swing animation
            // without firing a duplicate open request from the client side.
            return InteractionResult.SUCCESS;
        }
        MenuProvider provider = getMenuProvider(state, level, pos);
        if (provider != null) {
            player.openMenu(provider);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ToolStationBlockEntity station) {
                IItemHandler handler = station.getHandler();
                // Drop only the input slots — the output slot holds a synthetic preview built
                // from those inputs by refreshOutput(); dropping it would duplicate the parts
                // the player just put in (build path) or the tool itself (modify path).
                SimpleContainer dropContainer = new SimpleContainer(ToolStationBlockEntity.INPUT_SLOTS);
                for (int i = 0; i < ToolStationBlockEntity.INPUT_SLOTS; i++) {
                    dropContainer.setItem(i, handler.getStackInSlot(i));
                }
                Containers.dropContents(level, pos, dropContainer);
                if (handler instanceof ItemStackHandler stackHandler) {
                    for (int i = 0; i < stackHandler.getSlots(); i++) {
                        stackHandler.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Test seam: convenience builder for the props shape used by the registry. */
    public static Properties defaultProperties() {
        return Properties.of().strength(2.5F).sound(SoundType.WOOD).ignitedByLava();
    }
}
