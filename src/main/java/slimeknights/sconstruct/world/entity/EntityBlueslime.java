package slimeknights.sconstruct.world.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;

/**
 * Small slime mob native to blue slime islands. Inherits vanilla {@link Slime}'s split-on-death
 * machinery, jump-toward-target AI, and bouncing physics; the only thing this subclass
 * customises is the attribute baseline ({@link #createAttributes}) — {@code MAX_HEALTH} 8.0,
 * {@code MOVEMENT_SPEED} 0.3 — and the default size of one block, mirroring vanilla's smallest
 * slime tier so a freshly spawned blueslime drops a familiar single-cube silhouette before
 * worldgen splits up larger spawns.
 *
 * <p>Vanilla {@code Slime#registerAttributes} returns an empty builder because vanilla derives
 * health from size via {@code setSize}. {@link #createAttributes} here returns a non-empty
 * builder so {@link net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent} has
 * something to attach — the registered values become the "base" before
 * {@link Slime#setSize}'s size-scaling math runs at spawn time.
 */
public class EntityBlueslime extends Slime {

    public EntityBlueslime(EntityType<? extends EntityBlueslime> type, Level level) {
        super(type, level);
    }

    /**
     * Attribute baseline for the blueslime entity type. Values are the registered base; vanilla
     * {@link Slime#setSize} multiplies {@code MAX_HEALTH} by {@code size * size} on spawn, so a
     * size-1 blueslime resolves to {@code 8.0 * 1 * 1 = 8.0} hp at spawn.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.3D).add(Attributes.ATTACK_DAMAGE);
    }
}
