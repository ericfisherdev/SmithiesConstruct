package slimeknights.tconstruct.port1211.shared;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.tconstruct.port1211.common.TinkerRegistries;

/**
 * First fluid in the port: blood. Establishes the four-part pattern that Phase 3 slime fluids
 * and Phase 5 molten metals will scale up:
 *
 * <ol>
 *   <li>{@link FluidType} — physical properties (density, viscosity, temperature, lang key).
 *       Registered against {@link TinkerRegistries#FLUID_TYPES}.</li>
 *   <li>{@link BaseFlowingFluid.Source} + {@link BaseFlowingFluid.Flowing} — the static-source
 *       and flowing-stream {@link Fluid} pair. Registered against {@link TinkerRegistries#FLUIDS};
 *       wired together via a shared {@link BaseFlowingFluid.Properties} that points each at
 *       the other plus the type/block/bucket.</li>
 *   <li>{@link LiquidBlock} — the in-world placement of the source fluid. Registered against
 *       {@link TinkerRegistries#BLOCKS}. Uses {@link PushReaction#DESTROY} + {@code noLootTable}
 *       so a piston push or block break disposes of the fluid rather than dropping an item.</li>
 *   <li>{@link net.minecraft.world.item.BucketItem} — the filled bucket. Registered against
 *       {@link TinkerRegistries#ITEMS} (see {@link SharedItems#BUCKET_BLOOD}) with
 *       {@code craftRemainder(Items.BUCKET)} and {@code stacksTo(1)} so emptying it returns the
 *       vanilla bucket and stacking is forbidden.</li>
 * </ol>
 *
 * <p>The four registrations form a circular reference (Fluid → Block, Block → Fluid, Fluid →
 * Bucket, Bucket → Fluid). The cycle is broken by passing {@link DeferredHolder} suppliers,
 * not resolved values, into {@link BaseFlowingFluid.Properties}: the suppliers only fire
 * after all four registries have processed their events. Block and Item registries fire after
 * Fluid in NeoForge's registry order, so the {@code BLOOD.get()} call inside the
 * {@link LiquidBlock}/{@code BucketItem} lambdas resolves to the constructed source fluid.
 *
 * <p>Density {@code 1100} / viscosity {@code 1500} / temperature {@code 310} match the legacy
 * 1.12 {@code FluidColored("blood", 0x540000)} values that the smeltery used to flow-rate
 * blood at room temperature; pinning them here keeps Phase 5 smeltery interaction identical
 * to the 1.12 build.
 *
 * <p>{@link #init()} forces this class to load during mod construction so the field
 * initialisers run and the DeferredRegisters see every entry before their registry events fire.
 */
public final class SharedFluids {

    public static final DeferredHolder<FluidType, FluidType> BLOOD_TYPE = TinkerRegistries.FLUID_TYPES.register("blood",
            () -> new FluidType(FluidType.Properties.create().density(1100).viscosity(1500).temperature(310).descriptionId("fluid.tconstruct.blood")));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BLOOD = TinkerRegistries.FLUIDS.register("blood", () -> new BaseFlowingFluid.Source(bloodProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> BLOOD_FLOWING = TinkerRegistries.FLUIDS.register("flowing_blood", () -> new BaseFlowingFluid.Flowing(bloodProperties()));

    public static final DeferredBlock<LiquidBlock> BLOOD_BLOCK = TinkerRegistries.BLOCKS.register("blood", () -> new LiquidBlock(BLOOD.get(), BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY)));

    private SharedFluids() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. Same pattern as {@link SharedBlocks#init()}.
    }

    /**
     * Builds a fresh {@link BaseFlowingFluid.Properties} pointing at the type, source, flowing,
     * block, and bucket. Called from {@link #BLOOD}/{@link #BLOOD_FLOWING}'s factories at
     * registry-event time, after every referenced field has been initialised. Returning a new
     * instance per call mirrors how NeoForge's example mods do it; the Properties object is
     * consumed by the Fluid constructor and not retained.
     */
    private static BaseFlowingFluid.Properties bloodProperties() {
        return new BaseFlowingFluid.Properties(BLOOD_TYPE, BLOOD, BLOOD_FLOWING).block(BLOOD_BLOCK).bucket(SharedItems.BUCKET_BLOOD);
    }
}
