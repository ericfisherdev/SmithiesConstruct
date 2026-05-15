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

import slimeknights.sconstruct.port1211.tools.block.entity.PartBuilderBlockEntity;

/**
 * Part Builder block (SMTCON-92). Three-slot crafting station — pattern, material, output —
 * that resolves a typed pattern + an item-tag-matched material into a fully-stamped tool-part
 * {@link slimeknights.sconstruct.port1211.tools.item.MaterialItem} stack carrying the
 * material identity on its {@code PART_MATERIAL} component.
 *
 * <p>Right-clicking with an empty hand server-side opens the menu via {@link #useWithoutItem};
 * held-item interactions fall back to vanilla so placement and other held-item behaviours keep
 * working. Block destruction routes through {@link #onRemove}, which scatters the three slots
 * via {@link Containers#dropContents}.
 *
 * <p>Same shape as {@link StencilTableBlock} — only the BE type and the 3-slot drop sweep
 * differ.
 */
public class PartBuilderBlock extends BaseEntityBlock {

    /**
     * Save-format codec required by {@link BaseEntityBlock}. The block has no extra state
     * beyond its {@link net.minecraft.world.level.block.state.BlockBehaviour.Properties}, so
     * the simple-properties form is sufficient.
     */
    public static final MapCodec<PartBuilderBlock> CODEC = simpleCodec(PartBuilderBlock::new);

    public PartBuilderBlock(Properties properties) {
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
        return new PartBuilderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        // Defensive null on BE loss (chunk unload race, /setblock without BE, etc.) — caller's
        // Player#openMenu would NPE otherwise.
        return be instanceof PartBuilderBlockEntity ? (PartBuilderBlockEntity) be : null;
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
            if (be instanceof PartBuilderBlockEntity builder) {
                IItemHandler handler = builder.getHandler();
                SimpleContainer dropContainer = new SimpleContainer(handler.getSlots());
                for (int i = 0; i < handler.getSlots(); i++) {
                    dropContainer.setItem(i, handler.getStackInSlot(i));
                }
                Containers.dropContents(level, pos, dropContainer);
                // Clear the table contents so any block-entity-removed listeners don't double-drop.
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
