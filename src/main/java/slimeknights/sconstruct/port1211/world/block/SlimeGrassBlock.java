package slimeknights.sconstruct.port1211.world.block;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Grass cover for a coloured {@link SlimeDirtBlock}. Behaves like vanilla {@link
 * net.minecraft.world.level.block.GrassBlock} in spirit: random ticks try to spread the grass
 * onto adjacent slime dirt of the same colour, and dying back when the block above blocks light
 * reverts the grass to plain dirt. Bonemeal is a no-op for now — the legacy mod attached
 * decorative grass tufts on bonemeal; modern revival of that lives in a follow-up Phase-3 task.
 *
 * <p>Spread logic is implemented in {@link #randomTick} rather than by extending vanilla
 * {@link net.minecraft.world.level.block.SpreadingSnowyDirtBlock}: that class hard-codes
 * {@code Blocks.DIRT} as the spread target so the inherited tick can't be aimed at
 * {@link SlimeDirtBlock}. Re-implementing in a single override keeps the slime-only spread
 * contract explicit and avoids a fragile dependency on a private vanilla constant.
 */
public final class SlimeGrassBlock extends Block implements BonemealableBlock {

    /** Light level (sky+block max) at which grass survives on top of dirt. Matches vanilla. */
    private static final int GRASS_LIGHT_THRESHOLD = 9;

    /** Light level at which grass dies back to dirt. Matches vanilla. */
    private static final int GRASS_DEATH_LIGHT_THRESHOLD = 4;

    /** Number of spread attempts per random tick. Matches vanilla {@code SpreadingSnowyDirtBlock}. */
    private static final int SPREAD_ATTEMPTS = 4;

    /** Source-level fluid amount (full-block fluid). Vanilla uses this as the "kills grass underneath" threshold. */
    private static final int SOURCE_FLUID_AMOUNT = 8;

    private final SlimeColor color;
    private final Supplier<? extends Block> matchingDirt;

    /**
     * @param color        the slime colour both this grass and the dirt it spreads onto carry
     * @param matchingDirt supplier returning the {@link SlimeDirtBlock} this grass spreads onto;
     *                     a supplier (rather than a direct reference) breaks the registration
     *                     cycle — the dirt block is registered before the grass block fires its
     *                     factory, but at field-initialiser time the {@link
     *                     net.neoforged.neoforge.registries.DeferredBlock} for the dirt block
     *                     is still empty
     * @param properties   block behaviour
     */
    public SlimeGrassBlock(SlimeColor color, Supplier<? extends Block> matchingDirt, BlockBehaviour.Properties properties) {
        super(properties);
        this.color = Objects.requireNonNull(color, "color");
        this.matchingDirt = Objects.requireNonNull(matchingDirt, "matchingDirt");
    }

    /** The slime colour this grass block was registered with. */
    public SlimeColor color() {
        return color;
    }

    /**
     * Vanilla-equivalent {@code canBeGrass} check: the block directly above must not block
     * light enough to kill grass. Used by {@link #randomTick} both to decide whether the
     * current block dies back, and whether a neighbouring dirt block can become grass. Single-
     * layer snow always passes (snow doesn't kill grass); fully-flowing-water always fails
     * (8 is the source-level amount); otherwise approximate vanilla's {@code LightEngine}
     * calculation by reading the block's own light-occlusion: anything less than the world's
     * max light level lets enough light through.
     */
    static boolean canBeGrass(BlockState state, ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.is(net.minecraft.world.level.block.Blocks.SNOW) && aboveState.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (aboveState.getFluidState().getAmount() == SOURCE_FLUID_AMOUNT) {
            return false;
        }
        return aboveState.getLightBlock(level, above) < level.getMaxLightLevel();
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canBeGrass(state, level, pos)) {
            // Light blocked above for too long — die back to plain matching-colour dirt.
            if (level.isAreaLoaded(pos, 3)) {
                level.setBlockAndUpdate(pos, matchingDirt.get().defaultBlockState());
            }
            return;
        }

        if (level.getMaxLocalRawBrightness(pos.above()) < GRASS_LIGHT_THRESHOLD) {
            return;
        }

        BlockState grassState = this.defaultBlockState();
        for (int attempt = 0; attempt < SPREAD_ATTEMPTS; attempt++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            BlockState targetState = level.getBlockState(target);
            if (!targetState.is(matchingDirt.get())) {
                continue;
            }
            if (canPropagate(grassState, level, target)) {
                level.setBlockAndUpdate(target, grassState);
            }
        }
    }

    /** Whether grass can replace the dirt block at {@code pos}: needs ample light and no liquid above. */
    private static boolean canPropagate(BlockState grassState, ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        FluidState aboveFluid = level.getFluidState(above);
        // Use the FluidTags.WATER tag rather than Fluids.WATER directly — the latter only
        // matches still water, so flowing water above the dirt would silently slip through and
        // let grass spread under a stream. The vanilla water tag covers both Fluids.WATER and
        // Fluids.FLOWING_WATER, plus any modded fluid that opts into "is water" semantics.
        if (aboveFluid.is(FluidTags.WATER)) {
            return false;
        }
        if (level.getRawBrightness(above, 0) < GRASS_DEATH_LIGHT_THRESHOLD && level.getBrightness(LightLayer.SKY, above) < GRASS_DEATH_LIGHT_THRESHOLD) {
            return false;
        }
        return canBeGrass(grassState, level, pos);
    }

    @Override
    public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        // Bonemeal-grown grass tufts/flowers are a follow-up; mark not-valid for now so vanilla
        // doesn't pop a particle effect that has no in-world result. The AC for SMTCON-54
        // covers "bonemeal grows saplings" via SlimeSaplingBlock, not grass.
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        // No-op — see isValidBonemealTarget. The interface mandate is to override.
    }

    /** Read the matching slime dirt block this grass spreads onto. Public for test seams and for downstream code that wants to query the pair (e.g. a worldgen feature reading the dirt block to seed a layer). */
    public Block matchingDirt() {
        return matchingDirt.get();
    }

    /** Test seam: snapshot the surfaces used by tests so a future redesign can rename in one place. */
    static List<Integer> spreadConstants() {
        return List.of(GRASS_LIGHT_THRESHOLD, GRASS_DEATH_LIGHT_THRESHOLD, SPREAD_ATTEMPTS);
    }
}
