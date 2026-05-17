package slimeknights.sconstruct.port1211.gadgets.entity;

import java.util.Objects;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.TinkerRegistries;

/**
 * Gadget-entity registration hub. Mirrors {@code ToolEntities} but scoped to the Phase-6
 * gadget projectiles so the gadgets subsystem owns its own entity surface.
 *
 * <p>SMTCON-133 registers the {@link ThrowballEntity}; SMTCON-135 adds the {@link GlowBallEntity}.
 * One entity type serves every throwball colour; the colour is carried on the projectile's item
 * stack.
 */
public final class GadgetEntities {

    /** Thrown throwball projectile — see {@link ThrowballEntity}. */
    public static final DeferredHolder<EntityType<?>, EntityType<ThrowballEntity>> THROWBALL = TinkerRegistries.ENTITY_TYPES.register("throwball", () -> EntityType.Builder
            .<ThrowballEntity> of(ThrowballEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(SConstruct.MOD_ID + ":throwball"));

    /** Thrown glow-ball projectile — see {@link GlowBallEntity}. */
    public static final DeferredHolder<EntityType<?>, EntityType<GlowBallEntity>> GLOW_BALL = TinkerRegistries.ENTITY_TYPES.register("glow_ball",
            () -> EntityType.Builder.<GlowBallEntity> of(GlowBallEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(SConstruct.MOD_ID + ":glow_ball"));

    private GadgetEntities() {
    }

    /** Forces class load so the static field initialisers register every gadget entity type. */
    public static void init() {
        Objects.requireNonNull(THROWBALL);
        Objects.requireNonNull(GLOW_BALL);
    }
}
