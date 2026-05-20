package slimeknights.sconstruct.smeltery.block.entity.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Multi-fluid smeltery tank (SMTCON-220) — the molten-metal reservoir the controller exposes as
 * its {@code FluidHandler.BLOCK} capability. Unlike NeoForge's {@code FluidTank} (single fluid
 * type per tank), this storage holds an ordered {@link List} of {@link FluidStack} entries so
 * molten iron, gold, and tin can coexist in one bowl — the prerequisite for alloying.
 *
 * <p>The tank sums all entries against a single {@link #capacity} cap; an incoming fill is merged
 * into an existing entry of the same fluid (and components) or appended as a new entry, then
 * clamped against whatever headroom remains across the whole list. Drains take from the head of
 * the list ({@link #drain(int, FluidAction)}) or from the matching entry
 * ({@link #drain(FluidStack, FluidAction)}). The head-drain matches upstream Tinkers' Construct
 * behaviour — a drain spout pulls whatever floats at the top of the bowl.
 *
 * <p>Mutations notify the owner via the {@link #onChange} callback so the controller can mark its
 * chunk dirty and schedule a fluid-update payload to clients.
 */
public final class SmelteryFluidTank implements IFluidHandler {

    /** NBT key for the persisted fluid list. */
    private static final String TAG_FLUIDS = "Fluids";

    /** NBT key for the persisted total capacity. */
    private static final String TAG_CAPACITY = "Capacity";

    private final List<FluidStack> fluids = new ArrayList<>();
    private final Runnable onChange;
    private int capacity;

    /**
     * @param capacity initial capacity in millibuckets — resizable later via {@link #setCapacity}
     * @param onChange callback fired after any state mutation so the owner can mark itself dirty
     */
    public SmelteryFluidTank(int capacity, Runnable onChange) {
        // Match setCapacity's invariant up front so the tank cannot start with a negative
        // pool, even if a caller passes a malformed initial value.
        this.capacity = Math.max(0, capacity);
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    /** The maximum total millibuckets the tank can hold across all fluid entries. */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Resizes the tank capacity. Existing fluids are kept; if the new capacity is smaller than
     * the current total contents, no fluid is removed here — that's the caller's responsibility
     * (e.g. via {@code releaseTankContents} during disassembly).
     */
    public void setCapacity(int newCapacity) {
        if (newCapacity != capacity) {
            capacity = Math.max(0, newCapacity);
            onChange.run();
        }
    }

    /** An unmodifiable snapshot of the fluid entries, in insertion order. */
    public List<FluidStack> getFluids() {
        return Collections.unmodifiableList(fluids);
    }

    /**
     * Total millibuckets currently stored across every fluid entry. Accumulates into a
     * {@code long} first and clamps to {@link Integer#MAX_VALUE} so a tank holding many
     * near-MAX entries cannot overflow the headroom calculation in {@link #fill}.
     */
    public int getTotalAmount() {
        long sum = 0L;
        for (FluidStack stack : fluids) {
            sum += stack.getAmount();
        }
        return (int) Math.min(sum, Integer.MAX_VALUE);
    }

    /**
     * Whether the tank holds nothing. Equivalent to {@code getTotalAmount() == 0} because the
     * tank invariant — maintained by {@link #fill}, {@link #drain}, and {@link #setFluids} —
     * guarantees the list never contains a zero-amount entry; once an entry reaches zero it is
     * removed, and zero-amount stacks are filtered out on the way in.
     */
    public boolean isEmpty() {
        return fluids.isEmpty();
    }

    /**
     * Replaces the entire fluid list with copies of {@code newFluids}. Empty entries are dropped;
     * the change fires {@link #onChange} exactly once. Used by the client-side payload handler
     * to mirror server state without round-tripping individual fills.
     */
    public void setFluids(List<FluidStack> newFluids) {
        fluids.clear();
        for (FluidStack stack : newFluids) {
            if (!stack.isEmpty()) {
                fluids.add(stack.copy());
            }
        }
        onChange.run();
    }

    @Override
    public int getTanks() {
        // Vanilla pipes and JEI expect at least one tank slot, even when empty — report one
        // virtual slot in that case so {@code getFluidInTank(0)} stays valid.
        return Math.max(1, fluids.size());
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank >= 0 && tank < fluids.size() ? fluids.get(tank) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        // The tank shares one pool across every fluid, so each virtual slot reports the same
        // total capacity rather than a fluid-specific subdivision.
        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || capacity <= 0) {
            return 0;
        }
        int headroom = capacity - getTotalAmount();
        if (headroom <= 0) {
            return 0;
        }
        int toFill = Math.min(resource.getAmount(), headroom);
        if (!action.execute()) {
            return toFill;
        }
        for (FluidStack existing : fluids) {
            if (FluidStack.isSameFluidSameComponents(existing, resource)) {
                existing.grow(toFill);
                onChange.run();
                return toFill;
            }
        }
        FluidStack added = resource.copy();
        added.setAmount(toFill);
        fluids.add(added);
        onChange.run();
        return toFill;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack entry = fluids.get(i);
            if (FluidStack.isSameFluidSameComponents(entry, resource)) {
                int draining = Math.min(entry.getAmount(), resource.getAmount());
                FluidStack drained = entry.copy();
                drained.setAmount(draining);
                if (action.execute()) {
                    entry.shrink(draining);
                    if (entry.isEmpty()) {
                        fluids.remove(i);
                    }
                    onChange.run();
                }
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || fluids.isEmpty()) {
            return FluidStack.EMPTY;
        }
        // Head-drain: a typed-amount drain on the first fluid in the list, matching upstream
        // Tinkers' Construct behaviour — a drain spout pulls whatever's at the top of the bowl.
        FluidStack head = fluids.get(0);
        FluidStack request = head.copyWithAmount(maxDrain);
        return drain(request, action);
    }

    /** Serialises the fluid list and capacity into {@code tag} for disk save. */
    public CompoundTag writeToNBT(HolderLookup.Provider provider, CompoundTag tag) {
        ListTag list = new ListTag();
        for (FluidStack stack : fluids) {
            list.add(stack.save(provider, new CompoundTag()));
        }
        tag.put(TAG_FLUIDS, list);
        tag.putInt(TAG_CAPACITY, capacity);
        return tag;
    }

    /**
     * Restores the fluid list and capacity from {@code tag}. Entries that fail to parse (mod
     * removal, format change) are silently dropped rather than corrupting the load — a smeltery
     * with a dead fluid loses that fluid, but the rest of its contents survive.
     */
    public void readFromNBT(HolderLookup.Provider provider, CompoundTag tag) {
        fluids.clear();
        ListTag list = tag.getList(TAG_FLUIDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            FluidStack stack = FluidStack.parseOptional(provider, list.getCompound(i));
            if (!stack.isEmpty()) {
                fluids.add(stack);
            }
        }
        // A missing TAG_CAPACITY can occur on a corrupt save or a forward-rev tag schema; fall
        // back to a zero pool rather than reusing the constructor value, so the controller's
        // bindStructure path is the single place that grows the tank back to its bowl size.
        capacity = tag.contains(TAG_CAPACITY, Tag.TAG_INT) ? Math.max(0, tag.getInt(TAG_CAPACITY)) : 0;
    }
}
