package slimeknights.sconstruct.port1211.smeltery;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;

/**
 * Phase-5 molten-metal fluids: all 20 {@link MoltenMetals#ALL} entries registered as full
 * {@link MoltenFluidSet} bundles via the shared {@link #moltenFluid} helper. The helper applies
 * the Phase-3 {@code WorldFluids#slimeFluid} pattern to molten metals — it centralises the
 * five-way circular wiring between {@link FluidType}, {@link BaseFlowingFluid.Source} /
 * {@link BaseFlowingFluid.Flowing}, {@link LiquidBlock}, and {@link BucketItem} so the whole
 * roster is registered by one static block iterating the {@link MoltenMetal} driver table.
 *
 * <p>Each fluid's registry path is {@code molten_<metal id>} (the legacy {@code TinkerFluids}
 * {@code molten_} naming convention), so {@code molten_iron}, {@code molten_obsidian}, etc.
 * {@link FluidType} properties are read straight off the {@link MoltenMetal}: temperature and
 * light level from the driver, density from the driver, motion scale a fixed {@code 0.0023}
 * (the slow, treacle-like drift the legacy molten metals had).
 *
 * <p>The 20 sets are exposed via the insertion-ordered {@link #ALL} list, the
 * {@link #MOLTEN} lookup map keyed by {@link MoltenMetal}, and the {@link #TINTS} map (an
 * {@code 0xAARRGGBB} value per set) that the SMTCON-110 client fluid-type extensions registrar
 * consumes to bind each fluid to its surface tint.
 *
 * <p>{@link #init()} forces this class to load during mod construction so the field
 * initialisers run and the {@link TinkerRegistries} {@code DeferredRegister}s see every entry
 * before their registry events fire. It is invoked from {@code SConstruct} for now; the call
 * relocates into the smeltery pulse when SMTCON-131 wires it.
 */
public final class SmelteryFluids {

    /** Motion scale shared by every molten metal — a slow, treacle-like drift. */
    private static final double MOTION_SCALE = 0.0023;

    private static final Map<MoltenMetal, MoltenFluidSet> MOLTEN_BUILDER = new LinkedHashMap<>();
    private static final Map<MoltenFluidSet, Integer> TINT_BUILDER = new LinkedHashMap<>();

    static {
        for (MoltenMetal metal : MoltenMetals.ALL) {
            moltenFluid(metal);
        }
    }

    /**
     * Every molten-metal fluid set, keyed by its {@link MoltenMetal} driver entry, in
     * {@link MoltenMetals#ALL} order. Downstream providers look a set up by metal rather than by
     * a named static field.
     */
    public static final Map<MoltenMetal, MoltenFluidSet> MOLTEN = Collections.unmodifiableMap(MOLTEN_BUILDER);

    /** Immutable insertion-ordered view over every registered molten fluid set. */
    public static final List<MoltenFluidSet> ALL = List.copyOf(MOLTEN_BUILDER.values());

    /**
     * Immutable mapping from {@link MoltenFluidSet} to its {@code 0xAARRGGBB} surface tint, in
     * {@link #ALL} order. Consumed by the SMTCON-110 client fluid-type extensions registrar so
     * it does not have to re-derive the colour from the {@link MoltenMetal} driver.
     */
    public static final Map<MoltenFluidSet, Integer> TINTS = Collections.unmodifiableMap(TINT_BUILDER);

    private SmelteryFluids() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the static block and
        // field-initialiser chain above. Same pattern as {@code WorldFluids#init()}.
    }

    /**
     * Look up the registered {@link MoltenFluidSet} for a {@link MoltenMetal}. Never returns
     * {@code null} — every {@link MoltenMetals#ALL} entry is registered by the static block, so
     * an absent value means class loading regressed and is worth surfacing loudly.
     */
    public static MoltenFluidSet get(MoltenMetal metal) {
        Objects.requireNonNull(metal, "metal");
        MoltenFluidSet set = MOLTEN.get(metal);
        if (set == null) {
            throw new IllegalStateException("SmelteryFluids.MOLTEN is missing an entry for " + metal.id() + " — class loading order regressed");
        }
        return set;
    }

    /**
     * Register a full {@link MoltenFluidSet} for one {@link MoltenMetal}. The five components
     * form a circular reference graph (Fluid → Block, Block → Fluid, Fluid → Bucket, Bucket →
     * Fluid, Fluid → Type); the cycle is broken exactly as {@code WorldFluids#slimeFluid} does —
     * the source / flowing / block / bucket holders are wrapped in single-element arrays and
     * read through a {@link Supplier} that resolves only when the
     * {@link BaseFlowingFluid.Properties} object is built inside each fluid's registration
     * lambda, after all four {@link DeferredHolder}s have been assigned.
     */
    private static MoltenFluidSet moltenFluid(MoltenMetal metal) {
        String id = "molten_" + metal.id();
        DeferredHolder<FluidType, FluidType> type = TinkerRegistries.FLUID_TYPES.register(id, () -> new FluidType(FluidType.Properties.create().temperature(metal.temperature())
                .lightLevel(metal.luminosity()).density(metal.density()).motionScale(MOTION_SCALE).descriptionId("fluid.sconstruct." + id)));

        // Single-element array holders break the circular reference between the four
        // post-FluidType registrations: the Properties supplier reads through them at
        // registry-event time, by which point each holder has been assigned.
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
        // lightLevel on the block matches the fluid's luminosity so a pool of molten metal lights
        // its surroundings the way lava does; the fixed strength / DESTROY push reaction mirror
        // the slime-fluid block so molten metal cannot be pistoned or cheaply mined.
        blockRef[0] = TinkerRegistries.BLOCKS.register(id, () -> new LiquidBlock(sourceRef[0].get(), BlockBehaviour.Properties.of().mapColor(metal.mapColor()).replaceable().noCollission()
                .strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid().lightLevel(state -> metal.luminosity()).sound(SoundType.EMPTY)));
        bucketRef[0] = TinkerRegistries.ITEMS.registerItem(id + "_bucket", props -> new BucketItem(sourceRef[0].get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));

        MoltenFluidSet set = new MoltenFluidSet(type, sourceRef[0], flowingRef[0], blockRef[0], bucketRef[0]);
        MOLTEN_BUILDER.put(metal, set);
        // Opaque 0xAARRGGBB — the 24-bit driver tint with a full-alpha byte, the form the client
        // fluid-type extension expects.
        TINT_BUILDER.put(set, 0xFF000000 | metal.tint());
        return set;
    }
}
