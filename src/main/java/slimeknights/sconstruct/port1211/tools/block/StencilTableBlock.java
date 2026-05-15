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

import slimeknights.sconstruct.port1211.tools.block.entity.StencilTableBlockEntity;

/**
 * Stencil Table block (SMTCON-91). Single-input / single-output crafting station that converts
 * a blank pattern into a typed pattern stamped with a cycling {@link
 * slimeknights.sconstruct.port1211.tools.PartType} cursor. Extends {@link BaseEntityBlock} so
 * the block carries a {@link StencilTableBlockEntity} for the inventory + cursor state.
 *
 * <p>Right-clicking with an empty hand server-side opens the menu via {@link #useWithoutItem}.
 * Held-item interactions fall back to vanilla so block placement (stencil table on top of
 * another) keeps working. Block destruction routes through {@link #onRemove}, which scatters
 * the input + output slots into the world via {@link Containers#dropContents}.
 *
 * <p>{@link #CODEC} is required by {@link BaseEntityBlock} for save-format reflection.
 * {@link #getRenderShape} returns {@link RenderShape#MODEL} so the block renders from its
 * blockstate JSON (no BER). The model + texture stubs are emitted by datagen; the texture
 * PNG is a follow-up per the ticket plan.
 *
 * <p>Same shape as
 * {@link slimeknights.sconstruct.port1211.tools.block.PatternChestBlock}; the only deltas are
 * the BE type and the 2-slot drop sweep (vs the chest's 32-slot sweep).
 */
public class StencilTableBlock extends BaseEntityBlock {

    /**
     * Save-format codec. {@link BaseEntityBlock} requires its concrete subclasses to expose a
     * {@link MapCodec}; the simple-properties form is sufficient because this block has no
     * extra state beyond {@link net.minecraft.world.level.block.state.BlockBehaviour.Properties}.
     */
    public static final MapCodec<StencilTableBlock> CODEC = simpleCodec(StencilTableBlock::new);

    public StencilTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // MODEL — vanilla blockstate-driven rendering, same as the pattern chest.
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StencilTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        // Defensive null on BE loss (chunk unload race, /setblock without BE, etc.) — caller's
        // Player#openMenu would NPE otherwise.
        return be instanceof StencilTableBlockEntity ? (StencilTableBlockEntity) be : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Held-item path: defer to vanilla. Empty-hand path below opens the menu.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            // Menu open is a server-side decision — server-side openMenu pushes the screen via
            // NeoForge networking. SUCCESS on the client completes the swing animation without
            // firing a duplicate open request.
            return InteractionResult.SUCCESS;
        }
        MenuProvider provider = getMenuProvider(state, level, pos);
        if (provider != null) {
            // Mirror PatternChestBlock — the menu's server constructor goes through the BE's
            // createMenu, and the client's IMenuTypeExtension.create factory falls back to the
            // empty-buf stub path. The packet-driven cycle button still resolves the BE via the
            // pos field on the network payload, not via the open-menu extra data.
            player.openMenu(provider);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StencilTableBlockEntity table) {
                IItemHandler handler = table.getHandler();
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
