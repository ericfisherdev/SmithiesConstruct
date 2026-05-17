package slimeknights.sconstruct.port1211.gadgets.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import slimeknights.sconstruct.port1211.shared.SharedBlocks;

/**
 * Thrown projectile spawned by a {@code GlowBallItem}. Extends {@link ThrowableItemProjectile}
 * so vanilla handles trajectory, gravity, and the client-side item-billboard render. On impact
 * it places a {@link SharedBlocks#GLOW glow block} at the struck position when that position is
 * air; if the position is already occupied the glow ball drops back as its item instead, so a
 * throw is never silently consumed for nothing.
 */
public class GlowBallEntity extends ThrowableItemProjectile implements ItemSupplier {

    public GlowBallEntity(EntityType<? extends GlowBallEntity> type, Level level) {
        super(type, level);
    }

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod") // setItem is the documented vanilla seam for stashing the projectile's item visual on spawn.
    public GlowBallEntity(EntityType<? extends GlowBallEntity> type, LivingEntity shooter, Level level, ItemStack stack) {
        super(type, shooter, level);
        if (!stack.isEmpty()) {
            setItem(stack);
        }
    }

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod") // setItem is the documented vanilla seam for stashing the projectile's item visual on spawn.
    public GlowBallEntity(EntityType<? extends GlowBallEntity> type, Level level, double x, double y, double z, ItemStack stack) {
        super(type, x, y, z, level);
        if (!stack.isEmpty()) {
            setItem(stack);
        }
    }

    @Override
    protected Item getDefaultItem() {
        // Test seam: AIR keeps the unit-test path off the registry — the real glow-ball item is
        // supplied via the constructor when GlowBallItem (or a dispenser) spawns the entity.
        return Items.AIR;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide()) {
            return;
        }
        BlockPos target = placementPos(result);
        if (level().getBlockState(target).isAir()) {
            level().setBlockAndUpdate(target, SharedBlocks.GLOW.get().defaultBlockState());
        }
        else {
            // No room for the glow block — drop the glow ball so the throw is recoverable.
            spawnAtLocation(getItem());
        }
        discard();
    }

    /**
     * The block position the glow block should occupy. For a block hit this is the air cell on
     * the struck face; for an entity hit or a spent projectile it is the projectile's own cell.
     */
    private BlockPos placementPos(HitResult result) {
        if (result instanceof BlockHitResult blockHit) {
            return blockHit.getBlockPos().relative(blockHit.getDirection());
        }
        return blockPosition();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }
}
