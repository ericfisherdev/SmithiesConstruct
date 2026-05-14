package slimeknights.sconstruct.port1211.world;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * Phase-3 worldgen features for slime trees: one {@link ConfiguredFeature} +
 * {@link PlacedFeature} per slime colour, plus the {@link ResourceKey} pairs the
 * {@link slimeknights.sconstruct.port1211.world.block.SlimeSaplingBlock} {@link TreeGrower}
 * references. Trees are sapling-driven, not natural — {@link PlacedFeature}s here use a
 * minimal placement modifier list so they only fire when a sapling explicitly grows them.
 *
 * <p>Tree shape per colour: 5-block-tall {@link StraightTrunkPlacer} trunk over a
 * 2-radius {@link BlobFoliagePlacer} canopy. Sized to match the AC's "4-6 block trunk, leaves
 * canopy" surface, picked midway through the range so the canopy hangs over the trunk's top
 * three blocks. Trunk uses the matching {@link WorldBlocks#SLIME_LOGS} log; foliage uses the
 * matching {@link slimeknights.sconstruct.port1211.world.block.SlimePlantSet#leaves()} block.
 *
 * <p>{@link #bootstrapConfigured} and {@link #bootstrapPlaced} are wired into
 * {@code DataGenerators#registerDatapackEntries} so {@code ./gradlew runData} emits the
 * matching {@code data/sconstruct/worldgen/configured_feature/slime_<color>_tree.json} and
 * {@code data/sconstruct/worldgen/placed_feature/slime_<color>_tree.json}.
 */
public final class WorldFeatures {

    /** Trunk height block-count — matches vanilla oak (5). */
    private static final int TRUNK_BLOCKS = 5;

    /** Foliage horizontal radius — matches vanilla oak (2). */
    private static final int FOLIAGE_RADIUS = 2;

    /** Foliage vertical extent above the trunk top — matches vanilla oak (3). */
    private static final int FOLIAGE_HEIGHT = 3;

    /** Two-layers feature size: trunk-top extent for the foliage layer (vanilla oak: 1/0/1). */
    private static final int FOLIAGE_LAYER_LIMIT = 1;

    private static final Map<SlimeColor, ResourceKey<ConfiguredFeature<?, ?>>> CONFIGURED_KEYS = buildConfiguredKeys();
    private static final Map<SlimeColor, ResourceKey<PlacedFeature>> PLACED_KEYS = buildPlacedKeys();

    private WorldFeatures() {
    }

    /** Configured-feature key for the slime tree of the supplied colour. */
    public static ResourceKey<ConfiguredFeature<?, ?>> configuredTreeKey(SlimeColor color) {
        return CONFIGURED_KEYS.get(color);
    }

    /** Placed-feature key for the slime tree of the supplied colour. */
    public static ResourceKey<PlacedFeature> placedTreeKey(SlimeColor color) {
        return PLACED_KEYS.get(color);
    }

    /**
     * Write one {@link ConfiguredFeature} per slime colour to the supplied bootstrap context.
     * Each entry binds a {@link Feature#TREE} configuration whose trunk and foliage block-state
     * providers point at the matching {@link WorldBlocks#SLIME_LOGS} / leaves block.
     */
    public static void bootstrapConfigured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        for (SlimeColor color : SlimeColor.values()) {
            Block trunkBlock = WorldBlocks.SLIME_LOGS.get(color).get();
            Block foliageBlock = WorldBlocks.PLANT_SETS.get(color).leaves().get();
            TreeConfiguration config = new TreeConfiguration.TreeConfigurationBuilder(BlockStateProvider.simple(trunkBlock), new StraightTrunkPlacer(TRUNK_BLOCKS, 0, 0),
                    BlockStateProvider.simple(foliageBlock), new BlobFoliagePlacer(ConstantInt.of(FOLIAGE_RADIUS), ConstantInt.of(0), FOLIAGE_HEIGHT),
                    new TwoLayersFeatureSize(FOLIAGE_LAYER_LIMIT, 0, FOLIAGE_LAYER_LIMIT)).ignoreVines().build();
            context.register(configuredTreeKey(color), new ConfiguredFeature<>(Feature.TREE, config));
        }
    }

    /**
     * Write one {@link PlacedFeature} per slime colour. Trees are sapling-driven so the
     * placement-modifier list is empty — vanilla {@code TreeGrower} pipes the configured
     * feature through {@link Feature#TREE#place} directly without consulting placement filters.
     */
    public static void bootstrapPlaced(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        for (SlimeColor color : SlimeColor.values()) {
            context.register(placedTreeKey(color), new PlacedFeature(configured.getOrThrow(configuredTreeKey(color)), List.of(PlacementUtils.HEIGHTMAP)));
        }
    }

    private static Map<SlimeColor, ResourceKey<ConfiguredFeature<?, ?>>> buildConfiguredKeys() {
        // EnumMap as the implementation choice (compact + iteration order matches enum
        // declaration) but the returned Map exposes only the interface — keeps the call site
        // decoupled and silences PMD's LooseCoupling check.
        Map<SlimeColor, ResourceKey<ConfiguredFeature<?, ?>>> map = new java.util.EnumMap<>(SlimeColor.class);
        for (SlimeColor color : SlimeColor.values()) {
            map.put(color, ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "slime_" + color.id() + "_tree")));
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    private static Map<SlimeColor, ResourceKey<PlacedFeature>> buildPlacedKeys() {
        Map<SlimeColor, ResourceKey<PlacedFeature>> map = new java.util.EnumMap<>(SlimeColor.class);
        for (SlimeColor color : SlimeColor.values()) {
            map.put(color, ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "slime_" + color.id() + "_tree")));
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    /** Visible-for-tests hook: the {@link OptionalInt} arguments StraightTrunkPlacer was built with. Lets tests pin trunk height without re-running the bootstrap. */
    static List<Integer> trunkSpec() {
        return List.of(TRUNK_BLOCKS, FOLIAGE_RADIUS, FOLIAGE_HEIGHT);
    }
}
