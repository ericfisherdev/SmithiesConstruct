package slimeknights.sconstruct.port1211.gadgets.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.gadgets.GadgetBlocks;

/**
 * Block entity for the wooden hopper (SMTCON-138) — a pre-iron-tier hopper that moves items at
 * half the speed of a vanilla iron hopper.
 *
 * <p>It reuses {@link HopperBlockEntity}'s entire transfer machinery; the only behavioural
 * change is the transfer cooldown. Vanilla's {@code HopperBlockEntity} hard-codes an 8-tick
 * cooldown by calling {@link #setCooldown(int)} after every move, so this subclass overrides
 * that one seam to double any positive cooldown — 8 ticks becomes {@value #COOLDOWN_MULTIPLIER}
 * × 8 = 16, halving the throughput. A zero / negative cooldown (the "ready now" and "clear"
 * cases) is passed through untouched so the hopper still ticks correctly.
 *
 * <p>{@code HopperBlockEntity}'s only public constructor pins the block-entity type to vanilla
 * {@link BlockEntityType#HOPPER}, so {@link #getType()} is overridden to report this mod's
 * registered type instead — the type the saved-data {@code id} and the ticker lookup both key
 * off. The item inventory, NBT format, and hopper GUI are inherited unchanged.
 */
public class WoodenHopperBlockEntity extends HopperBlockEntity {

    /** Factor applied to every positive transfer cooldown — a wooden hopper is twice as slow. */
    private static final int COOLDOWN_MULTIPLIER = 2;

    public WoodenHopperBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        // HopperBlockEntity's constructor pins the field to BlockEntityType.HOPPER; report the
        // mod's own type so save/load and the block ticker resolve the wooden hopper correctly.
        return GadgetBlocks.WOODEN_HOPPER_BE.get();
    }

    @Override
    public void setCooldown(int cooldownTime) {
        // Double any real cooldown so transfers run at half the iron hopper's rate; leave the
        // zero / negative "ready" and "clear" sentinels alone so ticking stays correct.
        super.setCooldown(cooldownTime > 0 ? cooldownTime * COOLDOWN_MULTIPLIER : cooldownTime);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + SConstruct.MOD_ID + ".wooden_hopper");
    }
}
