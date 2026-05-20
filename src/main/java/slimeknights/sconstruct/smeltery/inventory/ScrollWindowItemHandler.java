package slimeknights.sconstruct.smeltery.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * A fixed-size sliding window over a larger {@link IItemHandler} (SMTCON-216). The smeltery
 * controller's melting inventory is sized to the smeltery's interior volume, but its menu shows
 * only a {@link SmelteryControllerMenu#VISIBLE_SLOTS}-slot grid; this view exposes exactly that
 * many slots, mapping window slot {@code i} to delegate slot {@code offset + i}. Scrolling the
 * GUI just moves {@link #offset}, so the menu keeps a stable set of slot objects rather than
 * rebuilding them — {@code Slot} positions are final and cannot be moved.
 *
 * <p>A window slot whose mapped delegate index runs past the delegate's end (a partly-filled
 * final row, or an inventory smaller than the window) is inert: it reads empty and rejects every
 * insertion. {@link #isRealSlot(int)} lets the menu mark such a slot inactive.
 *
 * <p>The window implements {@link IItemHandlerModifiable} because vanilla's
 * {@link net.neoforged.neoforge.items.SlotItemHandler#set} unconditionally casts its handler to
 * that interface when applying {@code ClientboundContainerSetContentPacket} — a plain
 * {@link IItemHandler} would crash the menu sync as soon as the server pushed the initial slot
 * contents on GUI open.
 */
public final class ScrollWindowItemHandler implements IItemHandlerModifiable {

    private final IItemHandler delegate;
    private final int windowSize;
    private int offset;

    public ScrollWindowItemHandler(IItemHandler delegate, int windowSize) {
        this.delegate = delegate;
        this.windowSize = windowSize;
    }

    /** Slides the window so its first slot maps to delegate slot {@code offset}. */
    public void setOffset(int offset) {
        this.offset = Math.max(0, offset);
    }

    /** Whether window slot {@code windowIndex} currently maps to a real delegate slot. */
    public boolean isRealSlot(int windowIndex) {
        int delegateIndex = offset + windowIndex;
        return delegateIndex >= 0 && delegateIndex < delegate.getSlots();
    }

    @Override
    public int getSlots() {
        return windowSize;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return isRealSlot(slot) ? delegate.getStackInSlot(offset + slot) : ItemStack.EMPTY;
    }

    /**
     * Forwarded to the delegate when it is modifiable; window slots without a real backing slot
     * ignore the write. Vanilla's {@code SlotItemHandler.set} routes container-sync writes
     * through this method, so it must succeed in the common case even though external code never
     * calls it directly.
     */
    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (isRealSlot(slot) && delegate instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(offset + slot, stack);
        }
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isRealSlot(slot) ? delegate.insertItem(offset + slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return isRealSlot(slot) ? delegate.extractItem(offset + slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return isRealSlot(slot) ? delegate.getSlotLimit(offset + slot) : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isRealSlot(slot) && delegate.isItemValid(offset + slot, stack);
    }
}
