package slimeknights.sconstruct.port1211.gadgets.block.entity;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import slimeknights.sconstruct.port1211.gadgets.GadgetBlocks;
import slimeknights.sconstruct.port1211.gadgets.block.DryingRackBlock;
import slimeknights.sconstruct.port1211.gadgets.block.DryingState;
import slimeknights.sconstruct.port1211.gadgets.recipe.DryingRecipe;
import slimeknights.sconstruct.port1211.gadgets.recipe.GadgetRecipes;

/**
 * Block entity for the drying rack (SMTCON-137). Holds a single item; each server tick, if that
 * item matches a {@link DryingRecipe}, the drying counter advances and — once it reaches the
 * recipe's {@code dryTime} — the slot's contents are replaced with the recipe output.
 *
 * <p>The rack's block state carries a {@link DryingState} so the model can swap between empty,
 * drying, and done; this block entity keeps that property in step with the slot contents and
 * the drying progress.
 */
public class DryingRackBlockEntity extends BlockEntity {

    private static final String TAG_ITEM = "Item";
    private static final String TAG_PROGRESS = "DryProgress";

    /** The rack's single item slot — the item resting on the rack. */
    private final ItemStackHandler inputHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            // A changed slot resets the counter (a new item starts drying from zero) and
            // refreshes the block-state so the model tracks the contents.
            dryProgress = 0;
            setChanged();
            refreshDryingState();
        }
    };

    /** Server ticks the current item has spent drying; {@code 0} when nothing is drying. */
    private int dryProgress;

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(GadgetBlocks.DRYING_RACK_BE.get(), pos, state);
    }

    /** The rack's single-slot inventory — used by the block's right-click add / remove handling. */
    public ItemStackHandler getInputHandler() {
        return inputHandler;
    }

    /** Server ticks the current item has spent drying. */
    public int getDryProgress() {
        return dryProgress;
    }

    /**
     * Server-side {@code BlockEntityTicker} entry point, registered by {@link DryingRackBlock}.
     * Advances the drying counter for the held item and, on completion, swaps the slot for the
     * recipe output.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        rack.tickDrying();
    }

    private void tickDrying() {
        if (level == null || level.isClientSide()) {
            return;
        }
        ItemStack item = inputHandler.getStackInSlot(0);
        if (item.isEmpty()) {
            return;
        }
        Optional<DryingRecipe> recipe = findDryingRecipe(item);
        if (recipe.isEmpty()) {
            // An item with no drying recipe simply rests on the rack — no progress to make.
            return;
        }
        DryingRecipe drying = recipe.get();
        if (dryProgress < drying.dryTime()) {
            dryProgress++;
            setChanged();
        }
        if (dryProgress >= drying.dryTime()) {
            // Drying complete — replace the wet item with the recipe output. onContentsChanged
            // resets the counter and refreshes the block-state to DONE.
            inputHandler.setStackInSlot(0, drying.assemble(new SingleRecipeInput(item), level.registryAccess()));
        }
        else {
            refreshDryingState();
        }
    }

    /** The drying recipe matching {@code item}, if any is registered. */
    private Optional<DryingRecipe> findDryingRecipe(ItemStack item) {
        if (level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(GadgetRecipes.DRYING_TYPE.get(), new SingleRecipeInput(item), level).map(RecipeHolder::value);
    }

    /** Recomputes the {@link DryingState} block-state property from the slot contents and progress. */
    private void refreshDryingState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        DryingState target;
        if (inputHandler.getStackInSlot(0).isEmpty()) {
            target = DryingState.EMPTY;
        }
        else {
            target = dryProgress > 0 ? DryingState.DRYING : DryingState.DONE;
        }
        BlockState current = getBlockState();
        if (current.hasProperty(DryingRackBlock.DRYING_STATE) && current.getValue(DryingRackBlock.DRYING_STATE) != target) {
            level.setBlock(getBlockPos(), current.setValue(DryingRackBlock.DRYING_STATE, target), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(TAG_ITEM, inputHandler.serializeNBT(provider));
        tag.putInt(TAG_PROGRESS, dryProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(TAG_ITEM, Tag.TAG_COMPOUND)) {
            inputHandler.deserializeNBT(provider, tag.getCompound(TAG_ITEM));
        }
        dryProgress = tag.getInt(TAG_PROGRESS);
    }
}
