package slimeknights.sconstruct.port1211.smeltery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import slimeknights.sconstruct.port1211.smeltery.block.SearedTankIoBlock;

/**
 * Block entity for the two seared tank blocks (SMTCON-118) — the standalone fluid containers of
 * the smeltery family. Unlike the seared drain and chute, a tank is <em>not</em> a pure proxy:
 * it owns a real {@link FluidTank} and works as fluid storage whether or not it has been claimed
 * by a smeltery controller. It still extends {@link SmelteryComponentBlockEntity} so the
 * controller-binding machinery (SMTCON-116/117) keeps working — an assembled smeltery still
 * stamps every tank with its controller — but the {@code Capabilities.FluidHandler.BLOCK}
 * registration exposes <em>this</em> tank, not the controller's, so a loose tank is usable on
 * its own and a bucket interaction always has a handler to talk to.
 *
 * <p><strong>Capacity.</strong> The two tank variants store different volumes — the IO tank
 * {@value #CAPACITY_IO} mB, the input tank {@value #CAPACITY_IN} mB. The capacity is derived
 * from the placed block in the constructor ({@link #capacityFor}) rather than passed in, so the
 * block-entity-type factory and the block's {@code newBlockEntity} cannot disagree on it.
 *
 * <p>The tank contents are round-tripped through this BE's own NBT, so a tank preserves its
 * fluid across a world save. Breaking the block spills the contents as filled buckets — that
 * drop is handled by {@code SearedTankBlock}, which reads {@link #getFluidHandler()}.
 */
public class SearedTankBE extends SmelteryComponentBlockEntity {

    /** Capacity in mB of the seared tank IO — the larger in/out storage tank. */
    public static final int CAPACITY_IO = 4 * FluidType.BUCKET_VOLUME;

    /** Capacity in mB of the seared tank in — the smaller input-only tank. */
    public static final int CAPACITY_IN = 2 * FluidType.BUCKET_VOLUME;

    private static final String TAG_TANK = "Tank";

    /** This tank's own fluid storage; capacity fixed by the variant placed. */
    private final FluidTank fluidTank;

    /**
     * @param type  the registered {@link BlockEntityType} for the tank variant this BE backs
     * @param pos   the block position, forwarded to {@link SmelteryComponentBlockEntity}
     * @param state the placed block state — its block decides the tank capacity
     */
    public SearedTankBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.fluidTank = new FluidTank(capacityFor(state)) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    /** The capacity in mB for the tank variant identified by {@code state}'s block. */
    private static int capacityFor(BlockState state) {
        return state.getBlock() instanceof SearedTankIoBlock ? CAPACITY_IO : CAPACITY_IN;
    }

    /** This tank's fluid storage, exposed as the {@code FluidHandler.BLOCK} capability. */
    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    /**
     * Persists the controller binding (via {@link SmelteryComponentBlockEntity}) and this tank's
     * own contents, so a stored tank reloads with its fluid intact.
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(TAG_TANK, fluidTank.writeToNBT(provider, new CompoundTag()));
    }

    /**
     * Restores the controller binding and this tank's contents. A missing tag leaves the tank
     * empty — the correct default for a tank saved before it held anything.
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(TAG_TANK, Tag.TAG_COMPOUND)) {
            fluidTank.readFromNBT(provider, tag.getCompound(TAG_TANK));
        }
    }
}
