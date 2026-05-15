package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;

/**
 * Vanilla-equivalent hatchet tinker tool — the single-handed axe. Concrete {@link ToolCore}
 * subclass bound to the {@link ToolDefinition#HATCHET} 3-slot recipe (handle + axe-head +
 * binding). The class name {@code AxeItem} matches the vanilla tool-slot terminology even
 * though the underlying definition is legacy {@code HATCHET} — see the SMTCON-78 broadaxe /
 * lumberaxe follow-up for the two-handed variant. See {@link PickaxeItem} for the rationale
 * behind one subclass per tool slot.
 */
public class AxeItem extends ToolCore {

    public AxeItem(Item.Properties properties) {
        super(properties, ToolDefinition.HATCHET);
    }
}
