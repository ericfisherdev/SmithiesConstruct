package slimeknights.sconstruct.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.tools.ToolDefinition;

/**
 * Vanilla-equivalent pickaxe tinker tool. Concrete {@link ToolCore} subclass bound to the
 * {@link ToolDefinition#PICKAXE} 3-slot recipe (handle + pick-head + binding). All behaviour
 * lives in the base class — this subclass exists so the item registry has a typed entry per
 * tool slot rather than a single {@code ToolCore} item parameterised by definition. The typed
 * subclass keeps {@code stack.getItem() instanceof PickaxeItem} usable as a discriminator for
 * downstream pulses (JEI categories, modifier compatibility checks, tool-station UI).
 */
public class PickaxeItem extends ToolCore {

    public PickaxeItem(Item.Properties properties) {
        super(properties, ToolDefinition.PICKAXE);
    }
}
