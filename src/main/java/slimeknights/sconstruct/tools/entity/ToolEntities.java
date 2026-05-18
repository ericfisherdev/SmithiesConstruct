package slimeknights.sconstruct.tools.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.common.TinkerRegistries;

/**
 * Tool-entity registration hub. Mirrors {@link slimeknights.sconstruct.world.WorldEntities}
 * but scoped to tool projectiles so the tools pulse owns its own entity surface without
 * crossing into the world pulse's slime / world-gen entity roster.
 *
 * <p>Currently registers a single thrown projectile — the shuriken — but the file is here so
 * the SMTCON-102 arrow entity and any future thrown weapons land in the same namespace.
 */
public final class ToolEntities {

    /** Thrown shuriken projectile — see {@link ShurikenEntity}. */
    public static final DeferredHolder<EntityType<?>, EntityType<ShurikenEntity>> SHURIKEN = TinkerRegistries.ENTITY_TYPES.register("shuriken",
            () -> EntityType.Builder.<ShurikenEntity> of(ShurikenEntity::new, MobCategory.MISC).sized(0.4F, 0.1F).clientTrackingRange(4).updateInterval(10).build(SConstruct.MOD_ID + ":shuriken"));

    /** Tinker arrow projectile — see {@link TinkerArrowEntity}. */
    public static final DeferredHolder<EntityType<?>, EntityType<TinkerArrowEntity>> TINKER_ARROW = TinkerRegistries.ENTITY_TYPES.register("tinker_arrow", () -> EntityType.Builder
            .<TinkerArrowEntity> of(TinkerArrowEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build(SConstruct.MOD_ID + ":tinker_arrow"));

    private ToolEntities() {
    }

    /** Forces class load so the static field initialisers register every tool entity. */
    public static void init() {
        java.util.Objects.requireNonNull(SHURIKEN);
        java.util.Objects.requireNonNull(TINKER_ARROW);
    }
}
