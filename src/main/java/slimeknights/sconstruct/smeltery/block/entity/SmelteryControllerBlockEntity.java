package slimeknights.sconstruct.smeltery.block.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import slimeknights.sconstruct.common.SmithiesParticles;
import slimeknights.sconstruct.smeltery.SmelteryComponents;
import slimeknights.sconstruct.smeltery.SmelteryFuelSource;
import slimeknights.sconstruct.smeltery.block.SmelteryComponentBlock;
import slimeknights.sconstruct.smeltery.block.SmelteryControllerBlock;
import slimeknights.sconstruct.smeltery.block.entity.inventory.SmelteryFluidTank;
import slimeknights.sconstruct.smeltery.block.entity.module.SmelteryFuelModule;
import slimeknights.sconstruct.smeltery.inventory.SmelteryControllerMenu;
import slimeknights.sconstruct.smeltery.multiblock.ComponentType;
import slimeknights.sconstruct.smeltery.multiblock.SmelteryStructure;
import slimeknights.sconstruct.smeltery.multiblock.SmelteryStructureValidator;
import slimeknights.sconstruct.smeltery.network.SmelteryFluidUpdatePayload;
import slimeknights.sconstruct.smeltery.network.SmelteryFuelUpdatePayload;
import slimeknights.sconstruct.smeltery.network.SmelteryMeltingUpdatePayload;
import slimeknights.sconstruct.smeltery.network.SmelteryStructureUpdatePayload;
import slimeknights.sconstruct.smeltery.recipe.MeltingRecipe;
import slimeknights.sconstruct.smeltery.recipe.SmelteryRecipes;

/**
 * Block entity for the smeltery controller (SMTCON-114) -- the brain of the multiblock. It owns
 * the smeltery's fluid tank, the item input (melting) slots, the current internal temperature,
 * and the list of in-flight {@link MeltingProgress melts} the server tick advances.
 *
 * <p><strong>Structure and sizing.</strong> The fluid tank and melting-slot inventory are
 * created at fixed initial sizes ({@link #INITIAL_TANK_CAPACITY} / {@link #INITIAL_MELTING_SLOTS})
 * so the controller is a complete, usable BE on its own. On assembly {@link #bindStructure}
 * resizes the tank capacity (SMTCON-215) and the melting-slot count (SMTCON-216) to the bowl
 * volume; {@link #unbindStructure} restores both initial sizes.
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
public class SmelteryControllerBlockEntity extends BlockEntity implements MenuProvider {

    /** Melting-slot count before {@link #bindStructure} resizes it to the assembled interior. */
    public static final int INITIAL_MELTING_SLOTS = 9;

    /** Molten-metal capacity in millibuckets contributed by each interior block of the bowl. */
    public static final int MB_PER_INTERIOR_CELL = 2592;

    /** Tank capacity in mB before {@link #bindStructure} resizes it to the assembled interior. */
    public static final int INITIAL_TANK_CAPACITY = INITIAL_MELTING_SLOTS * MB_PER_INTERIOR_CELL;

    /**
     * Wall blocks each {@code fuelDrawPerTick} step represents (SMTCON-217). Matches upstream
     * Tinkers' Construct 1.20.1's {@code BLOCKS_PER_FUEL}: at 15 walls per step a small smeltery
     * burns at the base rate, a max-size smeltery burns several times faster.
     */
    private static final int WALLS_PER_FUEL_DRAW_STEP = 15;

    private static final String TAG_TANK = "Tank";
    private static final String TAG_MELTING_SLOTS = "MeltingSlots";
    private static final String TAG_TEMPERATURE = "Temperature";
    private static final String TAG_ACTIVE_MELTS = "ActiveMelts";
    private static final String TAG_FUEL_MODULE = "FuelModule";
    private static final String TAG_STRUCTURE = "Structure";

    /** Update-tag key for the target temperature — sync-only, not part of the saved state. */
    private static final String TAG_TARGET_TEMPERATURE = "TargetTemperature";

    /** Update-tag key for the interior render bounds — sync-only, not part of the saved state. */
    private static final String TAG_RENDER_BOUNDS = "RenderBounds";

    /** Number of integers in the {@link #TAG_RENDER_BOUNDS} array — the six box corners. */
    private static final int RENDER_BOUNDS_LENGTH = 6;

    /** Internal temperature in kelvin above which the smeltery emits ambient smoke. */
    private static final int SMOKE_TEMPERATURE_THRESHOLD = 1000;

    /** Server-tick interval between ambient smoke emissions — keeps the effect subtle. */
    private static final int SMOKE_EMIT_INTERVAL = 10;

    /**
     * The smeltery's molten-metal tank (SMTCON-220) — a multi-fluid reservoir so iron, gold, and
     * tin can coexist in the bowl for alloying. {@link #bindStructure} resizes its capacity to
     * the assembled bowl volume.
     */
    private final SmelteryFluidTank fluidTank = new SmelteryFluidTank(INITIAL_TANK_CAPACITY, this::setChanged);

    /**
     * Item input slots -- items dropped here are matched to melting recipes; {@link #bindStructure}
     * resizes this inventory to the interior volume. A slot whose item is mid-melt is
     * <em>reserved</em>: the overrides below reject
     * both extraction and insertion for it (see {@link #isSlotReserved(int)}) so a hopper or
     * player cannot pull the input back out — or stack onto it — while the melt is running, which
     * would otherwise let the completion in {@link #tickMelts()} duplicate or destroy items.
     */
    private final ItemStackHandler meltingSlots = new ItemStackHandler(INITIAL_MELTING_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            meltingItemsDirty = true;
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

    /** Current internal temperature in kelvin, set each tick from the active fuel source. */
    private int currentTemperature;

    /** Temperature in kelvin the active fuel source can sustain — the heat the smeltery heads toward. */
    private int targetTemperature;

    /** Melts currently in progress, advanced one tick at a time by {@link #tickMelts()}. */
    private final List<MeltingProgress> activeMelts = new ArrayList<>();

    /**
     * Slot-indexed lookup over {@link #activeMelts} (SMTCON-225) — every entry in {@code
     * activeMelts} also lives here keyed by its slot, so {@link #getMeltProgress} and
     * {@link #isSlotReserved} are O(1) instead of O(n). Mutations to {@code activeMelts} must
     * mirror through this map in lockstep; the invariant is asserted by the size equality of
     * the two collections at every commit point.
     */
    private final Map<Integer, MeltingProgress> meltsBySlot = new HashMap<>();

    /**
     * Burner module (SMTCON-222 / SMTCON-217) — owns the per-tick fuel-draw policy so the
     * single-block melter and alloy furnace variants can reuse the same burn logic without
     * copy-pasting it. Wired to resolve the hottest bound tank via {@link #hottestFuelSource}
     * and to emit temperature through {@link #setTemperature}. The per-tick burn rate scales
     * with shell size (set in {@link #bindStructure}) and the burn cycle is independent of
     * active melt count — a smeltery full of items costs no more to heat than one with a
     * single melt, matching every Tinkers' Construct release since 1.16.
     */
    private final SmelteryFuelModule fuelModule = new SmelteryFuelModule(this::hottestFuelSource, this::setTemperature);

    /**
     * The assembled smeltery's interior bounding box as last synced to clients (SMTCON-124).
     * Server-side this mirrors {@code structure.map(SmelteryStructure::bounds)}; client-side it
     * is set straight from {@code SmelteryStructureUpdatePayload} and read by the renderer.
     */
    private Optional<BoundingBox> renderBounds = Optional.empty();

    /** Temperature last pushed to chunk trackers — a sentinel that forces the first sync. */
    private int lastSyncedTemperature = Integer.MIN_VALUE;

    /** Target temperature last pushed to chunk trackers — a sentinel that forces the first sync. */
    private int lastSyncedTargetTemperature = Integer.MIN_VALUE;

    /** Tank contents last pushed to chunk trackers, so an unchanged tank is not re-synced. */
    private List<FluidStack> lastSyncedFluids = List.of();

    /** Interior bounds last pushed to chunk trackers, so an unchanged structure is not re-synced. */
    private Optional<BoundingBox> lastSyncedBounds = Optional.empty();

    /** Melting-slot contents last pushed to chunk trackers, so unchanged slots are not re-synced. */
    private List<ItemStack> lastSyncedMeltingItems = List.of();

    /**
     * Set whenever a melting slot mutates; gates the per-tick snapshot in {@link #syncToTrackers}
     * so an idle smeltery does not copy the whole inventory every tick. Starts {@code true} so
     * the first sync always runs.
     */
    private boolean meltingItemsDirty = true;

    /**
     * The validated multiblock shape this controller currently drives, or
     * {@link Optional#empty()} when the controller is not part of an assembled smeltery. Not
     * persisted — it is re-derived by {@link #tryAssemble()} on the first server tick after a
     * world load (see {@link #loadAdditional}), so the in-memory structure can never drift from
     * the blocks actually in the world.
     */
    private Optional<SmelteryStructure> structure = Optional.empty();

    /**
     * Whether the controller must re-run {@link #tryAssemble()} on its next server tick. Set on
     * block-entity construction and after every world load so a freshly placed or reloaded
     * controller assembles itself, and set again by {@link #invalidate()} when a nearby seared
     * or component block is broken (SMTCON-117).
     */
    private boolean needsValidation = true;

    /**
     * Whether the controller restored its multiblock from NBT (SMTCON-218) and still owes its
     * components a controller-stamp refresh on the first server tick. The disk path bypasses
     * {@link #bindStructure}, so component BEs in newly-loaded chunks may not yet know which
     * controller commands them; this flag triggers a stamp pass once chunks are reachable.
     */
    private boolean needsComponentRestamp;

    /**
     * Rolling cursor into the assembled structure's interior cells — incremented every time the
     * streaming interior validity check (SMTCON-219) sweeps a cell, wrapping modulo
     * {@link SmelteryStructure#bowlVolume()}. Reset to {@code 0} on {@link #bindStructure} and
     * {@link #unbindStructure} so a re-bind always starts from the floor's first cell; not
     * persisted because the sweep order does not affect correctness.
     */
    private int interiorCheckCursor;

    /**
     * Ticks between two consecutive cells of the streaming interior validity check (SMTCON-219).
     * Matches upstream Tinkers' Construct 1.18.2 — a sweep of one cell per four server ticks
     * gives a 100-cell interior a 20-second cycle for negligible CPU cost, and catches a
     * piston-placed or worldgen-replaced interior block within that window without an event hook.
     */
    private static final int INTERIOR_CHECK_INTERVAL = 4;

    /**
     * Ticks between consecutive expansion-poll checks (SMTCON-228). 200 ticks = 10 seconds —
     * frequent enough that a player who stacks a new wall ring on a live smeltery sees it absorb
     * within a few seconds, infrequent enough that the per-tick cost is negligible. Matches
     * upstream Tinkers' Construct 1.18.2's expansion cadence.
     */
    private static final int EXPANSION_POLL_INTERVAL = 200;

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
        setTemperature(temperature);
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
     * The melt progress of the given melting slot as a percentage in {@code [0, 100]}, or
     * {@code 0} when no melt is running in that slot. Used by the controller menu (SMTCON-125)
     * to drive the per-slot progress bars.
     */
    public int getMeltProgress(int slot) {
        // O(1) lookup via the SMTCON-225 slot index — the underlying list is still walked once
        // per server tick by tickMelts, but the menu's per-frame query no longer pays that cost.
        MeltingProgress melt = meltsBySlot.get(slot);
        return melt == null ? 0 : Math.min(100, melt.elapsedTicks() * 100 / melt.requiredTicks());
    }

    /** The smeltery controller's menu title. */
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.sconstruct.smeltery");
    }

    /** Opens the {@link SmelteryControllerMenu} for {@code player} (SMTCON-125). */
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        Objects.requireNonNull(playerInventory, "playerInventory");
        Objects.requireNonNull(player, "player");
        return new SmelteryControllerMenu(containerId, playerInventory, this);
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
        meltsBySlot.put(melt.slot(), melt);
        assertMeltIndexInvariant();
        setChanged();
    }

    /**
     * Runtime check that the {@link #activeMelts} list and the {@link #meltsBySlot} index agree
     * on size (SMTCON-225). The two collections are mutated through {@link #addMelt},
     * {@link #tickMelts}'s completion path, {@link #resizeMeltingSlots}, and
     * {@link #loadAdditional}; a missed mirror on any of those would let the index silently lie
     * to {@link #isSlotReserved} and {@link #getMeltProgress}. Failing fast at the commit point
     * makes the desync visible at the moment it happens rather than as a phantom reservation
     * later. Throws {@link IllegalStateException} on mismatch so callers cannot ignore it.
     */
    private void assertMeltIndexInvariant() {
        if (activeMelts.size() != meltsBySlot.size()) {
            throw new IllegalStateException("activeMelts (" + activeMelts.size() + ") and meltsBySlot (" + meltsBySlot.size() + ") out of sync");
        }
    }

    /**
     * Whether a melting slot currently backs an in-flight melt. A reserved slot is locked
     * against extraction and insertion through the exposed item handler. Derived from
     * {@link #activeMelts} so it needs no separate persisted state — the reservation set is
     * implied by the melts themselves and is restored for free when they load.
     */
    boolean isSlotReserved(int slot) {
        // O(1) via the slot index (SMTCON-225); the index is kept in lockstep with activeMelts
        // through addMelt / tickMelts / resizeMeltingSlots / loadAdditional.
        return meltsBySlot.containsKey(slot);
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
                        // Consume exactly the one item this melt processed — shrinking the stack
                        // rather than emptying the slot. A slot stacked deeper keeps its
                        // remaining items, which startMelts picks up as the next melt.
                        ItemStack input = meltingSlots.getStackInSlot(melt.slot());
                        if (!input.isEmpty()) {
                            ItemStack remaining = input.copy();
                            remaining.shrink(1);
                            meltingSlots.setStackInSlot(melt.slot(), remaining);
                        }
                    }
                    iterator.remove();
                    meltsBySlot.remove(melt.slot(), melt);
                    assertMeltIndexInvariant();
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
     * {@code SmelteryControllerBlock#getTicker}. Re-validates the multiblock when flagged, draws
     * fuel from the bound tanks, then advances the active melts — but only while fuel is
     * available, so an out-of-fuel smeltery pauses its melts rather than running them cold.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SmelteryControllerBlockEntity controller) {
        if (controller.needsValidation) {
            controller.tryAssemble();
        }
        else if (controller.needsComponentRestamp) {
            controller.restampRestoredComponents();
        }
        // Streaming interior validity check (SMTCON-219): one cell per four ticks, rotating
        // through the bowl. Catches placements the SMTCON-117 break-event listener misses — pistons,
        // water/lava flows, foreign-mod block setters — without paying the cost of a full rescan.
        if (controller.isAssembled() && level.getGameTime() % INTERIOR_CHECK_INTERVAL == 0L) {
            controller.streamInteriorCheck();
        }
        // Expansion poll (SMTCON-228): once every 200 ticks, ask whether a new wall ring has
        // closed above the current top; flag a re-validation so the bowl absorbs it without the
        // player having to break and re-place the controller.
        if (controller.isAssembled() && level.getGameTime() % EXPANSION_POLL_INTERVAL == 0L) {
            controller.pollExpansion();
        }
        controller.startMelts();
        controller.tickSmeltery();
        controller.syncToTrackers();
        controller.emitSmoke(level);
    }

    /**
     * Classifies the interior cell the cursor currently points at and, if the cell is no longer a
     * valid {@link SmelteryStructureValidator.BlockRole#INTERIOR}, flags the controller to
     * re-validate on its next tick. Advances the cursor every sweep, whether or not the cell was
     * valid, so a persistently-bad cell does not stop the rest of the sweep.
     */
    /**
     * Polls whether the assembled smeltery could now extend its shell one wall ring upward
     * (SMTCON-228) and, when {@link SmelteryStructureValidator#canExpand} agrees, flags a
     * re-validation so the next tick's {@code tryAssemble} pass absorbs the new layer. The
     * check is cheap (one ring walk plus one interior-layer scan, both bounded by the v1
     * {@link SmelteryStructureValidator#MAX_INTERIOR_SIZE}) and runs only once every
     * {@link #EXPANSION_POLL_INTERVAL} ticks, so the per-tick cost stays negligible.
     */
    private void pollExpansion() {
        if (level == null || !structure.isPresent()) {
            return;
        }
        if (SmelteryStructureValidator.canExpand(level, structure.get())) {
            needsValidation = true;
        }
    }

    private void streamInteriorCheck() {
        if (level == null || !structure.isPresent()) {
            return;
        }
        SmelteryStructure assembled = structure.get();
        // The cursor can outrun the bowl after a resize; wrap defensively before indexing so the
        // first sweep after a shrink does not throw an IndexOutOfBoundsException.
        interiorCheckCursor = Math.floorMod(interiorCheckCursor, assembled.bowlVolume());
        BlockPos cell = assembled.interiorCell(interiorCheckCursor);
        interiorCheckCursor = (interiorCheckCursor + 1) % assembled.bowlVolume();
        SmelteryStructureValidator.BlockRole role = SmelteryStructureValidator.classifierFor(level).classify(cell);
        if (role != SmelteryStructureValidator.BlockRole.INTERIOR) {
            needsValidation = true;
        }
    }

    /**
     * Re-stamps the controller's position onto every component of an NBT-restored structure
     * (SMTCON-218). Component BEs persist their own {@code controllerPos}, so a stamp is normally
     * unnecessary — but a chunk edit, a partial save, or a component BE loaded from a different
     * chunk that lost its tag is harmless to refresh defensively. Runs once after the structure
     * is restored, on the first server tick when the world is fully attached and the component
     * positions are reachable; the flag is cleared whether or not every component resolved.
     */
    private void restampRestoredComponents() {
        needsComponentRestamp = false;
        if (level == null || !isAssembled()) {
            return;
        }
        for (BlockPos componentPos : structure.get().components().keySet()) {
            if (level.getBlockEntity(componentPos) instanceof SmelteryComponentBlockEntity component) {
                component.setControllerPos(getBlockPos());
            }
            // Also re-apply the IN_STRUCTURE blockstate property (SMTCON-226). A pre-SMTCON-226
            // save will load with in_structure=false and bypass bindStructure, so JEI/Jade and
            // any block-render layer keyed on the property would read stale data without this
            // refresh.
            setInStructureProperty(componentPos, true);
        }
    }

    /**
     * Scans the melting slots and queues a {@link MeltingProgress} for every slot whose item
     * matches a {@link MeltingRecipe}, as long as the smeltery has a working fuel source. This
     * is the trigger that turns a loaded melting slot into an in-flight melt (SMTCON-213) —
     * without it the controller holds items but never melts them.
     *
     * <p>Server-side, assembled smelteries only. A slot already backing a melt is skipped via
     * {@link #isSlotReserved(int)}, so an item melts once and is not re-queued every tick.
     * Following 1.12 Tinkers' Construct, any fuel melts any recipe — the recipe temperature is
     * not a gate. A melt that outpaces its fuel is paused by {@link #drawFuel()}, not here.
     */
    private void startMelts() {
        if (level == null || level.isClientSide() || !isAssembled() || hottestFuelSource() == null) {
            return;
        }
        for (int slot = 0; slot < meltingSlots.getSlots(); slot++) {
            if (isSlotReserved(slot)) {
                continue;
            }
            ItemStack stack = meltingSlots.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Optional<RecipeHolder<MeltingRecipe>> recipe = level.getRecipeManager().getRecipeFor(SmelteryRecipes.MELTING_TYPE.get(), new SingleRecipeInput(stack), level);
            if (recipe.isPresent()) {
                MeltingRecipe melting = recipe.get().value();
                addMelt(new MeltingProgress(slot, melting.time(), melting.output()));
            }
        }
    }

    /**
     * Emits a couple of ambient {@code smeltery_smoke} particles just above the controller block
     * while it is assembled and running hot. Throttled to once every {@link #SMOKE_EMIT_INTERVAL}
     * ticks and kept to one or two particles per emission so the effect stays subtle.
     *
     * <p>Uses {@link ServerLevel#sendParticles} — server-safe and broadcast to tracking clients —
     * rather than {@code Level#addParticle}, which is client-only and would crash a dedicated
     * server when called from this server-tick path.
     */
    private void emitSmoke(Level level) {
        if (!isAssembled() || currentTemperature <= SMOKE_TEMPERATURE_THRESHOLD || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = getBlockPos();
        if (!ParticleEmission.shouldEmitThisTick(pos, level.getGameTime(), SMOKE_EMIT_INTERVAL)) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;
        // Small XZ spread, no inbound velocity — the particle supplies its own slow upward drift.
        serverLevel.sendParticles(SmithiesParticles.SMELTERY_SMOKE.get(), x, y, z, 1, 0.15D, 0.0D, 0.15D, 0.0D);
    }

    /**
     * Pushes any changed smeltery state to clients tracking the controller's chunk (SMTCON-124).
     * Each of the three updates — heat, tank contents, and assembled shape — is sent only when
     * its value differs from what was last synced, so an idle smeltery emits no packets and a
     * change is delivered at most once per tick.
     */
    // ServerLevel is AutoCloseable in the type system, but the world is owned by the server
    // lifecycle, not by this block entity — PMD's CloseResource heuristic does not model that.
    @SuppressWarnings("PMD.CloseResource")
    private void syncToTrackers() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ChunkPos chunk = new ChunkPos(getBlockPos());
        if (currentTemperature != lastSyncedTemperature || targetTemperature != lastSyncedTargetTemperature) {
            lastSyncedTemperature = currentTemperature;
            lastSyncedTargetTemperature = targetTemperature;
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk, new SmelteryFuelUpdatePayload(getBlockPos(), currentTemperature, targetTemperature));
        }
        List<FluidStack> fluids = fluidTank.getFluids();
        if (!fluidListsMatch(fluids, lastSyncedFluids)) {
            // A single deep-copy serves both the snapshot-for-equality-checks and the network
            // payload — neither callsite mutates the list downstream, so sharing the copy is
            // safe and spares an extra allocation per change.
            List<FluidStack> snapshot = copyFluidList(fluids);
            lastSyncedFluids = snapshot;
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk, new SmelteryFluidUpdatePayload(getBlockPos(), snapshot));
        }
        Optional<BoundingBox> bounds = structure.map(SmelteryStructure::bounds);
        if (!bounds.equals(lastSyncedBounds)) {
            lastSyncedBounds = bounds;
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk, new SmelteryStructureUpdatePayload(getBlockPos(), bounds));
        }
        if (meltingItemsDirty) {
            meltingItemsDirty = false;
            List<ItemStack> meltingItems = meltingSlotContents();
            if (!meltingItemsMatch(meltingItems, lastSyncedMeltingItems)) {
                lastSyncedMeltingItems = meltingItems;
                PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk, new SmelteryMeltingUpdatePayload(getBlockPos(), meltingItems));
            }
        }
    }

    /** A slot-indexed snapshot of the melting-slot contents — each stack copied so it is immutable. */
    private List<ItemStack> meltingSlotContents() {
        List<ItemStack> contents = new ArrayList<>(meltingSlots.getSlots());
        for (int slot = 0; slot < meltingSlots.getSlots(); slot++) {
            contents.add(meltingSlots.getStackInSlot(slot).copy());
        }
        return contents;
    }

    /** Defensive deep copy of a fluid list — every entry is {@link FluidStack#copy() copied} so the snapshot does not alias the live tank. */
    private static List<FluidStack> copyFluidList(List<FluidStack> source) {
        List<FluidStack> copy = new ArrayList<>(source.size());
        for (FluidStack stack : source) {
            copy.add(stack.copy());
        }
        return copy;
    }

    /** Whether two fluid-list snapshots hold the same stacks in the same order. */
    private static boolean fluidListsMatch(List<FluidStack> a, List<FluidStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!FluidStack.matches(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Whether two slot-indexed melting-item snapshots hold the same stacks in the same order. */
    private static boolean meltingItemsMatch(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.matches(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies a {@code SmelteryFluidUpdatePayload} to this client-side controller — the tank is
     * set to the synced contents so the next frame renders the smeltery's fill.
     */
    public void applyFluidUpdate(List<FluidStack> contents) {
        fluidTank.setFluids(contents);
    }

    /**
     * Applies a {@code SmelteryMeltingUpdatePayload} to this client-side controller — every
     * melting slot is set to the synced stack so the renderer draws the items being melted. A
     * slot past the end of the payload is cleared, so a shrunk update leaves no stale stack
     * rendering as a phantom melt; each inbound stack is copied to avoid aliasing the payload.
     */
    public void applyMeltingUpdate(List<ItemStack> items) {
        for (int slot = 0; slot < meltingSlots.getSlots(); slot++) {
            meltingSlots.setStackInSlot(slot, slot < items.size() ? items.get(slot).copy() : ItemStack.EMPTY);
        }
    }

    /** Applies a {@code SmelteryFuelUpdatePayload} to this client-side controller's heat gauge. */
    public void applyFuelUpdate(int current, int target) {
        currentTemperature = current;
        targetTemperature = target;
    }

    /**
     * Applies a {@code SmelteryStructureUpdatePayload} to this client-side controller. Sets the
     * render bounds, and re-derives the tank capacity from the interior volume — the capacity is
     * absent from the tank's synced NBT, so without this the GUI gauge would size every fill
     * against the unassembled default.
     */
    public void applyStructureUpdate(Optional<BoundingBox> bounds) {
        renderBounds = bounds;
        int volume = bounds.map(box -> box.getXSpan() * box.getYSpan() * box.getZSpan()).orElse(INITIAL_MELTING_SLOTS);
        fluidTank.setCapacity(volume * MB_PER_INTERIOR_CELL);
    }

    /** The temperature in kelvin the active fuel source can sustain. */
    public int getTargetTemperature() {
        return targetTemperature;
    }

    /** The assembled smeltery's interior bounding box, as known to this side; empty when unassembled. */
    public Optional<BoundingBox> getRenderBounds() {
        return renderBounds;
    }

    /**
     * Drives one server tick of the smeltery: with no melts pending the controller idles cold;
     * otherwise it draws fuel from the hottest bound tank and advances the melts, or — if no
     * tank can provide fuel — leaves the melts paused until fuel returns.
     */
    private void tickSmeltery() {
        if (activeMelts.isEmpty()) {
            setTemperature(0);
            return;
        }
        if (drawFuel()) {
            tickMelts();
        }
    }

    /**
     * Wraps {@link SmelteryFuelModule#tickBurn} for one server tick of fuel-draw work — the
     * module decides how much to consume against the active melt count and reports back the
     * resulting temperature via {@link #setTemperature}. Kept as a method on the controller so
     * the call site in {@link #tickSmeltery} stays self-documenting.
     */
    private boolean drawFuel() {
        return fuelModule.tickBurn();
    }

    /**
     * The hottest seared tank bound to this structure that can currently provide fuel, or
     * {@code null} when no tank can. Shared by {@link #drawFuel()}, which consumes from it, and
     * {@link #peekFuelTemperature()}, which only reads its temperature.
     */
    private SmelteryFuelSource hottestFuelSource() {
        SmelteryFuelSource hottest = null;
        if (level != null && structure.isPresent()) {
            for (Map.Entry<BlockPos, ComponentType> component : structure.get().components().entrySet()) {
                if (component.getValue() == ComponentType.TANK && level.getBlockEntity(component.getKey()) instanceof SmelteryFuelSource fuel && fuel.canProvideFuel()
                        && (hottest == null || fuel.getTemperature() > hottest.getTemperature())) {
                    hottest = fuel;
                }
            }
        }
        return hottest;
    }

    /**
     * Updates the internal temperature, marking the chunk dirty only when the value changes. The
     * current and target temperatures move together — the smeltery has no gradual heat-up curve
     * yet — but are kept as distinct fields so {@code SmelteryFuelUpdatePayload} carries both.
     */
    private void setTemperature(int temperature) {
        // Guard on both fields: after a reload currentTemperature loads from NBT but
        // targetTemperature defaults to 0, so a same-current-value tick must still refresh it.
        if (currentTemperature != temperature || targetTemperature != temperature) {
            currentTemperature = temperature;
            targetTemperature = temperature;
            setChanged();
        }
    }

    /** Whether this controller currently drives a validated multiblock smeltery. */
    public boolean isAssembled() {
        return structure.isPresent();
    }

    /**
     * The validated multiblock shape this controller drives, or {@link Optional#empty()} when
     * the controller is loose. Read-only — callers cannot mutate the controller's assembly state
     * through the returned {@link SmelteryStructure}, which is itself immutable.
     */
    public Optional<SmelteryStructure> getStructure() {
        return structure;
    }

    /**
     * Flags the controller to re-validate its multiblock on the next server tick. Called by the
     * SMTCON-117 disassembly listener when a seared or component block near this controller is
     * broken — the controller does not act immediately because the {@code BlockEvent.BreakEvent}
     * fires before the block is actually removed, so a same-tick re-scan would still see it.
     */
    public void invalidate() {
        needsValidation = true;
    }

    /**
     * Asks the validator whether a block change at {@code pos} with post-change role
     * {@code newRole} could affect this controller's assembled structure (SMTCON-227), and only
     * flags re-validation when the answer is yes. A loose controller (no cached structure) has
     * no structure to compare against and so falls back to the eager {@link #invalidate()} path
     * — the validator will quickly bail if no smeltery has formed near the change.
     */
    public void notifyChange(BlockPos pos, SmelteryStructureValidator.BlockRole newRole) {
        if (structure.isPresent()) {
            if (SmelteryStructureValidator.shouldUpdate(structure.get(), pos, newRole)) {
                needsValidation = true;
            }
        }
        else {
            needsValidation = true;
        }
    }

    /**
     * Re-runs structure validation and reconciles the controller's assembly state with the
     * blocks now in the world. On success the controller binds the new {@link SmelteryStructure}
     * and stamps every component block with its position; on failure it unbinds, and — if it
     * <em>was</em> assembled — releases its tank contents into the world rather than vanishing
     * them, since disassembly is a recovery path and must not destroy stored metal.
     */
    public void tryAssemble() {
        needsValidation = false;
        if (level == null || level.isClientSide()) {
            return;
        }
        Direction interiorDirection = getBlockState().getValue(SmelteryControllerBlock.FACING).getOpposite();
        Optional<SmelteryStructure> validated = SmelteryStructureValidator.validate(level, getBlockPos(), interiorDirection);
        if (validated.isPresent()) {
            bindStructure(validated.get());
        }
        else {
            Optional<SmelteryStructure> previous = structure;
            unbindStructure();
            previous.ifPresent(this::releaseTankContents);
        }
        setChanged();
    }

    /** Adopts {@code assembled} as the live structure and stamps every component with this controller. */
    private void bindStructure(SmelteryStructure assembled) {
        clearComponentStamps();
        structure = Optional.of(assembled);
        for (BlockPos componentPos : assembled.components().keySet()) {
            if (level.getBlockEntity(componentPos) instanceof SmelteryComponentBlockEntity component) {
                component.setControllerPos(getBlockPos());
            }
            // Mirror the binding into a blockstate property (SMTCON-226) so tooltips, JEI/Jade,
            // and block-render layers can ask "is this seared block part of an assembled
            // smeltery?" without a block-entity lookup.
            setInStructureProperty(componentPos, true);
        }
        // Scale the tank and the melting inventory to the bowl: a bigger smeltery holds more
        // metal and melts more at once. setCapacity keeps the held fluid and is idempotent, so
        // re-running it on every re-validation is harmless.
        fluidTank.setCapacity(assembled.bowlVolume() * MB_PER_INTERIOR_CELL);
        resizeMeltingSlots(assembled.bowlVolume());
        // Restart the streaming interior check (SMTCON-219) from the bowl's first cell so a
        // re-bind does not leave the cursor pointing past the new bounds.
        interiorCheckCursor = 0;
        // SMTCON-217: a bigger smeltery burns fuel faster — derive the per-tick charge-progress
        // rate from the wall count, matching upstream TC 1.20.1's 1 + walls/15 formula.
        fuelModule.setFuelDrawPerTick(1 + assembled.walls().size() / WALLS_PER_FUEL_DRAW_STEP);
        setLit(true);
    }

    /** Drops the live structure and detaches every component it had claimed. */
    private void unbindStructure() {
        clearComponentStamps();
        structure = Optional.empty();
        fluidTank.setCapacity(INITIAL_TANK_CAPACITY);
        resizeMeltingSlots(INITIAL_MELTING_SLOTS);
        interiorCheckCursor = 0;
        // Reset the fuel rate to the minimum-size default (SMTCON-217) so a loose controller
        // does not keep burning at the higher rate of its former shell.
        fuelModule.setFuelDrawPerTick(1);
        setLit(false);
    }

    /**
     * Resizes the melting inventory to {@code newSize}, preserving the contents
     * {@link ItemStackHandler#setSize} would otherwise discard. A shrink strands the slots past
     * the new end: their items are dropped into the world and any melt reserving them is
     * discarded, so a smaller smeltery keeps no phantom reservations. The change is pushed to
     * tracking clients with a block update so their inventory copy and the screen resize too.
     * Runs server-side only — its sole callers, {@link #bindStructure} / {@link #unbindStructure},
     * are reached only through the server-side {@link #tryAssemble}.
     */
    private void resizeMeltingSlots(int newSize) {
        int oldSize = meltingSlots.getSlots();
        if (oldSize == newSize || level == null) {
            return;
        }
        List<ItemStack> kept = new ArrayList<>(oldSize);
        for (int slot = 0; slot < oldSize; slot++) {
            kept.add(meltingSlots.getStackInSlot(slot));
        }
        meltingSlots.setSize(newSize);
        for (int slot = 0; slot < newSize && slot < kept.size(); slot++) {
            meltingSlots.setStackInSlot(slot, kept.get(slot));
        }
        if (newSize < oldSize) {
            activeMelts.removeIf(melt -> melt.slot() >= newSize);
            // Mirror the shrink into the slot-indexed map (SMTCON-225) — without this the index
            // would keep dead entries for slots that no longer exist, breaking the lockstep
            // invariant tickMelts and isSlotReserved rely on.
            meltsBySlot.keySet().removeIf(slot -> slot >= newSize);
            assertMeltIndexInvariant();
            for (int slot = newSize; slot < kept.size(); slot++) {
                if (!kept.get(slot).isEmpty()) {
                    Containers.dropItemStack(level, getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, kept.get(slot));
                }
            }
        }
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /**
     * Reflects the assembled state in the controller's {@code lit} blockstate so the block model
     * swaps to (or away from) its glowing front face. Skips the {@code setBlock} when the state
     * already matches, so an idle smeltery emits no block updates.
     */
    private void setLit(boolean lit) {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(SmelteryControllerBlock.LIT) && state.getValue(SmelteryControllerBlock.LIT) != lit) {
            level.setBlock(getBlockPos(), state.setValue(SmelteryControllerBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    /** Clears this controller's position from every component of the current structure, if any. */
    private void clearComponentStamps() {
        structure.ifPresent(current -> {
            for (BlockPos componentPos : current.components().keySet()) {
                if (level.getBlockEntity(componentPos) instanceof SmelteryComponentBlockEntity component) {
                    component.setControllerPos(null);
                }
                // Also clear the IN_STRUCTURE blockstate property (SMTCON-226) so a component
                // released by this controller no longer claims to belong to an assembled smeltery.
                setInStructureProperty(componentPos, false);
            }
        });
    }

    /**
     * Toggles the {@code in_structure} blockstate property on the component block at {@code pos}
     * (SMTCON-226). The property lives on {@link SmelteryComponentBlock#IN_STRUCTURE} for the
     * five seared component blocks and on {@link SmelteryControllerBlock#IN_STRUCTURE} for the
     * controller itself; both blocks declare it independently so the property check is keyed by
     * blockstate, not by class hierarchy. A no-op when the block at {@code pos} has neither
     * property (e.g. it was replaced before the unbind ran), or when the property already matches.
     */
    private void setInStructureProperty(BlockPos pos, boolean inStructure) {
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(SmelteryComponentBlock.IN_STRUCTURE) && state.getValue(SmelteryComponentBlock.IN_STRUCTURE) != inStructure) {
            level.setBlock(pos, state.setValue(SmelteryComponentBlock.IN_STRUCTURE, inStructure), Block.UPDATE_ALL);
        }
        else if (state.hasProperty(SmelteryControllerBlock.IN_STRUCTURE) && state.getValue(SmelteryControllerBlock.IN_STRUCTURE) != inStructure) {
            level.setBlock(pos, state.setValue(SmelteryControllerBlock.IN_STRUCTURE, inStructure), Block.UPDATE_ALL);
        }
    }

    /**
     * Pours the tank's molten metal into the world as source-fluid blocks across the former
     * interior volume, so a disassembled smeltery leaves its contents visible and recoverable
     * rather than deleting them. Only the amount actually placed is drained, so if the interior
     * has no room the unplaced metal stays in the tank — disassembly never loses fluid.
     */
    private void releaseTankContents(SmelteryStructure previous) {
        if (fluidTank.isEmpty()) {
            return;
        }
        BoundingBox interior = previous.bounds();
        // Snapshot the list up front — drain() mutates it, and iterating a live list while it
        // shrinks would skip entries. Each fluid is then released into the world in turn until
        // either the fluid is exhausted or the interior runs out of empty cells.
        List<FluidStack> snapshot = copyFluidList(fluidTank.getFluids());
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        for (FluidStack contents : snapshot) {
            if (contents.isEmpty()) {
                continue;
            }
            BlockState liquid = contents.getFluid().defaultFluidState().createLegacyBlock();
            if (liquid.isAir()) {
                // The fluid has no in-world block form — leave it in the tank rather than vanish it.
                continue;
            }
            int placeable = contents.getAmount() / FluidType.BUCKET_VOLUME;
            int placed = 0;
            for (int y = interior.minY(); y <= interior.maxY() && placed < placeable; y++) {
                for (int x = interior.minX(); x <= interior.maxX() && placed < placeable; x++) {
                    for (int z = interior.minZ(); z <= interior.maxZ() && placed < placeable; z++) {
                        target.set(x, y, z);
                        // Only count a release the world actually accepted — setBlock returns false
                        // if the placement did not take, and crediting it would drain fluid that
                        // was never poured out.
                        if (level.getBlockState(target).isAir() && level.setBlock(target, liquid, Block.UPDATE_ALL)) {
                            placed++;
                        }
                    }
                }
            }
            if (placed > 0) {
                fluidTank.drain(contents.copyWithAmount(placed * FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /**
     * Initial-sync tag sent to a client when it starts tracking the controller's chunk. Carries
     * the saved state plus the two sync-only fields ({@link #targetTemperature} and the structure
     * render bounds) so a newly-tracking client sees the full smeltery state at once; the
     * SMTCON-124 delta payloads then keep it current without resending this whole tag.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = saveWithoutMetadata(provider);
        // The full persisted structure (SMTCON-218) is disk-only — the client only renders the
        // bounds, which travel separately under TAG_RENDER_BOUNDS, so stripping it keeps the
        // chunk-tracking packet small.
        tag.remove(TAG_STRUCTURE);
        tag.putInt(TAG_TARGET_TEMPERATURE, targetTemperature);
        structure.map(SmelteryStructure::bounds)
                .ifPresent(bounds -> tag.putIntArray(TAG_RENDER_BOUNDS, new int[] { bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ() }));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Applies the {@link #getUpdateTag} initial-sync tag on the client — the saved state via
     * {@code super}, then the two sync-only fields.
     */
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        targetTemperature = tag.getInt(TAG_TARGET_TEMPERATURE);
        int[] bounds = tag.getIntArray(TAG_RENDER_BOUNDS);
        // Route through applyStructureUpdate so the tank capacity is re-derived from the bounds,
        // exactly as the SMTCON-124 delta payload does — the initial sync must not skip it.
        applyStructureUpdate(bounds.length == RENDER_BOUNDS_LENGTH ? Optional.of(new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5])) : Optional.empty());
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
        // Persist the assembled multiblock (SMTCON-218) so a reload restores it without re-running
        // the validator from scratch. Stripped from the client sync tag in getUpdateTag — disk only.
        structure.ifPresent(assembled -> tag.put(TAG_STRUCTURE, assembled.writeToTag()));
        // Persist the SMTCON-217 fuel charge so a smeltery reloaded mid-burn resumes from the
        // remaining-tick count it had at save time, rather than losing the charge and re-paying
        // the per-charge mB cost. Disk only — clients receive temperature via SmelteryFuelUpdatePayload.
        CompoundTag fuelTag = new CompoundTag();
        fuelModule.writeToNBT(fuelTag);
        tag.put(TAG_FUEL_MODULE, fuelTag);
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
        meltsBySlot.clear();
        ListTag melts = tag.getList(TAG_ACTIVE_MELTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < melts.size(); i++) {
            // A melt whose result fluid no longer parses (mod removed) is dropped rather than
            // crashing the world load -- MeltingProgress.load returns empty for a dead fluid.
            // A melt whose slot is out of range or already taken by an earlier loaded melt is
            // likewise dropped, so corrupt save data cannot seed a duplicate or orphaned melt.
            MeltingProgress.load(provider, melts.getCompound(i)).ifPresent(melt -> {
                if (melt.slot() >= 0 && melt.slot() < meltingSlots.getSlots() && !isSlotReserved(melt.slot())) {
                    activeMelts.add(melt);
                    meltsBySlot.put(melt.slot(), melt);
                }
            });
        }
        assertMeltIndexInvariant();
        // Restore the persisted multiblock (SMTCON-218) so a reloaded controller is immediately
        // assembled without re-running the validator. Tank capacity and melting-slot sizing are
        // derived from the structure here — chunk save NBT for those does not include the larger
        // sizes assigned at bind time, only the items/contents, so a missing rebind would leave
        // the controller running at INITIAL_* sizes and stranding the loaded slot data.
        //
        // A missing or malformed TAG_STRUCTURE (pre-SMTCON-218 saves, or a corrupted compound)
        // falls back to the legacy needsValidation flag — the first server tick will run the
        // validator and either re-assemble or release tank contents via the normal disassembly
        // path.
        Optional<SmelteryStructure> restored = tag.contains(TAG_STRUCTURE, Tag.TAG_COMPOUND) ? SmelteryStructure.readFromTag(tag.getCompound(TAG_STRUCTURE)) : Optional.empty();
        if (restored.isPresent() && meltingSlots.getSlots() == restored.get().bowlVolume()) {
            structure = restored;
            fluidTank.setCapacity(restored.get().bowlVolume() * MB_PER_INTERIOR_CELL);
            // Restore the SMTCON-217 fuelDrawPerTick from the same shell-size formula bindStructure
            // uses — the rate itself is not persisted because the structure is, and the rate is a
            // pure function of the wall count.
            fuelModule.setFuelDrawPerTick(1 + restored.get().walls().size() / WALLS_PER_FUEL_DRAW_STEP);
            // Component BEs persist their own controllerPos so they normally rehydrate without
            // help, but the disk restore path bypasses bindStructure; schedule a stamp refresh on
            // the first server tick (when the world and component chunks are guaranteed loaded)
            // so a partial save or a chunk-edited component still gets reconnected.
            needsValidation = false;
            needsComponentRestamp = true;
        }
        else {
            // A missing tag (pre-SMTCON-218 save), a malformed compound, or a mismatch between
            // the restored bowl volume and the loaded slot count all fall back to the legacy
            // validator path — the first post-load tick will re-assemble or release contents.
            needsValidation = true;
        }
        // Restore the SMTCON-217 fuel charge (ticks remaining + cached temperature) so a smeltery
        // reloaded mid-burn resumes from where it left off, no fresh charge required.
        if (tag.contains(TAG_FUEL_MODULE, Tag.TAG_COMPOUND)) {
            fuelModule.readFromNBT(tag.getCompound(TAG_FUEL_MODULE));
        }
    }
}
