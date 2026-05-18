package slimeknights.sconstruct.tools.block.entity;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import slimeknights.sconstruct.tools.PatternChestRegistry;
import slimeknights.sconstruct.tools.inventory.PatternChestMenu;

/**
 * Block entity backing the 32-slot Pattern Chest (SMTCON-90). Owns a single
 * {@link ItemStackHandler} of fixed size 32; persistence round-trips that handler
 * through {@link #saveAdditional(CompoundTag, HolderLookup.Provider)} /
 * {@link #loadAdditional(CompoundTag, HolderLookup.Provider)} so chest contents survive
 * a world reload.
 *
 * <p>Implements {@link MenuProvider} so a server-side
 * {@code player.openMenu(state.getMenuProvider(level, pos))} call from
 * {@link slimeknights.sconstruct.tools.block.PatternChestBlock#useWithoutItem}
 * returns the BE itself as the provider — the matching client constructor on
 * {@link PatternChestMenu} reconstructs the menu by looking up the BE through the
 * extra-data block-pos.
 *
 * <p>The handler is also the inventory exposed through {@code Capabilities.ItemHandler.BLOCK}
 * (registered in {@link slimeknights.sconstruct.tools.ToolCapabilities}), which is
 * what makes the chest hopper-compatible: a hopper pushing into the chest's top face calls
 * into {@link #getHandler()} via the capability and inserts into slot 0 onward.
 */
public class PatternChestBlockEntity extends BlockEntity implements MenuProvider {

    /** Inventory slot count. 32 matches the legacy 1.12 pattern chest size. */
    public static final int SLOTS = 32;

    /** NBT key under which the handler's serialised state is stored. */
    private static final String TAG_INVENTORY = "Inventory";

    private final ItemStackHandler handler = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            // Mark the chunk dirty so the world saves the updated handler state on next flush.
            setChanged();
        }
    };

    public PatternChestBlockEntity(BlockPos pos, BlockState state) {
        super(PatternChestRegistry.PATTERN_CHEST_BE.get(), pos, state);
    }

    /**
     * Backing handler accessor. Used both by the menu (so {@link PatternChestMenu} can bind
     * {@code SlotItemHandler}s against the live BE handler) and by the
     * {@code Capabilities.ItemHandler.BLOCK} provider registered in
     * {@link slimeknights.sconstruct.tools.ToolCapabilities} so hoppers see the same
     * inventory the player sees in the GUI.
     */
    public ItemStackHandler getHandler() {
        return handler;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        // Round-trip the entire handler through NBT. ItemStackHandler#serializeNBT writes
        // every non-empty slot and the slot count, so the deserialise pass restores the
        // exact contents (and the same size) — no per-slot tag plumbing here.
        tag.put(TAG_INVENTORY, handler.serializeNBT(provider));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(TAG_INVENTORY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            handler.deserializeNBT(provider, tag.getCompound(TAG_INVENTORY));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.sconstruct.pattern_chest");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        Objects.requireNonNull(playerInventory, "playerInventory");
        Objects.requireNonNull(player, "player");
        return new PatternChestMenu(containerId, playerInventory, this);
    }
}
