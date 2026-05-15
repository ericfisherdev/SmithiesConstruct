package slimeknights.sconstruct.port1211.tools.inventory;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import slimeknights.sconstruct.port1211.tools.ToolStationRegistry;
import slimeknights.sconstruct.port1211.tools.block.entity.ToolStationBlockEntity;
import slimeknights.sconstruct.port1211.tools.item.ToolCore;

/**
 * Container menu for the Tool Station / Tool Forge (SMTCON-93). Six BE-backed input slots
 * + one output slot + the standard 27 + 9 player inventory.
 *
 * <p>Slot semantics:
 *
 * <ul>
 *   <li><strong>Inputs</strong> (idx 0..5) — accept any item at the menu / handler layer. The
 *       server-side {@code refreshOutput} on the BE is the source of truth for whether the
 *       inputs assemble into a known tool; non-matching inputs just produce no output.</li>
 *   <li><strong>Output</strong> (idx 6) — read-only via {@link OutputSlot}. The BE pre-stamps
 *       this slot whenever the inputs resolve; the player can only <em>take</em> from the
 *       slot, which routes through {@code consumeBuildInputs} (build path) or
 *       {@code consumeModifyInput} (modify path) on the BE.</li>
 * </ul>
 *
 * <p>The same menu class backs both the Tool Station and the Tool Forge — they share slot
 * shape and behaviour; the only difference is which {@link MenuType} (and therefore which BE
 * type backing the connection) the screen is opened with. The constructor's {@code menuType}
 * parameter lets the registry feed the right type in.
 */
public final class ToolStationMenu extends AbstractContainerMenu {

    /** GUI x of the leftmost input slot. */
    private static final int INPUT_GRID_X = 26;
    /** GUI y of the input row. Two rows of 3 slots vertically aligned with the output. */
    private static final int INPUT_GRID_Y = 17;
    /** GUI x of the output slot. */
    private static final int OUTPUT_X = 134;
    /** GUI y of the output slot. */
    private static final int OUTPUT_Y = 35;

    /** Slot count owned by the BE (6 inputs + 1 output). */
    private static final int BE_SLOTS = ToolStationBlockEntity.SLOTS;

    private static final int PLAYER_INV_COLS = 9;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y = PLAYER_INV_Y + PLAYER_INV_ROWS * 18 + 4;

    /** Maximum squared distance for the interaction-validity check. */
    private static final double INTERACT_DISTANCE_SQ = 64.0D;

    @Nullable
    private final ToolStationBlockEntity blockEntity;

    /** Server-side constructor — called by {@link ToolStationBlockEntity#createMenu}. */
    public ToolStationMenu(int containerId, Inventory playerInventory, ToolStationBlockEntity blockEntity) {
        super(resolveMenuType(blockEntity), containerId);
        this.blockEntity = blockEntity;
        addAllSlots(blockEntity, blockEntity.getHandler(), playerInventory);
    }

    /**
     * Factory for the client-side menu instantiation via {@link IMenuTypeExtension#create}.
     * The {@code advanced} flag is baked in at registration time — the station / forge menu
     * types each call this with the right flag so the {@link MenuType} that super() pins
     * matches the registered type identity (a mismatch would assert-fail on packet receipt).
     */
    public static ToolStationMenu fromNetwork(boolean advanced, int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        return new ToolStationMenu(advanced, containerId, playerInventory, extraData);
    }

    /** Client-side constructor — invoked by {@link #fromNetwork} via {@code IMenuTypeExtension.create}. */
    private ToolStationMenu(boolean advanced, int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(advanced ? ToolStationRegistry.TOOL_FORGE_MENU.get() : ToolStationRegistry.TOOL_STATION_MENU.get(), containerId);
        ToolStationBlockEntity be = null;
        if (extraData.readableBytes() >= Long.BYTES) {
            BlockPos pos = extraData.readBlockPos();
            BlockEntity raw = playerInventory.player.level().getBlockEntity(pos);
            if (raw instanceof ToolStationBlockEntity station) {
                be = station;
            }
        }
        this.blockEntity = be;
        IItemHandler handler = be != null ? be.getHandler() : new ItemStackHandler(BE_SLOTS);
        addAllSlots(be, handler, playerInventory);
    }

    /**
     * Pick the {@link MenuType} for the server-side constructor based on the BE's concrete
     * class — the Tool Forge BE registers against {@link ToolStationRegistry#TOOL_FORGE_MENU},
     * everything else (base Tool Station) against {@link ToolStationRegistry#TOOL_STATION_MENU}.
     */
    private static MenuType<?> resolveMenuType(ToolStationBlockEntity blockEntity) {
        if (blockEntity.acceptsAdvancedTools()) {
            return ToolStationRegistry.TOOL_FORGE_MENU.get();
        }
        return ToolStationRegistry.TOOL_STATION_MENU.get();
    }

    /** Single slot-layout pass shared by both constructors. */
    private void addAllSlots(@Nullable ToolStationBlockEntity be, IItemHandler handler, Inventory playerInventory) {
        // Inputs laid out 3 wide × 2 tall starting at (INPUT_GRID_X, INPUT_GRID_Y). Slot indices
        // are positional 0..5 in row-major order; the BE reads them in the same order so the
        // top-left slot is the canonical "slot 0" for the modify path.
        for (int i = 0; i < ToolStationBlockEntity.INPUT_SLOTS; i++) {
            int col = i % 3;
            int row = i / 3;
            addSlot(new InputSlot(handler, i, INPUT_GRID_X + col * 18, INPUT_GRID_Y + row * 18));
        }
        addSlot(new OutputSlot(be, handler, ToolStationBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y));
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
    public ToolStationBlockEntity getBlockEntity() {
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
        if (slotIndex < BE_SLOTS) {
            // BE → player inventory.
            if (!moveItemStackTo(stack, BE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, original);
            if (slotIndex == ToolStationBlockEntity.OUTPUT_SLOT) {
                // Output's onTake decrements inputs; the BE then re-stamps the output. Skip
                // the trailing slot.set(EMPTY) so the freshly-stamped output isn't wiped.
                return original;
            }
        }
        else {
            // Player → BE inputs (output slot rejects placement via OutputSlot#mayPlace).
            if (!moveItemStackTo(stack, 0, ToolStationBlockEntity.INPUT_SLOTS, false)) {
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

    /** Input slot — permissive at the menu layer; the BE decides what produces an output. */
    private static final class InputSlot extends SlotItemHandler {
        InputSlot(IItemHandler handler, int slot, int x, int y) {
            super(handler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }

    /**
     * Output slot — rejects placement (the BE owns the contents). {@code onTake} routes to
     * the build-input vs modify-input consumer depending on which path the slot 0 input took.
     */
    private static final class OutputSlot extends SlotItemHandler {
        @Nullable
        private final ToolStationBlockEntity blockEntity;

        OutputSlot(@Nullable ToolStationBlockEntity blockEntity, IItemHandler handler, int slot, int x, int y) {
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
            // Server-only mutation: the BE's handler / build registry lookups are server state.
            // The client carries a cached BE reference but waits for the server's slot updates.
            if (blockEntity != null && !player.level().isClientSide()) {
                // Read the live slot-0 stack from the BE rather than the (stale) preview stack
                // already taken from the output slot — the consumer signature depends on
                // whether slot 0 held a built tool (modify path) or a part stack (build path).
                ItemStack slot0 = blockEntity.getHandler().getStackInSlot(0);
                if (slot0.getItem() instanceof ToolCore) {
                    blockEntity.consumeModifyInput();
                }
                else {
                    blockEntity.consumeBuildInputs();
                }
            }
        }
    }
}
