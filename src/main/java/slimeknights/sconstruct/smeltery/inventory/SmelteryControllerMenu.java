package slimeknights.sconstruct.smeltery.inventory;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import slimeknights.sconstruct.smeltery.SmelteryComponents;
import slimeknights.sconstruct.smeltery.block.entity.SmelteryControllerBlockEntity;

/**
 * Container menu for the smeltery controller (SMTCON-125 / SMTCON-216). The controller's melting
 * inventory is sized to the assembled smeltery's interior volume, so it can hold far more than
 * the {@value #MELTING_COLS}&times;{@value #VISIBLE_ROWS} cells the GUI shows at once.
 *
 * <p>{@code Slot} positions are final, so the menu cannot move slots to scroll. Instead it backs
 * its {@value #VISIBLE_SLOTS} fixed melting slots with a {@link ScrollWindowItemHandler} — a
 * sliding window over the real inventory. {@link #setScrollRow} just moves that window's offset,
 * keeping the menu's slot set stable while every inventory slot stays reachable.
 *
 * <p>The standard 27 + 9 player inventory follows, plus {@link DataSlot}s syncing the
 * controller's temperature and the visible window's per-slot melt progress. The tank fluid is
 * <em>not</em> synced through the menu — the screen reads it off the block-entity, which the
 * SMTCON-124 sync payloads keep current.
 */
public final class SmelteryControllerMenu extends AbstractContainerMenu {

    /** Columns in the melting-slot grid. */
    public static final int MELTING_COLS = 3;

    /** Rows of melting slots visible at once — the scroll window height. */
    public static final int VISIBLE_ROWS = 3;

    /** Melting slots visible at once. */
    public static final int VISIBLE_SLOTS = MELTING_COLS * VISIBLE_ROWS;

    /** Pixel pitch between adjacent slots — the vanilla 18px slot cell. */
    private static final int SLOT_PITCH = 18;

    /** GUI x of the first (top-left) melting slot. */
    public static final int MELTING_X = 62;
    /** GUI y of the first (top-left) melting slot. */
    public static final int MELTING_Y = 17;

    private static final int PLAYER_INV_COLS = 9;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y = PLAYER_INV_Y + PLAYER_INV_ROWS * SLOT_PITCH + 4;

    /** Maximum distance squared from which a player may keep the controller menu open. */
    private static final double INTERACT_DISTANCE_SQ = 64.0D;

    @Nullable
    private final SmelteryControllerBlockEntity controller;

    /** The controller's full melting inventory — the real handler the scroll window slides over. */
    private final IItemHandler meltingInventory;

    /** The {@value #VISIBLE_SLOTS}-slot sliding window the visible melting slots are backed by. */
    private final ScrollWindowItemHandler window;

    /** First melting-grid row currently visible — the scroll offset, clamped to {@link #maxScrollRow()}. */
    private int scrollRow;

    private final DataSlot currentTemperature = DataSlot.standalone();
    private final DataSlot targetTemperature = DataSlot.standalone();
    private final DataSlot[] meltProgress = new DataSlot[VISIBLE_SLOTS];

    /** Server-side constructor — called by {@link SmelteryControllerBlockEntity#createMenu}. */
    public SmelteryControllerMenu(int containerId, Inventory playerInventory, SmelteryControllerBlockEntity controller) {
        super(SmelteryComponents.SMELTERY_CONTROLLER_MENU.get(), containerId);
        this.controller = controller;
        this.meltingInventory = controller.getItemHandler();
        this.window = new ScrollWindowItemHandler(meltingInventory, VISIBLE_SLOTS);
        addAllSlots(playerInventory);
        addDataSlots();
    }

    /** Client-side constructor — invoked by {@code IMenuTypeExtension.create}. */
    public SmelteryControllerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(SmelteryComponents.SMELTERY_CONTROLLER_MENU.get(), containerId);
        SmelteryControllerBlockEntity be = null;
        if (extraData.readableBytes() >= Long.BYTES) {
            BlockEntity raw = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
            if (raw instanceof SmelteryControllerBlockEntity found) {
                be = found;
            }
        }
        this.controller = be;
        this.meltingInventory = be != null ? be.getItemHandler() : new ItemStackHandler(SmelteryControllerBlockEntity.INITIAL_MELTING_SLOTS);
        this.window = new ScrollWindowItemHandler(meltingInventory, VISIBLE_SLOTS);
        addAllSlots(playerInventory);
        addDataSlots();
    }

    /** Lays out the visible melting-slot window and the player inventory; shared by both constructors. */
    private void addAllSlots(Inventory playerInventory) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < MELTING_COLS; col++) {
                addSlot(new WindowSlot(window, col + row * MELTING_COLS, MELTING_X + col * SLOT_PITCH, MELTING_Y + row * SLOT_PITCH));
            }
        }
        for (int row = 0; row < PLAYER_INV_ROWS; row++) {
            for (int col = 0; col < PLAYER_INV_COLS; col++) {
                addSlot(new Slot(playerInventory, col + row * PLAYER_INV_COLS + PLAYER_INV_COLS, PLAYER_INV_X + col * SLOT_PITCH, PLAYER_INV_Y + row * SLOT_PITCH));
            }
        }
        for (int col = 0; col < PLAYER_INV_COLS; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * SLOT_PITCH, HOTBAR_Y));
        }
    }

    /**
     * Registers the temperature and per-visible-slot melt-progress data slots. The slots are
     * plain standalone holders refreshed from the controller in {@link #broadcastChanges()}; the
     * vanilla menu sync mirrors them to the client where the screen reads them.
     */
    private void addDataSlots() {
        addDataSlot(currentTemperature);
        addDataSlot(targetTemperature);
        for (int slot = 0; slot < VISIBLE_SLOTS; slot++) {
            meltProgress[slot] = DataSlot.standalone();
            addDataSlot(meltProgress[slot]);
        }
    }

    /** The number of melting slots the controller exposes. */
    public int meltingSlotCount() {
        return meltingInventory.getSlots();
    }

    /** The highest valid scroll row — zero when the inventory fits the visible window. */
    public int maxScrollRow() {
        int rows = (meltingInventory.getSlots() + MELTING_COLS - 1) / MELTING_COLS;
        return Math.max(0, rows - VISIBLE_ROWS);
    }

    /** The current scroll offset in melting-grid rows. */
    public int getScrollRow() {
        return scrollRow;
    }

    /** Scrolls the visible window to {@code row}, clamped to {@link #maxScrollRow()}. */
    public void setScrollRow(int row) {
        scrollRow = Math.clamp(row, 0, maxScrollRow());
        window.setOffset(scrollRow * MELTING_COLS);
    }

    /**
     * Copies the controller's live state into the data slots before the standard sync runs. The
     * melt-progress slots track the visible window, so they follow the scroll offset.
     */
    @Override
    public void broadcastChanges() {
        if (controller != null) {
            currentTemperature.set(controller.getCurrentTemperature());
            targetTemperature.set(controller.getTargetTemperature());
            for (int visible = 0; visible < meltProgress.length; visible++) {
                meltProgress[visible].set(controller.getMeltProgress(scrollRow * MELTING_COLS + visible));
            }
        }
        super.broadcastChanges();
    }

    /** The controller backing this menu, or {@code null} on a client stub with no block-entity. */
    @Nullable
    public SmelteryControllerBlockEntity getController() {
        return controller;
    }

    /** The smeltery's current internal temperature in kelvin, as synced to this menu. */
    public int getCurrentTemperature() {
        return currentTemperature.get();
    }

    /** The temperature in kelvin the active fuel source can sustain, as synced to this menu. */
    public int getTargetTemperature() {
        return targetTemperature.get();
    }

    /** The melt progress (0–100) of the given visible-window slot, as synced to this menu. */
    public int getMeltProgress(int visibleSlot) {
        return visibleSlot >= 0 && visibleSlot < meltProgress.length ? meltProgress[visibleSlot].get() : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        if (controller == null) {
            // Only the client builds a no-controller stub; a server menu always has its
            // block-entity, so a null controller there is invalid rather than permissive.
            return player.level().isClientSide();
        }
        BlockPos pos = controller.getBlockPos();
        // The controller may have been broken — or broken and replaced — while the menu stayed
        // open; require the live block-entity to still be the very controller this menu was
        // built against so a stale reference cannot keep the menu interactable.
        if (!controller.equals(player.level().getBlockEntity(pos))) {
            return false;
        }
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= INTERACT_DISTANCE_SQ;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < VISIBLE_SLOTS) {
            // From a visible melting slot out into the player inventory.
            if (!moveItemStackTo(stack, VISIBLE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        }
        else if (!moveItemStackTo(stack, 0, VISIBLE_SLOTS, false)) {
            // From the player inventory into the visible melting-slot window.
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        }
        else {
            slot.setChanged();
        }
        return original;
    }

    /**
     * A melting slot backed by the {@link ScrollWindowItemHandler}. It goes inactive when its
     * window position currently maps past the end of the real inventory — a partly-filled final
     * row, or an inventory smaller than the window — so vanilla skips it for rendering and clicks.
     */
    private static final class WindowSlot extends SlotItemHandler {

        private final ScrollWindowItemHandler windowHandler;
        private final int windowIndex;

        private WindowSlot(ScrollWindowItemHandler windowHandler, int windowIndex, int x, int y) {
            super(windowHandler, windowIndex, x, y);
            this.windowHandler = windowHandler;
            this.windowIndex = windowIndex;
        }

        @Override
        public boolean isActive() {
            return windowHandler.isRealSlot(windowIndex);
        }
    }
}
