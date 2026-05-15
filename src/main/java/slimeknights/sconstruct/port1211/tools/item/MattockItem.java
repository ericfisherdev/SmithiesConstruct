package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;

/**
 * Axe / shovel / hoe hybrid bound to {@link ToolDefinition#MATTOCK}. Uses
 * {@link AoePattern#COLUMN_1x3} so the dig stroke extends one block above and below the struck
 * position — the legacy 1.12 mattock "1x3" dig pattern. The right-click ability surface
 * (axe-strip / shovel-flatten / hoe-till) is declared on the underlying {@link ToolDefinition}.
 */
public class MattockItem extends AoeToolCore {

    public MattockItem(Item.Properties properties) {
        super(properties, ToolDefinition.MATTOCK, AoePattern.COLUMN_1x3);
    }
}
