package slimeknights.sconstruct.port1211.world;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;
import slimeknights.sconstruct.port1211.world.structure.SlimeIslandPiece;
import slimeknights.sconstruct.port1211.world.structure.SlimeIslandStructure;

/**
 * Phase-3 worldgen structure registrations — both the code-side {@link StructureType} /
 * {@link StructurePieceType} handles and the datapack-side bootstrap methods that emit the
 * {@link Structure}, {@link StructureSet}, and {@link BiomeModifier} JSONs during {@code
 * runData}.
 *
 * <ul>
 *   <li>{@link #SLIME_ISLAND_TYPE} — the {@link StructureType} bound to
 *       {@link SlimeIslandStructure#CODEC}; one type covers every colour because the
 *       per-colour difference is encoded as a field on each datapack JSON instance.</li>
 *   <li>{@link #SLIME_ISLAND_PIECE} — the {@link StructurePieceType} that lets the chunk
 *       reload path re-instantiate a {@link SlimeIslandPiece} from its NBT after a save/load
 *       cycle (the {@code context, tag} ctor is the deserialiser).</li>
 *   <li>{@link #bootstrapStructures} — registers one {@link Structure} per slime colour at
 *       {@code sconstruct:slime_island_<colour>}. Each instance pins the matching
 *       {@link SlimeColor} on its codec field and inherits a shared {@link
 *       Structure.StructureSettings} targeting {@link BiomeTags#IS_OVERWORLD}.</li>
 *   <li>{@link #bootstrapStructureSets} — registers {@code sconstruct:slime_islands} as a
 *       single {@link StructureSet} containing all four colour variants. Placement is
 *       {@link RandomSpreadStructurePlacement} tuned 50% rarer than the original ticket spec
 *       (spacing {@link #SPACING}, separation {@link #SEPARATION}) so the islands don't
 *       saturate the overworld.</li>
 *   <li>{@link #bootstrapBiomeModifiers} — registers {@code sconstruct:slime_mob_spawns} as
 *       a {@link BiomeModifiers.AddSpawnsBiomeModifier} that adds blueslime + hugeslime
 *       spawn entries to {@link BiomeTags#IS_OVERWORLD}. The biome-tag refinement (per the
 *       ticket's open question 12) is a future follow-up; targeting the broad overworld tag
 *       today lets {@code /summon} keep working and lets the mobs surface on islands.</li>
 * </ul>
 *
 * <p>{@link #init()} forces this class to load so the {@code DeferredHolder} static
 * initialisers populate {@link TinkerRegistries#STRUCTURE_TYPES} and
 * {@link TinkerRegistries#STRUCTURE_PIECE_TYPES} before the matching registry events fire.
 */
public final class WorldStructures {

    public static final DeferredHolder<StructureType<?>, StructureType<SlimeIslandStructure>> SLIME_ISLAND_TYPE = TinkerRegistries.STRUCTURE_TYPES.register("slime_island",
            () -> () -> SlimeIslandStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SLIME_ISLAND_PIECE = TinkerRegistries.STRUCTURE_PIECE_TYPES.register("slime_island", () -> SlimeIslandPiece::new);

    /** Average chunk distance between slime island placement attempts. Bumped 50% from the ticket's 64 so islands aren't overrepresented. */
    public static final int SPACING = 96;

    /** Minimum chunk distance enforced between two slime islands. Scaled proportionally with {@link #SPACING}. Must stay strictly less than spacing. */
    public static final int SEPARATION = 36;

    /** Salt seed for the random-spread placement. Stable per mod so a /seed change still produces deterministic island offsets relative to other structures. */
    public static final int PLACEMENT_SALT = 0xACE5C1;

    /** Blueslime spawn weight on overworld biomes; matches vanilla slime weight (100). */
    private static final int BLUESLIME_SPAWN_WEIGHT = 100;

    /** Huge slime spawn weight — boss-rare, weight 1 vs. blueslime's 100. */
    private static final int HUGESLIME_SPAWN_WEIGHT = 1;

    /** Blueslime spawn pack size — 4-4 mirrors vanilla slime's group size. */
    private static final int BLUESLIME_PACK_SIZE = 4;

    /** Huge slime spawn pack size — 1-1 because the boss form spawns solo. */
    private static final int HUGESLIME_PACK_SIZE = 1;

    /** Single-set resource key holding every colour variant; placement is shared across colours. */
    public static final ResourceKey<StructureSet> SLIME_ISLANDS_SET = ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "slime_islands"));

    /** BiomeModifier resource key for the slime mob spawn additions. */
    public static final ResourceKey<BiomeModifier> SLIME_MOB_SPAWNS_KEY = ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "slime_mob_spawns"));

    /**
     * Loot-table resource key for the slime-island treasure chest. Exposed here (rather than
     * buried inside the loot sub-provider) so a future {@link SlimeIslandPiece} variant that
     * places a treasure chest can resolve the same key when calling
     * {@code RandomizableContainerBlockEntity#setLootTable}.
     */
    public static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> SLIME_ISLAND_CHEST_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "chests/slime_island"));

    private static final Map<SlimeColor, ResourceKey<Structure>> STRUCTURE_KEYS = buildStructureKeys();

    private WorldStructures() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. Same pattern as {@link WorldBlocks#init()}.
    }

    /** Structure resource key for the slime island of the supplied colour. */
    public static ResourceKey<Structure> structureKey(SlimeColor color) {
        return STRUCTURE_KEYS.get(color);
    }

    /**
     * Write one {@link Structure} entry per {@link SlimeColor} to the supplied bootstrap
     * context. Each entry binds a {@link SlimeIslandStructure} configured with a shared
     * {@link Structure.StructureSettings} that targets {@link BiomeTags#IS_OVERWORLD} and a
     * {@link UniformInt}({@link SlimeIslandStructure#RADIUS_MIN}, {@link
     * SlimeIslandStructure#RADIUS_MAX}) sampler for the disc radius.
     */
    public static void bootstrapStructures(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        for (SlimeColor color : SlimeColor.values()) {
            Structure.StructureSettings settings = new Structure.StructureSettings(biomes.getOrThrow(BiomeTags.IS_OVERWORLD), Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES,
                    TerrainAdjustment.NONE);
            context.register(structureKey(color), new SlimeIslandStructure(settings, color, UniformInt.of(SlimeIslandStructure.RADIUS_MIN, SlimeIslandStructure.RADIUS_MAX)));
        }
    }

    /**
     * Write the single {@link #SLIME_ISLANDS_SET} entry that pools all four colour variants
     * under one {@link RandomSpreadStructurePlacement}. Every entry has equal weight (1) so
     * a placement attempt picks a colour uniformly at random.
     */
    public static void bootstrapStructureSets(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
        List<StructureSet.StructureSelectionEntry> entries = new ArrayList<>();
        for (SlimeColor color : SlimeColor.values()) {
            entries.add(new StructureSet.StructureSelectionEntry(structures.getOrThrow(structureKey(color)), 1));
        }
        context.register(SLIME_ISLANDS_SET, new StructureSet(entries, new RandomSpreadStructurePlacement(SPACING, SEPARATION, RandomSpreadType.LINEAR, PLACEMENT_SALT)));
    }

    /**
     * Write {@link #SLIME_MOB_SPAWNS_KEY} as a {@link BiomeModifiers.AddSpawnsBiomeModifier}
     * that adds blueslime + hugeslime spawn entries to {@link BiomeTags#IS_OVERWORLD}.
     * Blueslime carries vanilla-slime weight (100) and pack size 4; huge slime is boss-rare at
     * weight 1 pack size 1.
     */
    public static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderSet<Biome> overworld = biomes.getOrThrow(BiomeTags.IS_OVERWORLD);
        List<MobSpawnSettings.SpawnerData> spawners = List.of(new MobSpawnSettings.SpawnerData(WorldEntities.BLUESLIME.get(), BLUESLIME_SPAWN_WEIGHT, BLUESLIME_PACK_SIZE, BLUESLIME_PACK_SIZE),
                new MobSpawnSettings.SpawnerData(WorldEntities.HUGESLIME.get(), HUGESLIME_SPAWN_WEIGHT, HUGESLIME_PACK_SIZE, HUGESLIME_PACK_SIZE));
        context.register(SLIME_MOB_SPAWNS_KEY, new BiomeModifiers.AddSpawnsBiomeModifier(overworld, spawners));
    }

    private static Map<SlimeColor, ResourceKey<Structure>> buildStructureKeys() {
        Map<SlimeColor, ResourceKey<Structure>> map = new EnumMap<>(SlimeColor.class);
        for (SlimeColor color : SlimeColor.values()) {
            map.put(color, ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "slime_island_" + color.id())));
        }
        return java.util.Collections.unmodifiableMap(map);
    }
}
