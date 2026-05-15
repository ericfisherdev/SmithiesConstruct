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

import slimeknights.sconstruct.port1211.tools.PartBuilderRegistry;
import slimeknights.sconstruct.port1211.tools.block.entity.PartBuilderBlockEntity;
import slimeknights.sconstruct.port1211.tools.item.PatternItem;

/**
 * Container menu for the Part Builder (SMTCON-92). Three BE-backed slots — pattern, material,
 * output — plus the standard 27 + 9 player inventory.
 *
 * <p>Slot semantics:
 *
 * <ul>
 *   <li><strong>Pattern</strong> (idx 0) — accepts typed patterns only (a
 *       {@link PatternItem} stack with the {@code TINKER_PATTERN_PART} component present),
 *       enforced via {@link PatternInputSlot}.</li>
 *   <li><strong>Material</strong> (idx 1) — accepts any item at the menu layer; the server-side
 *       {@code refreshOutput} on the BE is the authoritative gate for whether the held stack
 *       matches a registered {@link slimeknights.sconstruct.port1211.tools.material.Material}'s
 *       {@code repair_tag}. A non-match just produces no output.</li>
 *   <li><strong>Output</strong> (idx 2) — read-only via {@link OutputSlot}. The BE pre-stamps
 *       this slot whenever both inputs resolve to a known material; the player can only
 *       <em>take</em> from the slot, which decrements both inputs by one and (if stock
 *       remains) re-stamps another part.</li>
 * </ul>
 */
public final class PartBuilderMenu extends AbstractContainerMenu {

    /** GUI x of the pattern slot. */
    private static final int PATTERN_X = 26;
    /** GUI y of the pattern slot. */
    private static final int PATTERN_Y = 35;
    /** GUI x of the material slot. */
    private static final int MATERIAL_X = 62;
    /** GUI y of the material slot. */
    private static final int MATERIAL_Y = 35;
    /** GUI x of the output slot. */
    private static final int OUTPUT_X = 120;
    /** GUI y of the output slot. */
    private static final int OUTPUT_Y = 35;

    /** Slot count owned by the BE (pattern + material + output). */
    private static final int BE_SLOTS = PartBuilderBlockEntity.SLOTS;

    /** Player inventory layout — matches vanilla container conventions. */
    private static final int PLAYER_INV_COLS = 9;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y = PLAYER_INV_Y + PLAYER_INV_ROWS * 18 + 4;

    /** Maximum squared distance for the interaction-validity check. */
    private static final double INTERACT_DISTANCE_SQ = 64.0D;

    @Nullable
    private final PartBuilderBlockEntity blockEntity;

    /** Server-side constructor — called by {@link PartBuilderBlockEntity#createMenu}. */
    public PartBuilderMenu(int containerId, Inventory playerInventory, PartBuilderBlockEntity blockEntity) {
        super(PartBuilderRegistry.PART_BUILDER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        addAllSlots(blockEntity, blockEntity.getHandler(), playerInventory);
    }

    /** Client-side constructor — invoked by {@code IMenuTypeExtension.create}. */
    public PartBuilderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(PartBuilderRegistry.PART_BUILDER_MENU.get(), containerId);
        PartBuilderBlockEntity be = null;
        if (extraData.readableBytes() >= Long.BYTES) {
            BlockPos pos = extraData.readBlockPos();
            BlockEntity raw = playerInventory.player.level().getBlockEntity(pos);
            if (raw instanceof PartBuilderBlockEntity table) {
                be = table;
            }
        }
        this.blockEntity = be;
        IItemHandler handler = be != null ? be.getHandler() : new ItemStackHandler(BE_SLOTS);
        addAllSlots(be, handler, playerInventory);
    }

    /** Single slot-layout pass shared by both constructors. */
    private void addAllSlots(@Nullable PartBuilderBlockEntity be, IItemHandler handler, Inventory playerInventory) {
        addSlot(new PatternInputSlot(handler, PartBuilderBlockEntity.PATTERN_SLOT, PATTERN_X, PATTERN_Y));
        addSlot(new MaterialInputSlot(handler, PartBuilderBlockEntity.MATERIAL_SLOT, MATERIAL_X, MATERIAL_Y));
        addSlot(new OutputSlot(be, handler, PartBuilderBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y));
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
    public PartBuilderBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /** Identity comparison ({@code != this}) on the live block entity is intentional — vanilla
     *  AbstractFurnaceBlockEntity#stillValid uses the same reference-equality gate to detect
     *  BE replacement (world-edit overwrite, chunk reload), and value-equality on a BE is not
     *  meaningful since two distinct BEs at the same pos with the same data should not be
     *  treated as the same instance. */
    @Override
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            // Client stub — defer interaction validity to the server side.
            return true;
        }
        // Liveness + identity check ahead of the distance check: a BE that has been removed
        // (block broken, chunk unloaded) or replaced (e.g. world-edit overwrote the block)
        // must not keep the menu open on the player.
        BlockPos pos = blockEntity.getBlockPos();
        if (blockEntity.isRemoved() || blockEntity.getLevel() == null || blockEntity.getLevel().getBlockEntity(pos) != blockEntity) {
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
        // BE slots occupy [0, BE_SLOTS); player slots are everything after.
        if (slotIndex < BE_SLOTS) {
            // From a BE slot into the player inventory. Output slot also drains this way; its
            // onTake decrements both inputs for the craft accounting.
            if (!moveItemStackTo(stack, BE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, original);
            if (slotIndex == PartBuilderBlockEntity.OUTPUT_SLOT) {
                // Output's onTake re-stamps the slot via refreshOutput() — the trailing
                // {@code if (stack.isEmpty()) slot.set(ItemStack.EMPTY)} below would overwrite
                // the freshly-stamped part, so short-circuit before the clear.
                return original;
            }
        }
        else {
            // From the player inventory into one of the input slots. Try pattern slot first
            // when the stack is a typed pattern, otherwise material slot. Output rejects
            // placement via OutputSlot#mayPlace so attempting that range is a no-op anyway.
            boolean isTypedPattern = PatternItem.getPart(stack).isPresent();
            if (isTypedPattern) {
                if (!moveItemStackTo(stack, PartBuilderBlockEntity.PATTERN_SLOT, PartBuilderBlockEntity.PATTERN_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            else if (!moveItemStackTo(stack, PartBuilderBlockEntity.MATERIAL_SLOT, PartBuilderBlockEntity.MATERIAL_SLOT + 1, false)) {
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
     * Pattern slot — accepts typed patterns only. The handler's {@code isItemValid} is the
     * authoritative gate for capability-API neighbours; the menu enforcement here keeps
     * drag-and-drop honest without bouncing back.
     */
    private static final class PatternInputSlot extends SlotItemHandler {
        PatternInputSlot(IItemHandler handler, int slot, int x, int y) {
            super(handler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Typed-pattern only — the typed pattern carries the TINKER_PATTERN_PART component.
            return PatternItem.getPart(stack).isPresent();
        }
    }

    /**
     * Material slot — permissive at the menu layer. The BE's {@code refreshOutput} is the
     * source of truth for whether the inserted stack resolves to a known
     * {@link slimeknights.sconstruct.port1211.tools.material.Material}; an unmatched stack
     * just produces no output.
     */
    private static final class MaterialInputSlot extends SlotItemHandler {
        MaterialInputSlot(IItemHandler handler, int slot, int x, int y) {
            super(handler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }

    /**
     * Output slot — rejects placement (the BE owns the contents), allows take. {@code onTake}
     * delegates to {@link PartBuilderBlockEntity#consumeInputs} to deduct one pattern + one
     * material; the BE's input-changed callback then either re-stamps the output (both inputs
     * still have stock + the material still matches) or clears it.
     */
    private static final class OutputSlot extends SlotItemHandler {
        @Nullable
        private final PartBuilderBlockEntity blockEntity;

        OutputSlot(@Nullable PartBuilderBlockEntity blockEntity, IItemHandler handler, int slot, int x, int y) {
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
            // Server-only mutation: consumeInputs() decrements stacks and re-runs the registry
            // lookup that powers refreshOutput(), both of which are authoritative server state.
            // The client menu carries a cached BE reference from the open-screen sync but must
            // not mutate it — wait for the server to broadcast the slot updates instead.
            if (blockEntity != null && !player.level().isClientSide()) {
                blockEntity.consumeInputs();
            }
        }
    }
}
