package slimeknights.sconstruct.gadgets.entity;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import slimeknights.sconstruct.gadgets.item.ThrowballItem;
import slimeknights.sconstruct.world.block.SlimeColor;

/**
 * Thrown projectile spawned by a {@link ThrowballItem}. Extends {@link ThrowableItemProjectile}
 * so vanilla handles trajectory, gravity, and the client-side item-billboard render. On any
 * hit the throwball applies an area-of-effect tied to the slime colour of the item it carries:
 *
 * <ul>
 *   <li>{@link SlimeColor#BLUE} — slows every living entity in range.</li>
 *   <li>{@link SlimeColor#PURPLE} — a purely cosmetic explosion: particles and a boom, no
 *       block or entity damage.</li>
 *   <li>{@link SlimeColor#BLOOD} — weakens every living entity in range.</li>
 *   <li>{@link SlimeColor#MAGMA} — sets every living entity in range on fire.</li>
 * </ul>
 *
 * <p>The colour is read from the carried {@link #getItem() item stack} rather than a separate
 * synced data slot: {@link ThrowableItemProjectile} already syncs the item, and the one
 * registered throwball {@link EntityType} serves every colour, so the item is the single
 * source of truth for which effect to apply.
 */
public class ThrowballEntity extends ThrowableItemProjectile implements ItemSupplier {

    /** Radius in blocks around the impact point within which living entities take the effect. */
    private static final double EFFECT_RADIUS = 3.0D;

    /** Duration in ticks of the slowness / weakness effects — five seconds. */
    private static final int EFFECT_DURATION_TICKS = 100;

    /** Seconds the magma throwball sets caught entities on fire. */
    private static final float MAGMA_FIRE_SECONDS = 4.0F;

    public ThrowballEntity(EntityType<? extends ThrowballEntity> type, Level level) {
        super(type, level);
    }

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod") // setItem is the documented vanilla seam for stashing the projectile's item visual on spawn.
    public ThrowballEntity(EntityType<? extends ThrowballEntity> type, LivingEntity shooter, Level level, ItemStack stack) {
        super(type, shooter, level);
        if (!stack.isEmpty()) {
            setItem(stack);
        }
    }

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod") // setItem is the documented vanilla seam for stashing the projectile's item visual on spawn.
    public ThrowballEntity(EntityType<? extends ThrowballEntity> type, Level level, double x, double y, double z, ItemStack stack) {
        super(type, x, y, z, level);
        if (!stack.isEmpty()) {
            setItem(stack);
        }
    }

    @Override
    protected Item getDefaultItem() {
        // Test seam: AIR keeps the unit-test path off the registry — the real throwball item is
        // supplied via the constructor when ThrowballItem (or a dispenser) spawns the entity.
        return Items.AIR;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide()) {
            return;
        }
        applyImpactEffect();
        // Event 3 spawns the vanilla item-break "poof" particles on every tracking client.
        level().broadcastEntityEvent(this, (byte) 3);
        discard();
    }

    /** Applies this throwball's colour-specific area effect around its current position. */
    // ServerLevel is AutoCloseable in the type system, but the world is owned by the server
    // lifecycle, not by this entity — PMD's CloseResource heuristic does not model that.
    @SuppressWarnings("PMD.CloseResource")
    private void applyImpactEffect() {
        SlimeColor color = carriedColor();
        if (color == null) {
            return;
        }
        Vec3 at = position();
        if (color == SlimeColor.PURPLE) {
            // A purely cosmetic explosion — particles and a boom, no block or entity damage.
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            level().playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            return;
        }
        AABB area = new AABB(at.subtract(EFFECT_RADIUS, EFFECT_RADIUS, EFFECT_RADIUS), at.add(EFFECT_RADIUS, EFFECT_RADIUS, EFFECT_RADIUS));
        List<LivingEntity> caught = level().getEntitiesOfClass(LivingEntity.class, area, living -> living.distanceToSqr(at) <= EFFECT_RADIUS * EFFECT_RADIUS);
        for (LivingEntity living : caught) {
            switch (color) {
            case BLUE -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION_TICKS));
            case BLOOD -> living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION_TICKS));
            case MAGMA -> living.igniteForSeconds(MAGMA_FIRE_SECONDS);
            default -> {
                // PURPLE handled above; no other colours exist.
            }
            }
        }
    }

    /** The slime colour of the carried throwball item, or {@code null} if the item is not a throwball. */
    private SlimeColor carriedColor() {
        return getItem().getItem() instanceof ThrowballItem throwball ? throwball.color() : null;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }
}
