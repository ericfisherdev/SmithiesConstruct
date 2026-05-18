package slimeknights.sconstruct.tools.item;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import slimeknights.sconstruct.tools.ToolDefinition;
import slimeknights.sconstruct.tools.ToolHelper;

/**
 * Two-handed AOE melee weapon bound to {@link ToolDefinition#SCYTHE}. Extends the AOE mining
 * surface with an attack-side override: every swing that lands a hit also damages every other
 * {@link LivingEntity} within a small radius of the primary target so a sweep through a mob
 * crowd kills more than one entity per swing — the legacy 1.12 scythe attack pattern.
 *
 * <p>The mining-side pattern is set to {@link AoePattern#FULL3x3} so the scythe can also clear
 * a 3x3 of crops / leaves on a regular swing.
 */
public class ScytheItem extends AoeToolCore {

    /**
     * Half-extent (in blocks) of the AABB centred on the primary target that the AOE sweep
     * damage radius covers. Matches the legacy 1.12 scythe sweep radius — a 3-block diameter
     * cube around the hit entity catches an adjacent mob without sweeping through walls.
     */
    private static final double SWEEP_RADIUS = 1.5D;

    public ScytheItem(Item.Properties properties) {
        super(properties, ToolDefinition.SCYTHE, AoePattern.FULL3x3);
    }

    /**
     * Land the primary hit, then on a successful intact swing damage every other living entity
     * inside the {@link #SWEEP_RADIUS} AABB centred on the target. The cooldown sound,
     * durability tick, and modifier dispatch all live in {@code super.hurtEnemy}; the sweep
     * here only feeds extra entities into vanilla's {@link LivingEntity#hurt} on a separate
     * pass.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean handled = super.hurtEnemy(stack, target, attacker);
        if (handled && !target.level().isClientSide() && !ToolHelper.isBroken(stack)) {
            float damage = ToolHelper.getStats(stack).attackDamage();
            AABB sweep = target.getBoundingBox().inflate(SWEEP_RADIUS);
            for (LivingEntity other : target.level().getEntitiesOfClass(LivingEntity.class, sweep)) {
                if (other.equals(target) || other.equals(attacker)) {
                    continue;
                }
                // Skip entities the attacker can't see — a sweep through a wall shouldn't hit
                // mobs hidden behind cover, matching vanilla sword-sweep line-of-sight gating.
                if (!attacker.hasLineOfSight(other)) {
                    continue;
                }
                // Pick the damage source from the attacker's registry so kill credit,
                // advancements, and aggro target route correctly: players use playerAttack so
                // PvP statistics fire; non-player wielders fall through to mobAttack.
                DamageSource source = attacker instanceof Player wielder ? attacker.damageSources().playerAttack(wielder) : attacker.damageSources().mobAttack(attacker);
                other.hurt(source, damage);
            }
        }
        return handled;
    }
}
