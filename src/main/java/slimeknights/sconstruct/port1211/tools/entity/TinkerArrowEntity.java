package slimeknights.sconstruct.port1211.tools.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import slimeknights.sconstruct.port1211.tools.ToolHelper;

/**
 * Arrow projectile fired by SMTCON bows when the loaded ammo is a
 * {@link slimeknights.sconstruct.port1211.tools.item.TinkerArrowItem}. Extends vanilla
 * {@link Arrow} so trajectory, block-stick, and pickup behaviour all flow through the
 * battle-tested vanilla code path; only the on-spawn baseline damage is overridden so the
 * arrow's ToolStats.attackDamage feeds the impact figure.
 */
public class TinkerArrowEntity extends Arrow {

    /** Threshold below which the arrow ToolStats are treated as "not yet computed". */
    private static final float UNBUILT_DAMAGE_FLOOR = 0.0F;

    public TinkerArrowEntity(EntityType<? extends TinkerArrowEntity> type, Level level) {
        super(type, level);
    }

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod") // setBaseDamage is the documented vanilla seam for fixing the arrow's damage figure on spawn; deferring loses the value before the first network sync.
    public TinkerArrowEntity(EntityType<? extends TinkerArrowEntity> type, LivingEntity shooter, Level level, ItemStack arrowStack, ItemStack weapon) {
        super(level, shooter, arrowStack, weapon);
        // baseDamage is read off the stat snapshot the tool-station rebuild wrote when the
        // arrow's materials were applied. Empty / un-built arrows fall through to the vanilla
        // baseline so the creative-tab arrow item doesn't shoot for zero damage.
        float attackDamage = ToolHelper.getStats(arrowStack).attackDamage();
        if (attackDamage > UNBUILT_DAMAGE_FLOOR) {
            setBaseDamage(attackDamage);
        }
    }
}
