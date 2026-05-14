package slimeknights.sconstruct.port1211.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
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
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;

/**
 * Phase-3 slime fluids: four coloured variants (blue, purple, magma, blood) registered as full
 * {@link SlimeFluidSet} bundles via the shared {@link #slimeFluid} helper. The helper centralises
 * the five-way circular wiring between {@link FluidType}, {@link BaseFlowingFluid.Source} /
 * {@link BaseFlowingFluid.Flowing}, {@link LiquidBlock}, and {@link BucketItem} that the legacy
 * Phase-2 blood fluid in {@code SharedFluids} unrolls by hand — adding a new slime colour is now
 * a single line in this file rather than five field declarations plus a properties factory.
 *
 * <p>Density {@code 1400} / viscosity {@code 2000} are tuned higher than blood ({@code 1100} /
 * {@code 1500}) to give slime fluids a noticeably gloopier flow rate at room temperature — the
 * "slimey" feel the Phase-3 slime-island theming asks for. Tints are sourced from the legacy
 * {@code SlimeColorizer} constants so existing addons keying on the upstream colour palette stay
 * compatible.
 *
 * <p>The four sets are exposed both as named static fields ({@link #SLIMEBLUE} etc.) and via the
 * insertion-ordered {@link #ALL} list / {@link #TINTS} map. Downstream providers (creative-tab
 * listener, lang provider, client fluid-type extensions) iterate the list rather than reaching
 * for the named fields so a new colour lights up every consumer by appending one
 * {@link #slimeFluid} call.
 *
 * <p>{@link #init()} forces this class to load during mod construction so the field
 * initialisers run and the {@link TinkerRegistries} {@code DeferredRegister}s see every entry
 * before their registry events fire.
 */
public final class WorldFluids {

    /** Tint applied to both the bucket sprite and the in-world fluid surface, {@code 0xAARRGGBB}. */
    public static final int SLIMEBLUE_TINT = 0xFF2AEC81;
    public static final int SLIMEPURPLE_TINT = 0xFFA92DFF;
    public static final int SLIMEMAGMA_TINT = 0xFFD09800;
    public static final int SLIMEBLOOD_TINT = 0xFF6F0000;

    private static final List<SlimeFluidSet> SLIME_FLUID_BUILDER = new ArrayList<>();
    private static final Map<SlimeFluidSet, Integer> TINT_BUILDER = new LinkedHashMap<>();

    public static final SlimeFluidSet SLIMEBLUE = slimeFluid("slime_blue", MapColor.COLOR_LIGHT_BLUE, SLIMEBLUE_TINT);
    public static final SlimeFluidSet SLIMEPURPLE = slimeFluid("slime_purple", MapColor.COLOR_PURPLE, SLIMEPURPLE_TINT);
    public static final SlimeFluidSet SLIMEMAGMA = slimeFluid("slime_magma", MapColor.COLOR_ORANGE, SLIMEMAGMA_TINT);
    public static final SlimeFluidSet SLIMEBLOOD = slimeFluid("slime_blood", MapColor.COLOR_RED, SLIMEBLOOD_TINT);

    /**
     * Immutable insertion-ordered view over every registered slime fluid. Downstream providers
     * iterate this list instead of the named static fields so appending a new colour above
     * lights up every consumer (lang, tags, recipes, creative tab, client extensions) without
     * editing each one.
     */
    public static final List<SlimeFluidSet> ALL = List.copyOf(SLIME_FLUID_BUILDER);

    /**
     * Immutable mapping from {@link SlimeFluidSet} to its {@code 0xAARRGGBB} tint, in the same
     * order as {@link #ALL}. Consumed by the client fluid-type extensions registrar to bind each
     * fluid type to its colour.
     */
    public static final Map<SlimeFluidSet, Integer> TINTS = Collections.unmodifiableMap(TINT_BUILDER);

    private WorldFluids() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. Same pattern as {@code SharedFluids#init()}.
    }

    /**
     * Visits the four slime-bucket {@link ItemLike}s in declaration order. Used by the world
     * pulse's {@code BuildCreativeModeTabContentsEvent} listener so the buckets land in the same
     * tab as the rest of the mod's content without coupling the pulse to a specific tab key
     * lookup.
     */
    public static void acceptBuckets(Consumer<ItemLike> accept) {
        for (SlimeFluidSet set : ALL) {
            accept.accept(set.bucket().get());
        }
    }

    /**
     * Register a full {@link SlimeFluidSet} for a single slime colour. The five components form
     * a circular reference graph (Fluid → Block, Block → Fluid, Fluid → Bucket, Bucket → Fluid,
     * Fluid → Type); the cycle is broken by wrapping the source/flowing/block/bucket holders in
     * single-element arrays and reading them through a {@link Supplier} that resolves only when
     * the {@link BaseFlowingFluid.Properties} object is built inside each fluid's registration
     * lambda — after all four {@link DeferredHolder} entries have been added to their registers.
     *
     * <p>NeoForge processes registries in the order Fluid → Block → Item, so by the time the
     * {@link LiquidBlock} factory lambda fires the {@code source} holder resolves to the
     * constructed source fluid; the {@link BucketItem} factory likewise sees the source fluid by
     * the time {@link net.minecraft.core.registries.Registries#ITEM} fires.
     *
     * @param id   registry path under {@code sconstruct:} — used verbatim for the FluidType, the
     *             source fluid, the LiquidBlock, and (with {@code "_bucket"} suffix) the bucket
     *             item; the flowing fluid uses {@code "flowing_" + id}
     * @param color map colour for the LiquidBlock surface (the colour shown on cartography
     *              tables; the in-world fluid surface tint is controlled separately by the
     *              client fluid-type extension)
     * @param tint  {@code 0xAARRGGBB} tint applied by the client fluid-type extension to both
     *              the bucket sprite and the in-world fluid surface; stored in {@link #TINTS}
     *              so the client registrar can read it without duplicating the value
     */
    private static SlimeFluidSet slimeFluid(String id, MapColor color, int tint) {
        DeferredHolder<FluidType, FluidType> type = TinkerRegistries.FLUID_TYPES.register(id,
                () -> new FluidType(FluidType.Properties.create().density(1400).viscosity(2000).descriptionId("fluid.sconstruct." + id)));

        // Single-element array holders break the circular reference between the four
        // post-FluidType registrations: the Properties supplier reads through them at
        // registry-event time, by which point each holder has been assigned its registered
        // DeferredHolder.
        @SuppressWarnings("unchecked")
        DeferredHolder<Fluid, BaseFlowingFluid.Source>[] sourceRef = new DeferredHolder[1];
        @SuppressWarnings("unchecked")
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing>[] flowingRef = new DeferredHolder[1];
        @SuppressWarnings("unchecked")
        DeferredBlock<LiquidBlock>[] blockRef = (DeferredBlock<LiquidBlock>[]) new DeferredBlock[1];
        @SuppressWarnings("unchecked")
        DeferredItem<BucketItem>[] bucketRef = (DeferredItem<BucketItem>[]) new DeferredItem[1];

        Supplier<BaseFlowingFluid.Properties> properties = () -> new BaseFlowingFluid.Properties(type, sourceRef[0], flowingRef[0]).block(blockRef[0]).bucket(bucketRef[0]);

        sourceRef[0] = TinkerRegistries.FLUIDS.register(id, () -> new BaseFlowingFluid.Source(properties.get()));
        flowingRef[0] = TinkerRegistries.FLUIDS.register("flowing_" + id, () -> new BaseFlowingFluid.Flowing(properties.get()));
        blockRef[0] = TinkerRegistries.BLOCKS.register(id, () -> new LiquidBlock(sourceRef[0].get(),
                BlockBehaviour.Properties.of().mapColor(color).replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.SLIME_BLOCK)));
        bucketRef[0] = TinkerRegistries.ITEMS.registerItem(id + "_bucket", props -> new BucketItem(sourceRef[0].get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));

        SlimeFluidSet set = new SlimeFluidSet(type, sourceRef[0], flowingRef[0], blockRef[0], bucketRef[0]);
        SLIME_FLUID_BUILDER.add(set);
        TINT_BUILDER.put(set, tint);
        return set;
    }
}
