package slimeknights.sconstruct.tools.inventory;

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

import slimeknights.sconstruct.tools.StencilTableRegistry;
import slimeknights.sconstruct.tools.block.entity.StencilTableBlockEntity;

/**
 * Container menu for the Stencil Table (SMTCON-91). Two BE-backed slots — input + output —
 * plus the standard 27 + 9 player inventory.
 *
 * <p>Slot semantics:
 *
 * <ul>
 *   <li><strong>Input</strong> (idx 0) — accepts blank patterns only. Predicate enforced at
 *       both the menu slot layer (via {@link InputSlot}) and the underlying handler layer
 *       (via {@link StencilTableBlockEntity#isBlankPattern}).</li>
 *   <li><strong>Output</strong> (idx 1) — read-only via {@link OutputSlot}. The BE pre-stamps
 *       this slot with a typed pattern whenever the input has stock + the cursor is set; the
 *       player can only <em>take</em> from the slot, which decrements the input by one and
 *       (if input still has stock) re-stamps the output with another typed pattern.</li>
 * </ul>
 *
 * <p>Two constructors — both required by the {@code IMenuTypeExtension.create} factory in
 * {@link StencilTableRegistry}. The client constructor falls back to a 2-slot stub handler
 * when the BE is missing (chunk just unloaded / BE removed between open + sync) so the client
 * never NPEs while the server's CloseMenu packet is in flight.
 */
public final class StencilTableMenu extends AbstractContainerMenu {

    /** GUI x of the input slot. */
    private static final int INPUT_X = 48;
    /** GUI y of the input slot. */
    private static final int INPUT_Y = 35;
    /** GUI x of the output slot. */
    private static final int OUTPUT_X = 112;
    /** GUI y of the output slot. */
    private static final int OUTPUT_Y = 35;

    /** Slot count owned by the BE (input + output). */
    private static final int BE_SLOTS = StencilTableBlockEntity.SLOTS;

    /** Player inventory layout — matches vanilla container conventions. */
    private static final int PLAYER_INV_COLS = 9;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y = PLAYER_INV_Y + PLAYER_INV_ROWS * 18 + 4;

    /** Maximum distance from which a player can interact with the table. */
    private static final double INTERACT_DISTANCE_SQ = 64.0D;

    @Nullable
    private final StencilTableBlockEntity blockEntity;

    /** Server-side constructor — called by {@link StencilTableBlockEntity#createMenu}. */
    public StencilTableMenu(int containerId, Inventory playerInventory, StencilTableBlockEntity blockEntity) {
        super(StencilTableRegistry.STENCIL_TABLE_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        addAllSlots(blockEntity, blockEntity.getHandler(), playerInventory);
    }

    /** Client-side constructor — invoked by {@code IMenuTypeExtension.create}. */
    public StencilTableMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(StencilTableRegistry.STENCIL_TABLE_MENU.get(), containerId);
        // Read BE pos opportunistically — pattern chest does the same. When openMenu was
        // called without an extraData writer the buf is empty; defaulting to a no-BE stub keeps
        // the client constructor robust against either path.
        StencilTableBlockEntity be = null;
        if (extraData.readableBytes() >= Long.BYTES) {
            BlockPos pos = extraData.readBlockPos();
            BlockEntity raw = playerInventory.player.level().getBlockEntity(pos);
            if (raw instanceof StencilTableBlockEntity table) {
                be = table;
            }
        }
        this.blockEntity = be;
        IItemHandler handler = be != null ? be.getHandler() : new ItemStackHandler(BE_SLOTS);
        addAllSlots(be, handler, playerInventory);
    }

    /** Single slot-layout pass shared by both constructors. */
    private void addAllSlots(@Nullable StencilTableBlockEntity be, IItemHandler handler, Inventory playerInventory) {
        addSlot(new InputSlot(handler, StencilTableBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y));
        addSlot(new OutputSlot(be, handler, StencilTableBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y));
        for (int row = 0; row < PLAYER_INV_ROWS; row++) {
            for (int col = 0; col < PLAYER_INV_COLS; col++) {
                addSlot(new Slot(playerInventory, col + row * PLAYER_INV_COLS + PLAYER_INV_COLS, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < PLAYER_INV_COLS; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    @Nullable
    public StencilTableBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            // Client stub — defer interaction validity to the server side.
            return true;
        }
        BlockPos pos = blockEntity.getBlockPos();
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
        // BE slots occupy [0, BE_SLOTS); player slots are everything after.
        if (slotIndex < BE_SLOTS) {
            // From a BE slot into the player inventory. Output slot is fine to drain this way;
            // input slot also drains but the BE will refresh the output on the next contents-
            // changed callback (input went to zero -> clear output).
            if (!moveItemStackTo(stack, BE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            // After moveItemStackTo, notify the slot of the take if this was the output slot —
            // the OutputSlot.onTake path is what decrements the input for the craft accounting.
            slot.onTake(player, original);
            if (slotIndex == StencilTableBlockEntity.OUTPUT_SLOT) {
                // Output slot's onTake re-stamps the slot via refreshOutput() — the trailing
                // {@code if (stack.isEmpty()) slot.set(ItemStack.EMPTY)} below would overwrite
                // the freshly-stamped pattern, so short-circuit before the clear.
                return original;
            }
        }
        else {
            // From the player inventory into the input slot only. Output rejects placement via
            // OutputSlot#mayPlace so attempting [BE_SLOTS, BE_SLOTS) makes the call a no-op.
            if (!moveItemStackTo(stack, StencilTableBlockEntity.INPUT_SLOT, StencilTableBlockEntity.INPUT_SLOT + 1, false)) {
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

    /**
     * Input slot — accepts blank patterns only. The handler's {@code isItemValid} is the
     * authoritative gate for capability-API neighbours, but the menu also enforces here so
     * the player's drag-and-drop respects the "blank only" rule without bouncing back.
     */
    private static final class InputSlot extends SlotItemHandler {
        InputSlot(IItemHandler handler, int slot, int x, int y) {
            super(handler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return StencilTableBlockEntity.isBlankPattern(stack);
        }
    }

    /**
     * Output slot — rejects placement (the BE owns the contents), allows take. {@code onTake}
     * delegates to the BE's {@link StencilTableBlockEntity#consumeOneInput} to deduct the input
     * stock; the BE's input-changed callback then either re-stamps the output (input still has
     * stock) or clears it (input emptied).
     */
    private static final class OutputSlot extends SlotItemHandler {
        @Nullable
        private final StencilTableBlockEntity blockEntity;

        OutputSlot(@Nullable StencilTableBlockEntity blockEntity, IItemHandler handler, int slot, int x, int y) {
            super(handler, slot, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            if (blockEntity != null) {
                blockEntity.consumeOneInput();
            }
        }
    }
}
