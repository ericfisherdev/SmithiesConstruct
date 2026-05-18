package slimeknights.sconstruct.tools.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.common.data.ToolStats;
import slimeknights.sconstruct.tools.ToolDefinition;

/**
 * Mid-weight sword variant bound to {@link ToolDefinition#LONGSWORD}. The held bonus extends
 * the player's entity interaction range by +1 block via a class-specific
 * {@link Attributes#ENTITY_INTERACTION_RANGE} modifier layered on top of the stats-derived
 * baseline by {@link #augmentAttributes}. Vanilla swords ship no reach bonus, so this is
 * directly observable in-game.
 */
public class LongswordItem extends ToolCore {

    /** Reach bonus in blocks — legacy 1.12 longsword "extra reach" baseline. */
    public static final double REACH_BONUS = 1.0D;

    /**
     * Modifier id namespaced under the mod so the reach modifier can't collide with any
     * downstream addon. Vanilla rejects duplicate ids on the same attribute, so a stable, mod-
     * scoped id is the load-bearing detail here.
     */
    public static final ResourceLocation REACH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "longsword_reach");

    public LongswordItem(Item.Properties properties) {
        super(properties, ToolDefinition.LONGSWORD);
    }

    @Override
    public ItemAttributeModifiers augmentAttributes(ItemAttributeModifiers baseline, ToolStats stats) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : baseline.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        builder.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH_MODIFIER_ID, REACH_BONUS, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        return builder.build();
    }
}
