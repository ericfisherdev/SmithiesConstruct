package slimeknights.sconstruct.smeltery.block.entity;

import net.minecraft.core.BlockPos;

/**
 * Shared timing helper for the smeltery block entities' ambient particle effects (SMTCON-167).
 * A block entity that emits particles on a fixed tick interval calls
 * {@link #shouldEmitThisTick} so its emissions are phased by block position — without the
 * phase, every lit smeltery or cooling cast would emit on the same server ticks and spike the
 * particle-packet rate on a busy server.
 */
final class ParticleEmission {

    private ParticleEmission() {
    }

    /**
     * Whether a block entity at {@code pos} should emit its particle this tick, given a fixed
     * {@code interval}. The emission tick is offset by a position-derived phase so emitters
     * spread their sends across the interval instead of bursting in lockstep.
     */
    static boolean shouldEmitThisTick(BlockPos pos, long gameTime, int interval) {
        long phase = Math.floorMod(pos.hashCode(), interval);
        return (gameTime + phase) % interval == 0;
    }
}
