package slimeknights.sconstruct.port1211.smeltery.block.entity;

import java.util.ArrayList;
import java.util.Collections;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;
import slimeknights.sconstruct.port1211.smeltery.SmelteryFuelSource;
import slimeknights.sconstruct.port1211.smeltery.block.SmelteryControllerBlock;
import slimeknights.sconstruct.port1211.smeltery.multiblock.ComponentType;
import slimeknights.sconstruct.port1211.smeltery.multiblock.SmelteryStructure;
import slimeknights.sconstruct.port1211.smeltery.multiblock.SmelteryStructureValidator;
import slimeknights.sconstruct.port1211.smeltery.network.SmelteryFluidUpdatePayload;
import slimeknights.sconstruct.port1211.smeltery.network.SmelteryFuelUpdatePayload;
import slimeknights.sconstruct.port1211.smeltery.network.SmelteryStructureUpdatePayload;

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

    /** Millibuckets of fuel drawn from the active fuel tank per in-flight melt, per server tick. */
    private static final int FUEL_DRAW_PER_MELT = 10;

    private static final String TAG_TANK = "Tank";
    private static final String TAG_MELTING_SLOTS = "MeltingSlots";
    private static final String TAG_TEMPERATURE = "Temperature";
    private static final String TAG_ACTIVE_MELTS = "ActiveMelts";

    /** Update-tag key for the target temperature — sync-only, not part of the saved state. */
    private static final String TAG_TARGET_TEMPERATURE = "TargetTemperature";

    /** Update-tag key for the interior render bounds — sync-only, not part of the saved state. */
    private static final String TAG_RENDER_BOUNDS = "RenderBounds";

    /** Number of integers in the {@link #TAG_RENDER_BOUNDS} array — the six box corners. */
    private static final int RENDER_BOUNDS_LENGTH = 6;

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

    /** Current internal temperature in kelvin, set each tick from the active fuel source. */
    private int currentTemperature;

    /** Temperature in kelvin the active fuel source can sustain — the heat the smeltery heads toward. */
    private int targetTemperature;

    /** Melts currently in progress, advanced one tick at a time by {@link #tickMelts()}. */
    private final List<MeltingProgress> activeMelts = new ArrayList<>();

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
    private FluidStack lastSyncedFluid = FluidStack.EMPTY;

    /** Interior bounds last pushed to chunk trackers, so an unchanged structure is not re-synced. */
    private Optional<BoundingBox> lastSyncedBounds = Optional.empty();

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
     * {@code SmelteryControllerBlock#getTicker}. Re-validates the multiblock when flagged, draws
     * fuel from the bound tanks, then advances the active melts — but only while fuel is
     * available, so an out-of-fuel smeltery pauses its melts rather than running them cold.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SmelteryControllerBlockEntity controller) {
        if (controller.needsValidation) {
            controller.tryAssemble();
        }
        controller.tickSmeltery();
        controller.syncToTrackers();
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
        FluidStack fluid = fluidTank.getFluid();
        if (!FluidStack.matches(fluid, lastSyncedFluid)) {
            lastSyncedFluid = fluid.copy();
            List<FluidStack> contents = fluid.isEmpty() ? List.of() : List.of(fluid.copy());
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk, new SmelteryFluidUpdatePayload(getBlockPos(), contents));
        }
        Optional<BoundingBox> bounds = structure.map(SmelteryStructure::bounds);
        if (!bounds.equals(lastSyncedBounds)) {
            lastSyncedBounds = bounds;
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk, new SmelteryStructureUpdatePayload(getBlockPos(), bounds));
        }
    }

    /**
     * Applies a {@code SmelteryFluidUpdatePayload} to this client-side controller — the tank is
     * set to the synced contents so the next frame renders the smeltery's fill.
     */
    public void applyFluidUpdate(List<FluidStack> contents) {
        fluidTank.setFluid(contents.isEmpty() ? FluidStack.EMPTY : contents.get(0));
    }

    /** Applies a {@code SmelteryFuelUpdatePayload} to this client-side controller's heat gauge. */
    public void applyFuelUpdate(int current, int target) {
        currentTemperature = current;
        targetTemperature = target;
    }

    /** Applies a {@code SmelteryStructureUpdatePayload} to this client-side controller's render bounds. */
    public void applyStructureUpdate(Optional<BoundingBox> bounds) {
        renderBounds = bounds;
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
     * Polls every seared tank bound to the assembled structure, picks the hottest one that can
     * provide fuel, and consumes from it. Sets {@link #currentTemperature} to that tank's
     * temperature and returns {@code true}; with no fuel available it sets the temperature to
     * {@code 0} and returns {@code false} so {@link #tickSmeltery()} pauses the melts.
     */
    private boolean drawFuel() {
        SmelteryFuelSource hottest = null;
        int hottestTemperature = 0;
        if (level != null && structure.isPresent()) {
            for (Map.Entry<BlockPos, ComponentType> component : structure.get().components().entrySet()) {
                if (component.getValue() == ComponentType.TANK && level.getBlockEntity(component.getKey()) instanceof SmelteryFuelSource fuel && fuel.canProvideFuel()
                        && fuel.getTemperature() > hottestTemperature) {
                    hottest = fuel;
                    hottestTemperature = fuel.getTemperature();
                }
            }
        }
        if (hottest == null) {
            setTemperature(0);
            return false;
        }
        // Fuel draw scales with the number of melts in progress — a busier smeltery burns hotter.
        // Treat a zero-consumption draw as out-of-fuel so the melts pause rather than run cold.
        int consumed = hottest.consumeFuel(FUEL_DRAW_PER_MELT * activeMelts.size());
        if (consumed <= 0) {
            setTemperature(0);
            return false;
        }
        setTemperature(hottestTemperature);
        return true;
    }

    /**
     * Updates the internal temperature, marking the chunk dirty only when the value changes. The
     * current and target temperatures move together — the smeltery has no gradual heat-up curve
     * yet — but are kept as distinct fields so {@code SmelteryFuelUpdatePayload} carries both.
     */
    private void setTemperature(int temperature) {
        if (currentTemperature != temperature) {
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
        }
    }

    /** Drops the live structure and detaches every component it had claimed. */
    private void unbindStructure() {
        clearComponentStamps();
        structure = Optional.empty();
    }

    /** Clears this controller's position from every component of the current structure, if any. */
    private void clearComponentStamps() {
        structure.ifPresent(current -> {
            for (BlockPos componentPos : current.components().keySet()) {
                if (level.getBlockEntity(componentPos) instanceof SmelteryComponentBlockEntity component) {
                    component.setControllerPos(null);
                }
            }
        });
    }

    /**
     * Pours the tank's molten metal into the world as source-fluid blocks across the former
     * interior volume, so a disassembled smeltery leaves its contents visible and recoverable
     * rather than deleting them. Only the amount actually placed is drained, so if the interior
     * has no room the unplaced metal stays in the tank — disassembly never loses fluid.
     */
    private void releaseTankContents(SmelteryStructure previous) {
        FluidStack contents = fluidTank.getFluid();
        if (contents.isEmpty()) {
            return;
        }
        BlockState liquid = contents.getFluid().defaultFluidState().createLegacyBlock();
        if (liquid.isAir()) {
            // The fluid has no in-world block form — leave it in the tank rather than vanish it.
            return;
        }
        int placeable = contents.getAmount() / FluidType.BUCKET_VOLUME;
        int placed = 0;
        BoundingBox interior = previous.bounds();
        for (int y = interior.minY(); y <= interior.maxY() && placed < placeable; y++) {
            for (int x = interior.minX(); x <= interior.maxX() && placed < placeable; x++) {
                for (int z = interior.minZ(); z <= interior.maxZ() && placed < placeable; z++) {
                    BlockPos target = new BlockPos(x, y, z);
                    // Only count a release the world actually accepted — setBlock returns false
                    // if the placement did not take, and crediting it would drain fluid that was
                    // never poured out.
                    if (level.getBlockState(target).isAir() && level.setBlock(target, liquid, Block.UPDATE_ALL)) {
                        placed++;
                    }
                }
            }
        }
        if (placed > 0) {
            fluidTank.drain(placed * FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
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
        renderBounds = bounds.length == RENDER_BOUNDS_LENGTH ? Optional.of(new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5])) : Optional.empty();
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
        // The structure is never persisted — flag a re-validation so the first post-load tick
        // rebuilds it from the blocks actually in the world (which may have changed while the
        // chunk was unloaded).
        needsValidation = true;
    }
}
