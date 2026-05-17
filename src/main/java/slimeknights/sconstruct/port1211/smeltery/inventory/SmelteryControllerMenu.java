package slimeknights.sconstruct.port1211.smeltery.inventory;

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

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryControllerBlockEntity;

/**
 * Container menu for the smeltery controller (SMTCON-125). Lays out the controller's nine
 * melting slots as a 3&times;3 grid backed by {@link SmelteryControllerBlockEntity#getItemHandler()},
 * plus the standard 27 + 9 player inventory, and syncs the controller's live state — current and
 * target temperature, and the per-slot melt progress — through {@link DataSlot}s so the
 * {@code SmelteryControllerScreen} can render the heat gauge and slot progress bars.
 *
 * <p>The tank fluid is <em>not</em> synced through the menu — the screen reads it straight off
 * the block-entity, which is kept current by the SMTCON-124 sync payloads. Only the integer
 * state that has no other sync path travels through the data slots here.
 */
public final class SmelteryControllerMenu extends AbstractContainerMenu {

    /** Side length of the square melting-slot grid (9 slots → 3&times;3). */
    private static final int MELTING_GRID = 3;

    /** Number of melting slots owned by the controller block-entity. */
    private static final int MELTING_SLOTS = SmelteryControllerBlockEntity.INITIAL_MELTING_SLOTS;

    /** Pixel pitch between adjacent slots — the vanilla 18px slot cell. */
    private static final int SLOT_PITCH = 18;

    /** GUI x of the first (top-left) melting slot. */
    private static final int MELTING_X = 62;
    /** GUI y of the first (top-left) melting slot. */
    private static final int MELTING_Y = 17;

    private static final int PLAYER_INV_COLS = 9;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y = PLAYER_INV_Y + PLAYER_INV_ROWS * SLOT_PITCH + 4;

    /** Maximum distance squared from which a player may keep the controller menu open. */
    private static final double INTERACT_DISTANCE_SQ = 64.0D;

    @Nullable
    private final SmelteryControllerBlockEntity controller;

    private final DataSlot currentTemperature = DataSlot.standalone();
    private final DataSlot targetTemperature = DataSlot.standalone();
    private final DataSlot[] meltProgress = new DataSlot[MELTING_SLOTS];

    /** Server-side constructor — called by {@link SmelteryControllerBlockEntity#createMenu}. */
    public SmelteryControllerMenu(int containerId, Inventory playerInventory, SmelteryControllerBlockEntity controller) {
        super(SmelteryComponents.SMELTERY_CONTROLLER_MENU.get(), containerId);
        this.controller = controller;
        addAllSlots(controller.getItemHandler(), playerInventory);
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
        IItemHandler handler = be != null ? be.getItemHandler() : new ItemStackHandler(MELTING_SLOTS);
        addAllSlots(handler, playerInventory);
        addDataSlots();
    }

    /** Lays out the melting-slot grid and the player inventory; shared by both constructors. */
    private void addAllSlots(IItemHandler meltingSlots, Inventory playerInventory) {
        for (int row = 0; row < MELTING_GRID; row++) {
            for (int col = 0; col < MELTING_GRID; col++) {
                addSlot(new SlotItemHandler(meltingSlots, col + row * MELTING_GRID, MELTING_X + col * SLOT_PITCH, MELTING_Y + row * SLOT_PITCH));
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
     * Registers the temperature and per-slot melt-progress data slots. The slots are plain
     * standalone holders: the server refreshes them from the controller in
     * {@link #broadcastChanges()} and the vanilla menu sync mirrors the values to the client,
     * where {@link DataSlot#get()} then returns the synced value the screen reads.
     */
    private void addDataSlots() {
        addDataSlot(currentTemperature);
        addDataSlot(targetTemperature);
        for (int slot = 0; slot < MELTING_SLOTS; slot++) {
            meltProgress[slot] = DataSlot.standalone();
            addDataSlot(meltProgress[slot]);
        }
    }

    /**
     * Copies the controller's live state into the data slots before the standard sync runs, so
     * every change is broadcast to the client this tick. Server-side only — the client never
     * calls {@code broadcastChanges} — so the client's slots keep the values delivered by sync.
     */
    @Override
    public void broadcastChanges() {
        if (controller != null) {
            currentTemperature.set(controller.getCurrentTemperature());
            targetTemperature.set(controller.getTargetTemperature());
            for (int slot = 0; slot < meltProgress.length; slot++) {
                meltProgress[slot].set(controller.getMeltProgress(slot));
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

    /** The melt progress (0–100) of the given melting slot, as synced to this menu. */
    public int getMeltProgress(int slot) {
        return slot >= 0 && slot < meltProgress.length ? meltProgress[slot].get() : 0;
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
        if (slotIndex < MELTING_SLOTS) {
            // From a melting slot out into the player inventory.
            if (!moveItemStackTo(stack, MELTING_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        }
        else if (!moveItemStackTo(stack, 0, MELTING_SLOTS, false)) {
            // From the player inventory into the melting-slot grid.
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
}
