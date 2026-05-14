package slimeknights.sconstruct.port1211.world;

import java.util.List;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.world.entity.EntityBlueslime;
import slimeknights.sconstruct.port1211.world.entity.EntityHugeSlime;

/**
 * Phase-3 slime mob registrations. Two entity types:
 *
 * <ul>
 *   <li>{@link #BLUESLIME} — small (0.5×0.5) blue slime mob native to slime islands.</li>
 *   <li>{@link #HUGESLIME} — boss-sized (4×4) rare-spawn slime; default size 4 set in
 *       {@link EntityHugeSlime#finalizeSpawn}.</li>
 * </ul>
 *
 * <p>Both entity types use the {@code MOB_CATEGORY_MONSTER} placement bucket so vanilla's
 * monster-cap rules apply and {@code Slime.checkSlimeSpawnRules} (the predicate that ties slime
 * spawns to the slime-chunk algorithm + swamps + light level) carries over verbatim. Worldgen
 * island-spawn biasing arrives in SMTCON-58/59; this ticket sets up the entity type + attributes
 * + spawn-placement registration so {@code /summon} works today and the worldgen task only
 * needs to plug in biome modifiers.
 *
 * <p>{@link #register(IEventBus)} subscribes:
 *
 * <ul>
 *   <li>{@link EntityAttributeCreationEvent} — attaches the per-entity attribute baseline
 *       returned by {@code createAttributes()} on each entity class.</li>
 *   <li>{@link RegisterSpawnPlacementsEvent} — registers {@link SpawnPlacementTypes#ON_GROUND}
 *       + {@link Heightmap.Types#MOTION_BLOCKING_NO_LEAVES} + {@link Slime#checkSlimeSpawnRules}
 *       for both entity types so spawns are gated by the vanilla slime predicate.</li>
 * </ul>
 *
 * <p>{@link #init()} forces this class to load during mod construction so the
 * {@link DeferredHolder}s populate the {@link TinkerRegistries#ENTITY_TYPES} register before
 * the registry event fires.
 */
public final class WorldEntities {

    /** Width of the small blueslime entity. Mirrors vanilla's smallest slime size. */
    private static final float BLUESLIME_SIZE = 0.5F;

    /** Width of the huge slime entity. Vanilla "size 4" slime sits at 4 blocks square. */
    private static final float HUGESLIME_SIZE = 4.0F;

    public static final DeferredHolder<EntityType<?>, EntityType<EntityBlueslime>> BLUESLIME = TinkerRegistries.ENTITY_TYPES.register("blueslime", () -> EntityType.Builder
            .<EntityBlueslime> of(EntityBlueslime::new, MobCategory.MONSTER).sized(BLUESLIME_SIZE, BLUESLIME_SIZE).clientTrackingRange(8).updateInterval(3).build(SConstruct.MOD_ID + ":blueslime"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityHugeSlime>> HUGESLIME = TinkerRegistries.ENTITY_TYPES.register("hugeslime", () -> EntityType.Builder
            .<EntityHugeSlime> of(EntityHugeSlime::new, MobCategory.MONSTER).sized(HUGESLIME_SIZE, HUGESLIME_SIZE).clientTrackingRange(8).updateInterval(3).build(SConstruct.MOD_ID + ":hugeslime"));

    /** Insertion-ordered list view of every slime mob entity type. Iterated by datagen / lang / loot. */
    public static final List<DeferredHolder<EntityType<?>, ? extends EntityType<?>>> ALL = List.of(BLUESLIME, HUGESLIME);

    private WorldEntities() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. Same pattern as {@link WorldBlocks#init()}.
    }

    /**
     * Subscribe the per-entity attribute baseline and the slime spawn-placement registration to
     * the supplied mod event bus. Called by {@link TinkerWorldPulse#register} after
     * {@link #init()} so the entity holders are populated before either listener fires.
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(WorldEntities::onAttributeCreation);
        modBus.addListener(WorldEntities::onSpawnPlacements);
    }

    private static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(BLUESLIME.get(), EntityBlueslime.createAttributes().build());
        event.put(HUGESLIME.get(), EntityHugeSlime.createAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Both slime mobs share the vanilla slime spawn predicate so spawning continues to
        // honour slime chunks, swamps, the cap, and the lightless-spawn rule. The Heightmap
        // type matches vanilla Slime — MOTION_BLOCKING_NO_LEAVES — so a slime never spawns
        // inside a tree canopy.
        event.register(BLUESLIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WorldEntities::checkSlimeSpawnRulesAdapter,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HUGESLIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WorldEntities::checkSlimeSpawnRulesAdapter,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /**
     * Adapt {@link Slime#checkSlimeSpawnRules}'s {@code EntityType<Slime>}-typed predicate to any
     * {@link Slime} subclass entity type. The vanilla method body doesn't reflect on the
     * parameter's generic argument — it just forwards to {@code checkMobSpawnRules} or evaluates
     * the slime-chunk / swamp rules against the position — so an unchecked downcast on the
     * entity-type argument is correct at runtime. Java's invariant generics refuse the bare
     * method reference here because {@code EntityType<? extends Slime>} is not assignable to
     * {@code EntityType<Slime>} without that cast. A single generic adapter covers every slime
     * subclass — future additions plug into the same {@code WorldEntities::checkSlimeSpawnRulesAdapter}
     * call site without a new wrapper per type.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T extends Slime> boolean checkSlimeSpawnRulesAdapter(EntityType<T> type, net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnType,
            net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        return Slime.checkSlimeSpawnRules((EntityType) type, level, spawnType, pos, random);
    }

    /** Test seam: mod-namespaced sanity check on the entity ids. */
    static String namespace() {
        return SConstruct.MOD_ID;
    }
}
