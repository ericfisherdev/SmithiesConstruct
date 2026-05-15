package slimeknights.sconstruct.port1211.tools.block.entity;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.tools.PartType;
import slimeknights.sconstruct.port1211.tools.StencilTableRegistry;
import slimeknights.sconstruct.port1211.tools.inventory.StencilTableMenu;
import slimeknights.sconstruct.port1211.tools.item.PatternItem;

/**
 * Block entity backing the Stencil Table (SMTCON-91). Owns a two-slot
 * {@link ItemStackHandler} (input idx {@value #INPUT_SLOT}, output idx {@value #OUTPUT_SLOT})
 * plus a server-mutated {@link PartType} cursor — the part the typed-output stack will be
 * stamped with on every craft pull.
 *
 * <p>The input slot only accepts blank patterns (see {@link #isBlankPattern}); the output slot
 * is read-only via menu-layer slot subclassing and is re-populated by the BE on every input-
 * change or cursor-change. Pulling the output (a craft) decrements the input by 1, then the BE
 * either re-stamps the output with another typed pattern (if input still has stock) or clears
 * the output (input now empty).
 *
 * <p>Persistence round-trips the handler plus the cursor through {@link #saveAdditional} /
 * {@link #loadAdditional}; the cursor is encoded via {@link PartType#CODEC} (string form, no
 * ordinal coupling).
 *
 * <p>{@link MenuProvider} so a server-side
 * {@code player.openMenu(state.getMenuProvider(level, pos))} call from the block's
 * {@code useWithoutItem} returns the BE itself as the provider.
 */
public class StencilTableBlockEntity extends BlockEntity implements MenuProvider {

    /** Inventory slot index for the blank-pattern input. */
    public static final int INPUT_SLOT = 0;
    /** Inventory slot index for the typed-pattern output (read-only via the menu). */
    public static final int OUTPUT_SLOT = 1;
    /** Two-slot handler — input + output. */
    public static final int SLOTS = 2;

    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_PART = "Part";

    /**
     * Default selected part. Matches the first {@link PartType} enum value so a fresh-placed
     * stencil table reads naturally as "starts on the first part" — players cycle from there.
     */
    private static final PartType DEFAULT_PART = PartType.PICKHEAD;

    private final ItemStackHandler handler = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            // Input changing rebuilds the output to reflect the current cursor. Guarding on
            // slot == INPUT_SLOT keeps a re-stamp on the output (idx 1) from re-triggering the
            // refresh in a loop — refreshOutput writes to OUTPUT_SLOT through setStackInSlot,
            // which fires this callback again.
            if (slot == INPUT_SLOT) {
                refreshOutput();
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Output slot rejects every external insertion: it's owned by the BE, not the
            // player. The menu layer also enforces this at the slot level, but the handler
            // check defends against capability-API neighbours (hoppers, droppers) that bypass
            // the menu and write straight into the handler.
            if (slot == OUTPUT_SLOT) {
                return false;
            }
            // Input slot only accepts blank patterns. A typed pattern (component present) is
            // rejected so the player can't bypass the cycle button by jamming a typed stack
            // into the input.
            return isBlankPattern(stack);
        }
    };

    private PartType selectedPart = DEFAULT_PART;

    public StencilTableBlockEntity(BlockPos pos, BlockState state) {
        super(StencilTableRegistry.STENCIL_TABLE_BE.get(), pos, state);
    }

    /** Backing handler — also exposed via the {@code Capabilities.ItemHandler.BLOCK} channel. */
    public ItemStackHandler getHandler() {
        return handler;
    }

    /** Cursor — the {@link PartType} that the typed-output stack will be stamped with. */
    public PartType getSelectedPart() {
        return selectedPart;
    }

    /**
     * Server-side cursor mutator. Writes the new {@link PartType}, rebuilds the typed-output
     * stack to reflect the new part, marks the BE dirty so the next chunk flush captures the
     * change, and emits a block-update so any neighbours that observe the BE (e.g. comparators
     * — none currently, but future-proof) re-read the state.
     *
     * <p>{@link ServerLevel} parameter unused inside the body but kept on the signature as the
     * callsite contract — the cursor must only move on the server side, and asking for a
     * {@link ServerLevel} forces the caller to honour that.
     */
    public void setSelectedPart(PartType part, ServerLevel serverLevel) {
        Objects.requireNonNull(part, "part");
        Objects.requireNonNull(serverLevel, "serverLevel");
        this.selectedPart = part;
        refreshOutput();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Re-derive the typed-output slot from the current input + cursor. When the input has at
     * least one blank pattern, the output is stamped with a typed pattern carrying
     * {@link #selectedPart}; otherwise the output is cleared. Called on every input-change
     * (handler {@code onContentsChanged}) and every cursor-change ({@link #setSelectedPart}).
     */
    void refreshOutput() {
        ItemStack input = handler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            // setStackInSlot fires onContentsChanged which will recursively call refreshOutput;
            // guarded by the slot == INPUT_SLOT check in the handler callback above.
            handler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }
        if (!isBlankPattern(input)) {
            handler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }
        handler.setStackInSlot(OUTPUT_SLOT, PatternItem.makeTyped(StencilTableRegistry.PATTERN.get(), selectedPart));
    }

    /**
     * Consume one blank pattern from the input slot. Called by the menu's output-slot
     * {@code onTake} hook after the player has drained the typed pattern. After the
     * decrement, {@link #refreshOutput} repopulates the output if the input still has
     * stock, or clears it if not.
     */
    public void consumeOneInput() {
        ItemStack input = handler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }
        input.shrink(1);
        // setStackInSlot semantics: rewrites the slot's reference even when the shrunk stack
        // is now empty (handler treats an empty stack as ItemStack.EMPTY). This also fires
        // onContentsChanged which routes through refreshOutput — no explicit refresh call here.
        handler.setStackInSlot(INPUT_SLOT, input.isEmpty() ? ItemStack.EMPTY : input);
    }

    /**
     * Stencil-table blank-pattern predicate. A stack is a blank pattern iff its item is the
     * registered {@link StencilTableRegistry#BLANK_PATTERN} {@link Item} and the typed
     * {@link TinkerDataComponents#TINKER_PATTERN_PART} component is absent (the typed variant
     * uses the same item but with the component present).
     */
    public static boolean isBlankPattern(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!stack.is(StencilTableRegistry.BLANK_PATTERN.get())) {
            return false;
        }
        // Reject the typed variant: the same Item class is used for both the blank and the
        // typed pattern (differentiated only by the component), so the BLANK_PATTERN id check
        // alone is not sufficient.
        return PatternItem.getPart(stack).isEmpty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(TAG_INVENTORY, handler.serializeNBT(provider));
        // PartType.CODEC is StringRepresentable-backed so the serialised form is the part's
        // id() string (e.g. "pickhead") — survives reordering of PartType.values().
        Tag partTag = PartType.CODEC.encodeStart(NbtOps.INSTANCE, selectedPart).getOrThrow();
        tag.put(TAG_PART, partTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(TAG_INVENTORY, Tag.TAG_COMPOUND)) {
            handler.deserializeNBT(provider, tag.getCompound(TAG_INVENTORY));
        }
        if (tag.contains(TAG_PART)) {
            // Decoder uses the parse result's value when present, falls back to the default
            // otherwise — a malformed value (e.g. an unknown id from a downgrade) defaults
            // to PICKHEAD rather than crashing chunk load.
            Optional<PartType> parsed = PartType.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_PART)).result();
            this.selectedPart = parsed.orElse(DEFAULT_PART);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.sconstruct.stencil_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        Objects.requireNonNull(playerInventory, "playerInventory");
        Objects.requireNonNull(player, "player");
        return new StencilTableMenu(containerId, playerInventory, this);
    }
}
