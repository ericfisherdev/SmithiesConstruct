package slimeknights.sconstruct.tools.entity;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Thrown projectile spawned by {@link slimeknights.sconstruct.tools.item.ShurikenItem}.
 * Extends {@link ThrowableItemProjectile} so vanilla handles trajectory, gravity, and the
 * client-side spinning item billboard render. On entity hit deals {@link #BASE_DAMAGE} damage
 * scaled against the projectile's velocity; on block hit, drops as the item stack so the player
 * can pick it back up.
 */
public class ShurikenEntity extends ThrowableItemProjectile implements ItemSupplier {

    /** Damage at full launch velocity — scaled down for slower shurikens to keep arc-thrown shots fair. */
    public static final float BASE_DAMAGE = 4.0F;

    public ShurikenEntity(EntityType<? extends ShurikenEntity> type, Level level) {
        super(type, level);
    }

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod") // setItem is the documented vanilla seam for stashing the projectile's item visual on spawn; cannot be deferred without losing the visible item before the first network sync.
    public ShurikenEntity(EntityType<? extends ShurikenEntity> type, LivingEntity shooter, Level level, ItemStack stack) {
        super(type, shooter, level);
        if (!stack.isEmpty()) {
            setItem(stack);
        }
    }

    @Override
    protected Item getDefaultItem() {
        // Test seam: returning AIR keeps the unit-test path off the registry — the real item is
        // supplied via the constructor when ShurikenItem spawns the entity in-game.
        return net.minecraft.world.item.Items.AIR;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide() || !(result.getEntity() instanceof LivingEntity target)) {
            return;
        }
        // Scale damage with current velocity so a partial-charge throw doesn't land a full hit.
        float damage = BASE_DAMAGE * Math.min(1.0F, (float) getDeltaMovement().length());
        target.hurt(damageSources().thrown(this, getOwner()), damage);
    }

    @Override
    protected void onHit(net.minecraft.world.phys.HitResult result) {
        super.onHit(result);
        if (!level().isClientSide() && result.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            // Drop the shuriken as a pickup so the player can collect it back — matches the
            // legacy 1.12 "returnable shuriken" behaviour without the boomerang flight path.
            spawnAtLocation(getItem());
            discard();
        }
    }

    @Override
    public void playerTouch(Player player) {
        // Player walking over a stuck shuriken collects it directly rather than via the dropped
        // item — keeps the pickup feel snappy.
        if (!level().isClientSide() && player.equals(getOwner()) && player.getInventory().add(getItem())) {
            discard();
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }
}
