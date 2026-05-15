package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;
import slimeknights.sconstruct.port1211.tools.ToolHelper;

/**
 * Utility melee weapon bound to {@link ToolDefinition#FROE}. Deals 2x damage against
 * wood-armoured targets — defined as any {@link LivingEntity} wearing a leather armour piece
 * (vanilla's closest equivalent to the legacy 1.12 "wooden-armour" detection) — by re-hurting
 * the target with the bonus delta after the primary swing settles. Reuses the hatchet's mining
 * tag and abilities so the froe still chops trees outside combat.
 */
public class FroeItem extends ToolCore {

    /** Damage multiplier against wood-armoured targets — legacy 1.12 baseline. */
    public static final float WOOD_ARMOR_MULTIPLIER = 2.0F;

    public FroeItem(Item.Properties properties) {
        super(properties, ToolDefinition.FROE);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean wasWoodArmoured = isWoodArmoured(target);
        boolean handled = super.hurtEnemy(stack, target, attacker);
        if (handled && wasWoodArmoured) {
            // Vanilla's hurtAndBreak chain sets {@code target.invulnerableTime} to 20 ticks
            // after the primary swing — a follow-up hurt would be a no-op against those
            // i-frames. Zero the timer for the bonus packet so it lands, then restore the
            // attacker's damage source so attribution stays correct.
            float baseDamage = ToolHelper.getStats(stack).attackDamage();
            float bonus = baseDamage * (WOOD_ARMOR_MULTIPLIER - 1.0F);
            target.invulnerableTime = 0;
            target.hurt(attacker.damageSources().mobAttack(attacker), bonus);
        }
        return handled;
    }

    /**
     * Wood-armoured detection — any leather armour piece in any armour slot. Vanilla doesn't
     * ship a "wooden armour" set; leather is the in-game stand-in for the legacy 1.12
     * detection that targeted both wooden and leather pieces.
     */
    private static boolean isWoodArmoured(LivingEntity target) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            ItemStack piece = target.getItemBySlot(slot);
            if (piece.is(Items.LEATHER_HELMET) || piece.is(Items.LEATHER_CHESTPLATE) || piece.is(Items.LEATHER_LEGGINGS) || piece.is(Items.LEATHER_BOOTS)) {
                return true;
            }
        }
        return false;
    }
}
