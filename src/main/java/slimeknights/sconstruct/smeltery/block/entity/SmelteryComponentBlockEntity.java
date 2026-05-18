package slimeknights.sconstruct.smeltery.block.entity;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity shared by every non-controller smeltery component — the tank IO, tank in, gauge,
 * drain, and chute blocks (SMTCON-112). A single concrete class backs all five
 * {@link BlockEntityType}s rather than one BE class per block because, at this layer, the five
 * components carry identical state: a back-reference to the smeltery controller that has claimed
 * this position as part of an assembled multiblock. The {@link BlockEntityType} is therefore
 * passed into the constructor instead of being read from a single registry field — the same BE
 * shape is reused under five distinct registered types so each component block keeps its own
 * type identity (needed for the per-component capability and renderer wiring landing in
 * SMTCON-116/118).
 *
 * <p>The {@link #controllerPos} field is the whole purpose of this class: it makes the component
 * a <em>proxy</em>. A loose, unassembled component holds {@link Optional#empty()}; once the
 * controller's structure-validation pass (SMTCON-114) accepts the multiblock it stamps every
 * component with the controller's position so that a fluid insert against a tank, or an item
 * insert against a chute, can be forwarded to the controller's inventory without re-scanning the
 * structure on every interaction. This BE deliberately holds no inventory and exposes no menu —
 * the actual fluid tanks and the controller GUI are owned elsewhere; this is purely the
 * controller-position proxy.
 */
public class SmelteryComponentBlockEntity extends BlockEntity {

    /** NBT key under which {@link #controllerPos} is round-tripped via {@link NbtUtils}. */
    private static final String TAG_CONTROLLER_POS = "ControllerPos";

    /**
     * Position of the smeltery controller that has claimed this component, or
     * {@link Optional#empty()} when the component is not part of an assembled smeltery. The
     * contained {@link BlockPos} is always an {@link BlockPos#immutable() immutable} copy so a
     * caller passing a mutable {@code BlockPos.MutableBlockPos} cannot mutate this BE's state
     * from the outside after the fact.
     */
    private Optional<BlockPos> controllerPos = Optional.empty();

    /**
     * @param type  the registered {@link BlockEntityType} for the specific component block this
     *              BE backs — supplied by the caller because one class backs five distinct types
     * @param pos   the block position, forwarded to {@link BlockEntity}
     * @param state the placed block state, forwarded to {@link BlockEntity}
     */
    public SmelteryComponentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * The smeltery controller this component belongs to, if any. Returns an empty
     * {@link Optional} for a loose component so callers branch on assembly state without a null
     * check — a component with no controller simply does nothing on interaction.
     */
    public Optional<BlockPos> getControllerPos() {
        return controllerPos;
    }

    /**
     * Resolves the live {@link SmelteryControllerBlockEntity} this component proxies to. This is
     * the lookup the capability proxies in {@code SmelteryCapabilities} call (SMTCON-116): a
     * drain or tank forwards its fluid handler, and a chute forwards its item handler, to
     * whatever controller this returns.
     *
     * <p>Returns {@link Optional#empty()} for a loose component, when the level is not yet set,
     * or when the recorded controller position no longer holds a controller block-entity (it was
     * broken, or this component still carries a stale binding) — so a proxied capability simply
     * resolves to {@code null} rather than throwing.
     */
    public Optional<SmelteryControllerBlockEntity> getControllerOpt() {
        if (level == null || controllerPos.isEmpty()) {
            return Optional.empty();
        }
        return level.getBlockEntity(controllerPos.get()) instanceof SmelteryControllerBlockEntity controller ? Optional.of(controller) : Optional.empty();
    }

    /**
     * Records (or clears) the controller that owns this component. Called by the controller's
     * structure-validation pass when a smeltery assembles ({@code pos} non-null) and when it
     * breaks apart ({@code pos} null). A defensive {@link BlockPos#immutable() immutable} copy is
     * stored so a caller reusing a mutable position cannot later corrupt this BE's state, and
     * {@link #setChanged()} is invoked so the chunk is flagged dirty and the new ownership
     * survives a world save.
     *
     * <p>The component's proxied capability also changes meaning on every binding change — a
     * newly-bound component starts forwarding, an unbound one stops — so the block's capabilities
     * are invalidated here, prompting NeoForge to re-resolve them against the new ownership the
     * next time a neighbour queries.
     *
     * @param pos the controller position, or {@code null} to detach this component
     */
    public void setControllerPos(@Nullable BlockPos pos) {
        this.controllerPos = pos == null ? Optional.empty() : Optional.of(pos.immutable());
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(getBlockPos());
        }
    }

    /**
     * Persists {@link #controllerPos} when set. A loose component writes nothing, so its saved
     * NBT stays minimal and {@link #loadAdditional} reads back an empty controller — the correct
     * default for an unassembled component.
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        controllerPos.ifPresent(pos -> tag.put(TAG_CONTROLLER_POS, NbtUtils.writeBlockPos(pos)));
    }

    /**
     * Restores {@link #controllerPos}. An absent key leaves the field {@link Optional#empty()} —
     * a component saved while loose, or one saved before this field existed, loads back as
     * unassembled rather than throwing.
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.controllerPos = tag.contains(TAG_CONTROLLER_POS, Tag.TAG_INT_ARRAY) ? NbtUtils.readBlockPos(tag, TAG_CONTROLLER_POS) : Optional.empty();
    }
}
