package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;

/**
 * Vanilla-equivalent shovel tinker tool. Concrete {@link ToolCore} subclass bound to the
 * {@link ToolDefinition#SHOVEL} 3-slot recipe (handle + shovel-head + binding). See
 * {@link PickaxeItem} for the rationale behind one subclass per tool slot.
 */
public class ShovelItem extends ToolCore {

    public ShovelItem(Item.Properties properties) {
        super(properties, ToolDefinition.SHOVEL);
    }
}
