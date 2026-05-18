package slimeknights.sconstruct.tools.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.tools.block.entity.PatternChestBlockEntity;

/**
 * 32-slot Pattern Chest block (SMTCON-90). Extends {@link BaseEntityBlock} so the block
 * carries a {@link PatternChestBlockEntity} that persists its inventory across reloads.
 *
 * <p>Right-clicking with an empty hand server-side opens the chest's menu via
 * {@link #useWithoutItem}; the matching click-with-item path falls back to
 * {@link BaseEntityBlock}'s default to preserve vanilla "place block on top" placement
 * behaviour for stacked storage. Block destruction routes through {@link #onRemove}, which
 * drops the chest's contents into the world via {@link net.minecraft.world.Containers#dropContents}
 * reading the BE's exposed {@link IItemHandler}.
 *
 * <p>{@link #CODEC} is required by {@link BaseEntityBlock} for save-format reflection.
 * {@link #getRenderShape} returns {@link RenderShape#MODEL} so the block renders from its
 * blockstate JSON (no custom BER) — model + texture stubs are emitted by datagen; the
 * texture PNG is a follow-up per the ticket plan.
 */
public class PatternChestBlock extends BaseEntityBlock {

    /**
     * Save-format codec. {@link BaseEntityBlock} requires its concrete subclasses to expose
     * a {@link MapCodec}; the simple-properties form is sufficient because this block has no
     * extra state beyond {@link BlockBehaviour.Properties}.
     */
    public static final MapCodec<PatternChestBlock> CODEC = simpleCodec(PatternChestBlock::new);

    public PatternChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // MODEL — vanilla blockstate-driven rendering. INVISIBLE would suppress the block
        // model entirely (used by blocks whose BlockEntityRenderer draws everything); ENTITYBLOCK_ANIMATED
        // pairs the model with a BER. We just need the cube model with the pattern_chest texture.
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PatternChestBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        // The BE itself implements MenuProvider — return null defensively if the world
        // somehow lost the BE between place and interact (chunk unload race, /setblock without
        // BE, etc.) so the caller doesn't NPE inside Player#openMenu.
        return be instanceof PatternChestBlockEntity ? (PatternChestBlockEntity) be : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Held-item path: defer to vanilla so block placement (pattern chest on top of another)
        // and other item interactions still work. The empty-hand path below opens the menu.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            // Menu open is a server-side decision — the server pushes the screen open through
            // the NeoForge networking. Returning SUCCESS on the client side completes the swing
            // animation without firing a duplicate open request.
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        MenuProvider provider = getMenuProvider(state, level, pos);
        if (provider != null) {
            player.openMenu(provider);
        }
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PatternChestBlockEntity chest) {
                IItemHandler handler = chest.getHandler();
                // Translate the IItemHandler view into a vanilla SimpleContainer-shaped iterable
                // so Containers#dropContents can scatter the items at the broken position.
                net.minecraft.world.SimpleContainer dropContainer = new net.minecraft.world.SimpleContainer(handler.getSlots());
                for (int i = 0; i < handler.getSlots(); i++) {
                    dropContainer.setItem(i, handler.getStackInSlot(i));
                }
                net.minecraft.world.Containers.dropContents(level, pos, dropContainer);
                // Clear the chest contents so the block-entity-removed event doesn't double-drop
                // when other listeners walk the BE's inventory.
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
        return Properties.of().strength(2.5F).sound(net.minecraft.world.level.block.SoundType.WOOD).ignitedByLava();
    }
}
