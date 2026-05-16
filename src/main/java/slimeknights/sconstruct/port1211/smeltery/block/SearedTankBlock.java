package slimeknights.sconstruct.port1211.smeltery.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;

import slimeknights.sconstruct.port1211.smeltery.block.entity.SearedTankBE;

/**
 * Abstract base for the two seared tank blocks (SMTCON-118) — the standalone fluid containers
 * of the smeltery. Splits the tank-specific behaviour off {@link SmelteryComponentBlock}: a tank
 * carries a {@link SearedTankBE} (which owns a real {@link net.neoforged.neoforge.fluids.capability.templates.FluidTank})
 * rather than the bare controller-proxy block entity, accepts bucket interactions, and spills
 * its contents when broken.
 *
 * <p><strong>Bucket interaction.</strong> {@link #useItemOn} routes a held bucket through
 * {@link FluidUtil#interactWithFluidHandler}, which fills the tank from a full bucket or empties
 * the tank into an empty one against the tank's {@code FluidHandler.BLOCK} capability.
 *
 * <p><strong>Spill on break.</strong> {@link #onRemove} pops the tank's stored metal as filled
 * buckets — one per {@value FluidType#BUCKET_VOLUME} mB — so mining a full tank does not vanish
 * its contents. A sub-bucket remainder is not recoverable as an item and is dropped; tanks are
 * filled in bucket units in normal play, so this is not a practical loss.
 */
public abstract class SearedTankBlock extends SmelteryComponentBlock {

    protected SearedTankBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SearedTankBE(beType(), pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // interactWithFluidHandler handles both directions — full bucket fills the tank, empty
        // bucket drains it — against the tank's FluidHandler.BLOCK capability.
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Guard on an actual block change (not a state-only update) so a blockstate flip does
        // not spill the tank.
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SearedTankBE tank) {
            spillContents(level, pos, tank);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Pops the tank's stored fluid as filled buckets, one per bucket volume. */
    private static void spillContents(Level level, BlockPos pos, SearedTankBE tank) {
        FluidStack contents = tank.getFluidHandler().getFluidInTank(0);
        int buckets = contents.getAmount() / FluidType.BUCKET_VOLUME;
        if (buckets <= 0) {
            return;
        }
        // copyWithAmount keeps the fluid's data components — only the amount is changed to one
        // bucket — so a dropped bucket carries the same molten metal that was stored.
        ItemStack filledBucket = FluidUtil.getFilledBucket(contents.copyWithAmount(FluidType.BUCKET_VOLUME));
        if (filledBucket.isEmpty()) {
            // The fluid has no bucket form — nothing to drop rather than crash.
            return;
        }
        for (int i = 0; i < buckets; i++) {
            Block.popResource(level, pos, filledBucket.copy());
        }
    }
}
