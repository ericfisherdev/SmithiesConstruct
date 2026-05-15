package slimeknights.sconstruct.port1211.tools.inventory;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import slimeknights.sconstruct.port1211.tools.PatternChestRegistry;
import slimeknights.sconstruct.port1211.tools.block.entity.PatternChestBlockEntity;

/**
 * Container menu for the 32-slot Pattern Chest (SMTCON-90). Lays out 8 columns × 4 rows of
 * chest slots (32 total) bound to the BE's {@link IItemHandler}, then the standard 27-slot
 * player inventory + 9-slot hotbar beneath.
 *
 * <p>Two constructors — both required by the {@code IMenuTypeExtension.create} factory in
 * {@link PatternChestRegistry}:
 *
 * <ul>
 *   <li><strong>Server</strong> {@code (containerId, playerInv, blockEntity)} — used by
 *       {@link PatternChestBlockEntity#createMenu} when the server opens the menu.</li>
 *   <li><strong>Client</strong> {@code (containerId, playerInv, FriendlyByteBuf)} — used by
 *       the menu type factory; reconstructs the BE handle by reading the chest's
 *       {@link BlockPos} from the extra-data buffer and looking up the BE in the player's
 *       level. Falls back to a 32-slot stub handler if the BE is missing (chunk just
 *       unloaded, BE removed between open + sync, etc.) so the client never NPEs while
 *       the server's CloseMenu packet is in flight.</li>
 * </ul>
 *
 * <p>{@link #quickMoveStack} implements the standard chest split: shift-clicking a chest slot
 * moves the stack into the player inventory; shift-clicking a player slot moves into the
 * chest. Returning {@link ItemStack#EMPTY} when no movement happens is the
 * {@link AbstractContainerMenu} contract — vanilla relies on the returned value to know
 * whether to re-render the source slot.
 */
public final class PatternChestMenu extends AbstractContainerMenu {

    private static final int CHEST_COLS = 8;
    private static final int CHEST_ROWS = 4;
    /** Total chest slots == {@link PatternChestBlockEntity#SLOTS}. */
    private static final int CHEST_SLOTS = CHEST_COLS * CHEST_ROWS;

    /** First-slot x offset for the 8-column chest grid. Centres the grid in a 176-wide GUI. */
    private static final int CHEST_X = 8;
    /** First-slot y offset for the chest grid. */
    private static final int CHEST_Y = 18;

    /** Player inventory layout constants — matches vanilla container conventions. */
    private static final int PLAYER_INV_COLS = 9;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_X = 8;
    /** y offset for the top row of the 27-slot player inventory. */
    private static final int PLAYER_INV_Y = 18 + CHEST_ROWS * 18 + 14;
    /** y offset for the hotbar. */
    private static final int HOTBAR_Y = PLAYER_INV_Y + PLAYER_INV_ROWS * 18 + 4;

    /** Maximum distance from which a player can interact with the chest. */
    private static final double INTERACT_DISTANCE_SQ = 64.0D;

    @Nullable
    private final PatternChestBlockEntity blockEntity;

    /** Server-side constructor — called by {@link PatternChestBlockEntity#createMenu}. The
     *  class is {@code final} so the {@code addSlot} call inside {@link #addAllSlots} cannot
     *  be intercepted by a subclass — vanilla's slots-during-construction lifecycle is safe. */
    public PatternChestMenu(int containerId, Inventory playerInventory, PatternChestBlockEntity blockEntity) {
        super(PatternChestRegistry.PATTERN_CHEST_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        addAllSlots(blockEntity.getHandler(), playerInventory);
    }

    /** Client-side constructor — invoked by {@code IMenuTypeExtension.create}. Same final-class
     *  rationale as the server-side constructor above. */
    public PatternChestMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(PatternChestRegistry.PATTERN_CHEST_MENU.get(), containerId);
        BlockPos pos = extraData.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        this.blockEntity = be instanceof PatternChestBlockEntity ? (PatternChestBlockEntity) be : null;
        IItemHandler handler = blockEntity != null ? blockEntity.getHandler() : new ItemStackHandler(PatternChestBlockEntity.SLOTS);
        addAllSlots(handler, playerInventory);
    }

    /** Single slot-layout pass shared by both constructors. Private + only called from
     *  constructors — non-overridable by any concrete subclass (the class is final). */
    private void addAllSlots(IItemHandler handler, Inventory playerInventory) {
        for (int row = 0; row < CHEST_ROWS; row++) {
            for (int col = 0; col < CHEST_COLS; col++) {
                int index = row * CHEST_COLS + col;
                addSlot(new SlotItemHandler(handler, index, CHEST_X + col * 18, CHEST_Y + row * 18));
            }
        }
        for (int row = 0; row < PLAYER_INV_ROWS; row++) {
            for (int col = 0; col < PLAYER_INV_COLS; col++) {
                addSlot(new Slot(playerInventory, col + row * PLAYER_INV_COLS + PLAYER_INV_COLS, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < PLAYER_INV_COLS; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            // Client stub — defer interaction validity to the server side, which holds the
            // authoritative BE handle. Returning true keeps the GUI usable on the client; the
            // server's stillValid check is what actually gates inventory mutations.
            return true;
        }
        return player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5D, blockEntity.getBlockPos().getY() + 0.5D, blockEntity.getBlockPos().getZ() + 0.5D) <= INTERACT_DISTANCE_SQ;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        // Shift-click direction: chest slots (0..CHEST_SLOTS) push into player inventory
        // (CHEST_SLOTS..end); player slots push into chest. moveItemStackTo with the standard
        // [from, to) range mirrors vanilla ChestMenu behaviour.
        if (slotIndex < CHEST_SLOTS) {
            if (!moveItemStackTo(stack, CHEST_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        }
        else {
            if (!moveItemStackTo(stack, 0, CHEST_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        }
        else {
            slot.setChanged();
        }
        return original;
    }
}
