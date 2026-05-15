package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.data.ToolStats;
import slimeknights.sconstruct.port1211.tools.ToolDefinition;

/**
 * Light sword variant bound to {@link ToolDefinition#RAPIER}. The held bonus raises the
 * player's attack speed by 1.0 (one additional swing per second on top of the stats-derived
 * baseline) via a class-specific {@link Attributes#ATTACK_SPEED} modifier layered on top of the
 * stats-derived baseline by {@link #augmentAttributes}.
 */
public class RapierItem extends ToolCore {

    /** Attack-speed bonus — legacy 1.12 rapier baseline (one extra swing per second). */
    public static final double ATTACK_SPEED_BONUS = 1.0D;

    /** Modifier id namespaced under the mod so the speed bonus can't collide with addons. */
    public static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "rapier_speed");

    public RapierItem(Item.Properties properties) {
        super(properties, ToolDefinition.RAPIER);
    }

    @Override
    public ItemAttributeModifiers augmentAttributes(ItemAttributeModifiers baseline, ToolStats stats) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : baseline.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        builder.add(Attributes.ATTACK_SPEED, new AttributeModifier(SPEED_MODIFIER_ID, ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        return builder.build();
    }
}
