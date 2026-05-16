package slimeknights.sconstruct.port1211.smeltery.block.entity;

import java.util.Objects;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * One in-flight melt tracked by the {@link SmelteryControllerBlockEntity}. A melting operation
 * takes an item out of a melting slot, runs for a fixed number of ticks, and on completion
 * pours a {@link FluidStack} into the smeltery tank.
 *
 * <p>The {@code result} and {@code requiredTicks} come from a {@code MeltingRecipe} — that
 * recipe type and the slot-change trigger that creates a {@link MeltingProgress} when an item
 * is dropped into a melting slot land in SMTCON-120 / the recipe-impl follow-up. This class is
 * the runtime state the controller advances; it is deliberately recipe-agnostic so the ticking
 * machinery in {@link SmelteryControllerBlockEntity} is complete and testable now and the
 * recipe layer only has to <em>construct</em> these objects later.
 *
 * <p>{@link #elapsedTicks} is the only mutable field — {@link #advance()} increments it once per
 * server tick and {@link #isComplete()} reports when it has reached {@link #requiredTicks}.
 */
public final class MeltingProgress {

    private static final String TAG_SLOT = "Slot";
    private static final String TAG_ELAPSED = "Elapsed";
    private static final String TAG_REQUIRED = "Required";
    private static final String TAG_RESULT = "Result";

    private final int slot;
    private final int requiredTicks;
    private final FluidStack result;
    private int elapsedTicks;

    /**
     * @param slot          index of the melting slot whose item this melt consumes
     * @param requiredTicks server ticks the melt runs for before completing; must be positive
     * @param result        fluid poured into the tank on completion; must be non-empty
     */
    public MeltingProgress(int slot, int requiredTicks, FluidStack result) {
        Objects.requireNonNull(result, "result");
        if (slot < 0) {
            throw new IllegalArgumentException("slot must be non-negative; got " + slot);
        }
        if (requiredTicks <= 0) {
            throw new IllegalArgumentException("requiredTicks must be positive; got " + requiredTicks);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("result fluid must be non-empty");
        }
        this.slot = slot;
        this.requiredTicks = requiredTicks;
        // Defensive copy — FluidStack is mutable, so storing the caller's instance directly
        // would let them grow / shrink this melt's result after the fact.
        this.result = result.copy();
    }

    /** Melting-slot index this melt consumes from. */
    public int slot() {
        return slot;
    }

    /** Server ticks the melt runs for before {@link #isComplete()} turns true. */
    public int requiredTicks() {
        return requiredTicks;
    }

    /** Server ticks elapsed so far. */
    public int elapsedTicks() {
        return elapsedTicks;
    }

    /** Fluid to pour into the smeltery tank when this melt completes. Returns a defensive copy. */
    public FluidStack result() {
        return result.copy();
    }

    /** Advance the melt by one server tick. */
    public void advance() {
        elapsedTicks++;
    }

    /** Whether the melt has run its full duration and should pour its {@link #result()}. */
    public boolean isComplete() {
        return elapsedTicks >= requiredTicks;
    }

    /** Serialise this melt for the controller's block-entity save. */
    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_SLOT, slot);
        tag.putInt(TAG_ELAPSED, elapsedTicks);
        tag.putInt(TAG_REQUIRED, requiredTicks);
        tag.put(TAG_RESULT, result.save(provider));
        return tag;
    }

    /**
     * Reconstruct a melt from its saved tag. A tag whose {@code Result} fluid no longer parses
     * (the fluid's mod was removed) yields an empty {@link java.util.Optional} so the controller
     * drops the dead melt rather than crashing the world load.
     */
    public static java.util.Optional<MeltingProgress> load(HolderLookup.Provider provider, CompoundTag tag) {
        FluidStack result = FluidStack.parseOptional(provider, tag.getCompound(TAG_RESULT));
        if (result.isEmpty()) {
            return java.util.Optional.empty();
        }
        MeltingProgress progress = new MeltingProgress(tag.getInt(TAG_SLOT), tag.getInt(TAG_REQUIRED), result);
        progress.elapsedTicks = tag.getInt(TAG_ELAPSED);
        return java.util.Optional.of(progress);
    }

    /** Test seam: whether a tag carries the result sub-tag the loader needs. */
    static boolean hasResult(CompoundTag tag) {
        return tag.contains(TAG_RESULT, Tag.TAG_COMPOUND);
    }
}
