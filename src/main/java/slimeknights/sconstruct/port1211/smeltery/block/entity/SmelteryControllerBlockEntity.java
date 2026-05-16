package slimeknights.sconstruct.port1211.smeltery.block.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;

/**
 * Block entity for the smeltery controller (SMTCON-114) -- the brain of the multiblock. It owns
 * the smeltery's fluid tank, the item input (melting) slots, the current internal temperature,
 * and the list of in-flight {@link MeltingProgress melts} the server tick advances.
 *
 * <p><strong>Structure and sizing.</strong> The fluid tank and melting-slot inventory are
 * created at fixed initial sizes ({@link #INITIAL_TANK_CAPACITY} / {@link #INITIAL_MELTING_SLOTS})
 * so the controller is a complete, usable BE on its own. The structure-validation pass that
 * scans the seared shell and <em>resizes</em> both to the assembled smeltery's interior volume
 * lands in SMTCON-115 -- it will call {@link FluidTank#setCapacity(int)} and
 * {@link ItemStackHandler#setSize(int)}; the persisted contents survive a resize because both
 * are round-tripped verbatim here.
 *
 * <p><strong>Ticking.</strong> {@link #serverTick} is registered as the block's server-side
 * {@code BlockEntityTicker}. Each tick {@link #tickMelts()} advances every active melt by one
 * tick; a melt that reaches its required duration pours its result into the tank, clears the
 * melting slot it consumed, and is dropped from the active list. The trigger that <em>creates</em>
 * a {@link MeltingProgress} -- matching a {@code MeltingRecipe} to an item dropped into a slot --
 * lands with the recipe implementation (SMTCON-120); this BE is the machinery that runs them.
 *
 * <p><strong>Capabilities.</strong> The tank and the melting-slot inventory are exposed as
 * {@code Capabilities.FluidHandler.BLOCK} and {@code Capabilities.ItemHandler.BLOCK} (registered
 * in {@code SmelteryCapabilities}) so a seared drain can pull metal out and a seared chute can
 * push items in.
 *
 * <p>The controller GUI -- this BE implementing {@link net.minecraft.world.MenuProvider} so the
 * controller block's right-click opens a screen -- arrives in SMTCON-125.
 */
public class SmelteryControllerBlockEntity extends BlockEntity {

    /** Initial melting-slot count before SMTCON-115 resizes it to the assembled interior. */
    public static final int INITIAL_MELTING_SLOTS = 9;

    /** Initial tank capacity in mB before SMTCON-115 resizes it to the assembled interior. */
    public static final int INITIAL_TANK_CAPACITY = 9 * 2592;

    private static final String TAG_TANK = "Tank";
    private static final String TAG_MELTING_SLOTS = "MeltingSlots";
    private static final String TAG_TEMPERATURE = "Temperature";
    private static final String TAG_ACTIVE_MELTS = "ActiveMelts";

    /** The smeltery's molten-metal tank; resized to the interior volume by SMTCON-115. */
    private final FluidTank fluidTank = new FluidTank(INITIAL_TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    /**
     * Item input slots -- items dropped here are matched to melting recipes; resized by
     * SMTCON-115. A slot whose item is mid-melt is <em>reserved</em>: the overrides below reject
     * both extraction and insertion for it (see {@link #isSlotReserved(int)}) so a hopper or
     * player cannot pull the input back out — or stack onto it — while the melt is running, which
     * would otherwise let the completion in {@link #tickMelts()} duplicate or destroy items.
     */
    private final ItemStackHandler meltingSlots = new ItemStackHandler(INITIAL_MELTING_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isSlotReserved(slot) ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !isSlotReserved(slot) && super.isItemValid(slot, stack);
        }
    };

    /** Current internal temperature in kelvin; driven by fuel in a later fuel ticket. */
    private int currentTemperature;

    /** Melts currently in progress, advanced one tick at a time by {@link #tickMelts()}. */
    private final List<MeltingProgress> activeMelts = new ArrayList<>();

    public SmelteryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(SmelteryComponents.SMELTERY_CONTROLLER_BE.get(), pos, state);
    }

    /** The smeltery tank, exposed as the {@code FluidHandler.BLOCK} capability. */
    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    /** The melting-slot inventory, exposed as the {@code ItemHandler.BLOCK} capability. */
    public IItemHandler getItemHandler() {
        return meltingSlots;
    }

    /** Current internal temperature in kelvin. */
    public int getCurrentTemperature() {
        return currentTemperature;
    }

    /** Set the internal temperature; flags the chunk dirty so the new value is saved. */
    public void setCurrentTemperature(int temperature) {
        this.currentTemperature = temperature;
        setChanged();
    }

    /**
     * Read-only view of the in-flight melts -- used by the GUI and by tests. The list itself is
     * unmodifiable and {@link MeltingProgress#advance()} is package-private, so a caller outside
     * this package can read each melt's progress but cannot mutate the controller's state.
     */
    public List<MeltingProgress> getActiveMelts() {
        return Collections.unmodifiableList(activeMelts);
    }

    /**
     * Queue a new melt. The recipe layer (SMTCON-120) calls this when a slot item matches a
     * melting recipe. Queuing immediately reserves {@code melt}'s input slot — see
     * {@link #isSlotReserved(int)} — so the item cannot be removed or replaced while the melt
     * runs; the reservation lifts when the melt completes and leaves {@link #activeMelts}.
     *
     * <p>Rejects a melt whose slot is out of range or already backs another melt: a duplicate
     * slot would let two melts pour from one consumed input, and an out-of-range slot would
     * never have its input cleared on completion.
     */
    public void addMelt(MeltingProgress melt) {
        Objects.requireNonNull(melt, "melt");
        if (melt.slot() < 0 || melt.slot() >= meltingSlots.getSlots()) {
            throw new IllegalArgumentException("melt slot out of bounds: " + melt.slot());
        }
        if (isSlotReserved(melt.slot())) {
            throw new IllegalStateException("slot already has an active melt: " + melt.slot());
        }
        activeMelts.add(melt);
        setChanged();
    }

    /**
     * Whether a melting slot currently backs an in-flight melt. A reserved slot is locked
     * against extraction and insertion through the exposed item handler. Derived from
     * {@link #activeMelts} so it needs no separate persisted state — the reservation set is
     * implied by the melts themselves and is restored for free when they load.
     */
    boolean isSlotReserved(int slot) {
        for (MeltingProgress melt : activeMelts) {
            if (melt.slot() == slot) {
                return true;
            }
        }
        return false;
    }

    /**
     * Advance every active melt by one server tick. A melt that completes pours its result into
     * the tank, clears the melting slot it consumed, and is removed from the active list.
     * Extracted from {@link #serverTick} as an instance method so the ticking contract can be
     * unit-tested without a live {@link Level}.
     */
    public void tickMelts() {
        if (activeMelts.isEmpty()) {
            return;
        }
        boolean changed = false;
        Iterator<MeltingProgress> iterator = activeMelts.iterator();
        while (iterator.hasNext()) {
            MeltingProgress melt = iterator.next();
            if (!melt.isComplete()) {
                melt.advance();
                changed = true;
            }
            if (melt.isComplete()) {
                FluidStack result = melt.result();
                // Only finalise the melt once the tank can take the entire pour. If the
                // smeltery is full, fill() would partially accept and the remaining metal
                // would be lost when the slot is cleared — instead leave the completed melt in
                // place so it retries next tick (backpressure until a drain frees space).
                if (fluidTank.fill(result, IFluidHandler.FluidAction.SIMULATE) == result.getAmount()) {
                    fluidTank.fill(result, IFluidHandler.FluidAction.EXECUTE);
                    if (melt.slot() < meltingSlots.getSlots()) {
                        meltingSlots.setStackInSlot(melt.slot(), ItemStack.EMPTY);
                    }
                    iterator.remove();
                    changed = true;
                }
            }
        }
        // Skip the dirty mark when a tick moved nothing — a completed melt blocked by a full
        // tank must not churn chunk saves every tick while it waits for headroom.
        if (changed) {
            setChanged();
        }
    }

    /**
     * Server-side {@code BlockEntityTicker} entry point, registered by
     * {@code SmelteryControllerBlock#getTicker}. Delegates to {@link #tickMelts()}.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SmelteryControllerBlockEntity controller) {
        controller.tickMelts();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(TAG_TANK, fluidTank.writeToNBT(provider, new CompoundTag()));
        tag.put(TAG_MELTING_SLOTS, meltingSlots.serializeNBT(provider));
        tag.putInt(TAG_TEMPERATURE, currentTemperature);
        ListTag melts = new ListTag();
        for (MeltingProgress melt : activeMelts) {
            melts.add(melt.save(provider));
        }
        tag.put(TAG_ACTIVE_MELTS, melts);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(TAG_TANK, Tag.TAG_COMPOUND)) {
            fluidTank.readFromNBT(provider, tag.getCompound(TAG_TANK));
        }
        if (tag.contains(TAG_MELTING_SLOTS, Tag.TAG_COMPOUND)) {
            meltingSlots.deserializeNBT(provider, tag.getCompound(TAG_MELTING_SLOTS));
        }
        currentTemperature = tag.getInt(TAG_TEMPERATURE);
        activeMelts.clear();
        ListTag melts = tag.getList(TAG_ACTIVE_MELTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < melts.size(); i++) {
            // A melt whose result fluid no longer parses (mod removed) is dropped rather than
            // crashing the world load -- MeltingProgress.load returns empty for a dead fluid.
            // A melt whose slot is out of range or already taken by an earlier loaded melt is
            // likewise dropped, so corrupt save data cannot seed a duplicate or orphaned melt.
            MeltingProgress.load(provider, melts.getCompound(i)).ifPresent(melt -> {
                if (melt.slot() >= 0 && melt.slot() < meltingSlots.getSlots() && !isSlotReserved(melt.slot())) {
                    activeMelts.add(melt);
                }
            });
        }
    }
}
