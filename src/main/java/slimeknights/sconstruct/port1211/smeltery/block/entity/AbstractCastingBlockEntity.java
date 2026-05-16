package slimeknights.sconstruct.port1211.smeltery.block.entity;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import slimeknights.sconstruct.port1211.smeltery.recipe.CastingRecipe;
import slimeknights.sconstruct.port1211.smeltery.recipe.CastingRecipeInput;
import slimeknights.sconstruct.port1211.smeltery.recipe.SmelteryRecipes;

/**
 * Shared block entity for the two cast-handling smeltery blocks — the casting table and the
 * casting basin (SMTCON-113 / SMTCON-123). Both carry the same state: a single-slot cast
 * inventory, one {@link FluidTank}, and a cooling countdown. They differ only in tank capacity
 * (288 mB / one ingot for the table, 2592 mB / one block for the basin) and in
 * {@link #isBasin()}, which routes each to its own {@link CastingRecipe} variant.
 *
 * <p><strong>Recipe flow (SMTCON-123).</strong> The tank rejects any fluid that no
 * {@link CastingRecipe} for this block's variant and current cast can consume
 * ({@link FluidTank#isFluidValid}). Once the tank holds enough fluid to satisfy a matching
 * recipe, {@link #serverTick} starts a cooling countdown of the recipe's
 * {@link CastingRecipe#coolingTime()} ticks; when it elapses the cast slot is replaced with the
 * recipe output, the cast item is dropped back into the world if the recipe does not consume it,
 * and the tank is emptied.
 *
 * <p>State changes are pushed to chunk trackers via the standard block-entity sync
 * ({@link #getUpdateTag} / {@link #getUpdatePacket}) so a client sees the tank fill, the cooling
 * progress, and the finished cast. The dedicated {@code SmelteryFluidUpdatePayload} arrives in
 * SMTCON-124; until then the vanilla BE sync carries the same data.
 */
public abstract class AbstractCastingBlockEntity extends BlockEntity {

    /** NBT key for the serialised cast-slot inventory. */
    private static final String TAG_CAST = "Cast";

    /** NBT key for the serialised fluid tank. */
    private static final String TAG_TANK = "Tank";

    /** NBT key for the cooling countdown. */
    private static final String TAG_COOLING = "CoolingTimer";

    /** Single-slot cast inventory — holds the cast item the table / basin pours metal into. */
    private final ItemStackHandler castHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }
    };

    /** Capacity-bounded fluid tank — the poured metal lands here until a recipe consumes it. */
    private final FluidTank tank;

    /** Ticks remaining before the in-progress cast completes; {@code 0} when nothing is cooling. */
    private int coolingTimer;

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
                markUpdated();
            }

            @Override
            public boolean isFluidValid(FluidStack stack) {
                // Reject any fluid no casting recipe for this block + cast can consume.
                return acceptsFluid(stack);
            }
        };
    }

    /** Whether this is a casting basin ({@code true}) or a casting table ({@code false}). */
    protected abstract boolean isBasin();

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

    /** Ticks remaining before the in-progress cast completes, or {@code 0} when idle. */
    public int getCoolingTimer() {
        return coolingTimer;
    }

    /**
     * Server-side {@code BlockEntityTicker} entry point, registered by {@code AbstractCastingBlock}.
     * Advances a running cooling countdown and completes the cast when it elapses; when idle,
     * starts the countdown as soon as the tank holds a full casting recipe's worth of fluid.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractCastingBlockEntity casting) {
        casting.tickCasting();
    }

    /** Advances the cooling countdown, or starts one when the tank first holds a matching recipe. */
    private void tickCasting() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (coolingTimer > 0) {
            coolingTimer--;
            if (coolingTimer == 0) {
                completeCast();
            }
            else {
                setChanged();
            }
            return;
        }
        if (!tank.getFluid().isEmpty()) {
            findCastingRecipe(tank.getFluid()).ifPresent(recipe -> {
                coolingTimer = recipe.coolingTime();
                markUpdated();
            });
        }
    }

    /**
     * Completes the cast: replaces the cast slot with the recipe output, drops the original cast
     * back into the world when the recipe does not consume it, and empties the tank. Aborts
     * harmlessly if the cast was removed mid-cooling and no recipe matches any more.
     */
    private void completeCast() {
        if (level == null) {
            return;
        }
        FluidStack contents = tank.getFluid();
        Optional<CastingRecipe> match = findCastingRecipe(contents);
        if (match.isEmpty()) {
            return;
        }
        CastingRecipe recipe = match.get();
        ItemStack cast = castHandler.getStackInSlot(0);
        CastingRecipeInput input = new CastingRecipeInput(contents, cast, isBasin());
        castHandler.setStackInSlot(0, recipe.assemble(input, level.registryAccess()));
        if (!recipe.consumeCast() && !cast.isEmpty()) {
            Block.popResource(level, getBlockPos().above(), cast);
        }
        tank.setFluid(FluidStack.EMPTY);
        markUpdated();
    }

    /** The casting recipe matching the given tank fluid, this block's cast, and its variant. */
    private Optional<CastingRecipe> findCastingRecipe(FluidStack fluid) {
        if (level == null) {
            return Optional.empty();
        }
        CastingRecipeInput input = new CastingRecipeInput(fluid, castHandler.getStackInSlot(0), isBasin());
        return level.getRecipeManager().getRecipeFor(SmelteryRecipes.CASTING_TYPE.get(), input, level).map(RecipeHolder::value);
    }

    /**
     * Whether some casting recipe for this block's variant and current cast accepts {@code stack}'s
     * fluid — checked by fluid type only, ignoring amount, so an incremental pour is not rejected
     * before the tank is full.
     */
    private boolean acceptsFluid(FluidStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        ItemStack cast = castHandler.getStackInSlot(0);
        for (RecipeHolder<CastingRecipe> holder : level.getRecipeManager().getAllRecipesFor(SmelteryRecipes.CASTING_TYPE.get())) {
            CastingRecipe recipe = holder.value();
            if (recipe.isBasin() == isBasin() && recipe.fluid().fluids().contains(stack.getFluidHolder()) && recipe.cast().test(cast)) {
                return true;
            }
        }
        return false;
    }

    /** Flags the chunk dirty and pushes the block-entity state to chunk trackers. */
    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(TAG_CAST, castHandler.serializeNBT(provider));
        tag.put(TAG_TANK, tank.writeToNBT(provider, new CompoundTag()));
        tag.putInt(TAG_COOLING, coolingTimer);
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
        coolingTimer = tag.getInt(TAG_COOLING);
    }
}
