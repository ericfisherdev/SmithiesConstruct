package slimeknights.sconstruct.port1211.smeltery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Shared block entity for the two cast-handling smeltery blocks — the casting table and the
 * casting basin (SMTCON-113). Both carry exactly the same state: a single-slot cast inventory
 * and one {@link FluidTank}; they differ only in tank capacity (288 mB / one ingot for the
 * table, 2592 mB / one block for the basin), which the subclass passes to this constructor.
 *
 * <p>The {@link FluidTank} is exposed through {@link #getFluidHandler()} as the block's
 * {@code Capabilities.FluidHandler.BLOCK} capability so a fluid source above — a poured molten
 * metal, a drained smeltery — can fill it. At this layer the tank is a plain capacity-bounded
 * tank: it accepts any fluid up to capacity. Casting-recipe matching (reject fluid that no
 * {@code CastingRecipe} consumes) and recipe completion (swap the cast slot for the recipe
 * output, optionally consuming the cast) are deliberately out of scope — they land with the
 * casting recipe implementation (SMTCON-123). This class is the BE skeleton the AC asks for:
 * storage, persistence, and the fluid-handler capability, with no recipe logic.
 */
public abstract class AbstractCastingBlockEntity extends BlockEntity {

    /** NBT key for the serialised cast-slot inventory. */
    private static final String TAG_CAST = "Cast";

    /** NBT key for the serialised fluid tank. */
    private static final String TAG_TANK = "Tank";

    /** Single-slot cast inventory — holds the cast item the table / basin pours metal into. */
    private final ItemStackHandler castHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** Capacity-bounded fluid tank — the poured metal lands here until a recipe consumes it. */
    private final FluidTank tank;

    /**
     * @param type     the registered {@link BlockEntityType} for the concrete block (table or basin)
     * @param pos      block position, forwarded to {@link BlockEntity}
     * @param state    placed block state, forwarded to {@link BlockEntity}
     * @param capacity the tank capacity in mB — 288 for the table, 2592 for the basin
     */
    protected AbstractCastingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.tank = new FluidTank(capacity) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    /** The single-slot cast inventory. Used by the block's right-click add / remove handling. */
    public ItemStackHandler getCastHandler() {
        return castHandler;
    }

    /**
     * The fluid tank, exposed as the {@code Capabilities.FluidHandler.BLOCK} capability so a
     * fluid source can pour metal into the table / basin.
     */
    public IFluidHandler getFluidHandler() {
        return tank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(TAG_CAST, castHandler.serializeNBT(provider));
        tag.put(TAG_TANK, tank.writeToNBT(provider, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(TAG_CAST, Tag.TAG_COMPOUND)) {
            castHandler.deserializeNBT(provider, tag.getCompound(TAG_CAST));
        }
        if (tag.contains(TAG_TANK, Tag.TAG_COMPOUND)) {
            tank.readFromNBT(provider, tag.getCompound(TAG_TANK));
        }
    }
}
