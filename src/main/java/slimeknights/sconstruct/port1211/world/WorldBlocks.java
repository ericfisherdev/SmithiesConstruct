package slimeknights.sconstruct.port1211.world;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;
import slimeknights.sconstruct.port1211.world.block.SlimeDirtBlock;
import slimeknights.sconstruct.port1211.world.block.SlimeGrassBlock;
import slimeknights.sconstruct.port1211.world.block.SlimeLeavesBlock;
import slimeknights.sconstruct.port1211.world.block.SlimePlantSet;
import slimeknights.sconstruct.port1211.world.block.SlimeSaplingBlock;
import slimeknights.sconstruct.port1211.world.block.TinkerSlimeBlock;

/**
 * Phase-3 world blocks: the four coloured slime blocks plus the matching four-block plant set
 * (dirt + grass + leaves + sapling) per colour. The {@link #slimeBlock} helper centralises the
 * bouncy-block boilerplate; the {@link #slimePlantSet} helper centralises the plant-life
 * registrations so each colour is one extra line plus four texture files.
 *
 * <p><strong>Bouncy slime blocks:</strong> friction {@code 0.8}, slime-block sound, speed
 * factor {@code 0.4}, jump factor {@code 0.5}. Per-colour behaviour ({@link
 * SlimeColor#applyFallEffect}) varies: magma damages on landing, blood heals on landing,
 * blue/purple are vanilla-equivalent. {@code jumpFactor 0.5} deliberately deviates from vanilla
 * so the legacy compressed-slime trampoline feel survives.
 *
 * <p><strong>Plant set:</strong> dirt is a plain coloured Block; grass spreads to matching dirt
 * via {@link SlimeGrassBlock}; leaves inherit vanilla {@link
 * net.minecraft.world.level.block.LeavesBlock} decay; sapling carries a placeholder {@link
 * net.minecraft.world.level.block.grower.TreeGrower} until SMTCON-55 lands the configured
 * features. Each plant block ships with a matching {@code BlockItem}.
 *
 * <p>{@link #SLIME_BLOCKS} (bouncy blocks) and {@link #PLANT_SETS} (plant sets) expose the
 * registrations keyed by {@link SlimeColor}. {@link #ALL} flattens the bouncy blocks; {@link
 * #ALL_PLANTS} flattens every plant block across every colour. Downstream providers (lang,
 * blockstate, item model, creative tab, loot, tags) iterate these collections so a new colour
 * lights up every consumer with no provider edit.
 *
 * <p>{@link #init()} forces this class to load during mod construction so the field
 * initialisers run and the {@link TinkerRegistries#BLOCKS}/{@code ITEMS} registers see every
 * entry before their registry events fire.
 */
public final class WorldBlocks {

    private static final Map<SlimeColor, DeferredBlock<TinkerSlimeBlock>> SLIME_BUILDER = new LinkedHashMap<>();
    private static final Map<SlimeColor, SlimePlantSet> PLANT_BUILDER = new LinkedHashMap<>();

    public static final DeferredBlock<TinkerSlimeBlock> SLIMEBLUE = slimeBlock(SlimeColor.BLUE);
    public static final DeferredBlock<TinkerSlimeBlock> SLIMEPURPLE = slimeBlock(SlimeColor.PURPLE);
    public static final DeferredBlock<TinkerSlimeBlock> SLIMEMAGMA = slimeBlock(SlimeColor.MAGMA);
    public static final DeferredBlock<TinkerSlimeBlock> SLIMEBLOOD = slimeBlock(SlimeColor.BLOOD);

    public static final SlimePlantSet PLANTS_BLUE = slimePlantSet(SlimeColor.BLUE);
    public static final SlimePlantSet PLANTS_PURPLE = slimePlantSet(SlimeColor.PURPLE);
    public static final SlimePlantSet PLANTS_MAGMA = slimePlantSet(SlimeColor.MAGMA);
    public static final SlimePlantSet PLANTS_BLOOD = slimePlantSet(SlimeColor.BLOOD);

    /** Immutable view over the four bouncy slime-block holders keyed by {@link SlimeColor}. */
    public static final Map<SlimeColor, DeferredBlock<TinkerSlimeBlock>> SLIME_BLOCKS;

    /** Immutable view over the four plant sets keyed by {@link SlimeColor}. */
    public static final Map<SlimeColor, SlimePlantSet> PLANT_SETS;

    /** Insertion-ordered list view of the bouncy slime blocks — matches {@link #SLIME_BLOCKS} order. */
    public static final List<DeferredBlock<TinkerSlimeBlock>> ALL;

    /**
     * Insertion-ordered list view of <em>every</em> plant block (4 colours × 4 plant types =
     * 16 entries). Order: blue dirt → blue grass → blue leaves → blue sapling → purple dirt
     * → … Useful for datagen providers that need to enumerate every plant block uniformly.
     */
    public static final List<DeferredBlock<? extends Block>> ALL_PLANTS;

    static {
        SLIME_BLOCKS = Collections.unmodifiableMap(new LinkedHashMap<>(SLIME_BUILDER));
        ALL = List.copyOf(SLIME_BUILDER.values());
        PLANT_SETS = Collections.unmodifiableMap(new LinkedHashMap<>(PLANT_BUILDER));

        java.util.List<DeferredBlock<? extends Block>> plantsBuilder = new java.util.ArrayList<>();
        for (SlimePlantSet set : PLANT_BUILDER.values()) {
            plantsBuilder.addAll(set.all());
        }
        ALL_PLANTS = List.copyOf(plantsBuilder);
    }

    private WorldBlocks() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. Same pattern as {@link WorldFluids#init()}.
    }

    /** Visits each bouncy slime block's {@link ItemLike} in declaration order. */
    public static void acceptBlockItems(Consumer<ItemLike> accept) {
        for (DeferredBlock<TinkerSlimeBlock> block : ALL) {
            accept.accept(block.get());
        }
    }

    /**
     * Visits each plant block's {@link ItemLike} (dirt, grass, leaves, sapling) per colour in
     * declaration order. Used by the world pulse's creative-tab listener so the plant blocks
     * land in the same tab as the bouncy slime blocks.
     */
    public static void acceptPlantItems(Consumer<ItemLike> accept) {
        for (DeferredBlock<? extends Block> block : ALL_PLANTS) {
            accept.accept(block.get());
        }
    }

    private static DeferredBlock<TinkerSlimeBlock> slimeBlock(SlimeColor color) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(color.mapColor()).sound(SoundType.SLIME_BLOCK).friction(0.8F).speedFactor(0.4F).jumpFactor(0.5F).noOcclusion();
        DeferredBlock<TinkerSlimeBlock> block = TinkerRegistries.BLOCKS.register("slime_" + color.id() + "_block", () -> new TinkerSlimeBlock(color, properties));
        TinkerRegistries.ITEMS.registerSimpleBlockItem(block);
        SLIME_BUILDER.put(color, block);
        return block;
    }

    /**
     * Register the four plant-life blocks for a single slime colour. Dirt registers first so
     * grass can reference it via supplier; grass's spread target is the matching colour's dirt
     * block. Leaves and sapling are independent.
     */
    private static SlimePlantSet slimePlantSet(SlimeColor color) {
        BlockBehaviour.Properties dirtProperties = BlockBehaviour.Properties.of().mapColor(color.mapColor()).sound(SoundType.GRAVEL).strength(0.5F).randomTicks();
        DeferredBlock<SlimeDirtBlock> dirt = TinkerRegistries.BLOCKS.register("slime_" + color.id() + "_dirt", () -> new SlimeDirtBlock(color, dirtProperties));
        TinkerRegistries.ITEMS.registerSimpleBlockItem(dirt);

        BlockBehaviour.Properties grassProperties = BlockBehaviour.Properties.of().mapColor(color.mapColor()).sound(SoundType.GRASS).strength(0.6F).randomTicks();
        DeferredBlock<SlimeGrassBlock> grass = TinkerRegistries.BLOCKS.register("slime_" + color.id() + "_grass", () -> new SlimeGrassBlock(color, dirt::get, grassProperties));
        TinkerRegistries.ITEMS.registerSimpleBlockItem(grass);

        BlockBehaviour.Properties leavesProperties = BlockBehaviour.Properties.of().mapColor(color.mapColor()).sound(SoundType.GRASS).strength(0.2F).randomTicks().noOcclusion()
                .isValidSpawn(WorldBlocks::neverSpawn).isSuffocating(WorldBlocks::neverSuffocating).isViewBlocking(WorldBlocks::neverViewBlocking).ignitedByLava().pushReaction(PushReaction.DESTROY);
        DeferredBlock<SlimeLeavesBlock> leaves = TinkerRegistries.BLOCKS.register("slime_" + color.id() + "_leaves", () -> new SlimeLeavesBlock(color, leavesProperties));
        TinkerRegistries.ITEMS.registerSimpleBlockItem(leaves);

        BlockBehaviour.Properties saplingProperties = BlockBehaviour.Properties.of().mapColor(color.mapColor()).sound(SoundType.GRASS).strength(0.0F).noCollission().randomTicks()
                .pushReaction(PushReaction.DESTROY);
        DeferredBlock<SlimeSaplingBlock> sapling = TinkerRegistries.BLOCKS.register("slime_" + color.id() + "_sapling", () -> new SlimeSaplingBlock(color, saplingProperties));
        TinkerRegistries.ITEMS.registerSimpleBlockItem(sapling);

        SlimePlantSet set = new SlimePlantSet(dirt, grass, leaves, sapling);
        PLANT_BUILDER.put(color, set);
        return set;
    }

    /** Block-behaviour predicate: no entity is ever a valid spawn on this block. */
    private static boolean neverSpawn(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.EntityType<?> type) {
        return false;
    }

    /** Block-behaviour predicate: leaves never suffocate entities; lambda is a typed reference for the Properties API. */
    private static boolean neverSuffocating(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        return false;
    }

    /** Block-behaviour predicate: leaves never block view (used by skylight propagation). */
    private static boolean neverViewBlocking(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        return false;
    }
}
