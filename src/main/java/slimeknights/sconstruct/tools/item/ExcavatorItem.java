package slimeknights.sconstruct.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.tools.ToolDefinition;

/**
 * Heavy-shovel AOE counterpart to the hammer, bound to {@link ToolDefinition#EXCAVATOR}. Breaks
 * a 3x3 of shovel-friendly blocks per swing via {@link AoePattern#FULL3x3}.
 */
public class ExcavatorItem extends AoeToolCore {

    public ExcavatorItem(Item.Properties properties) {
        super(properties, ToolDefinition.EXCAVATOR, AoePattern.FULL3x3);
    }
}
