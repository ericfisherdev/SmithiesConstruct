package slimeknights.sconstruct.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.tools.ToolDefinition;

/**
 * Two-handed axe bound to {@link ToolDefinition#LUMBER_AXE}; swings fell the full connected log
 * stack via {@link AoePattern#TREE}. The BFS in {@link AoeHelper#walkConnectedLogs} caps at 64
 * blocks so a custom mega-tree can't lag the server.
 */
public class LumberAxeItem extends AoeToolCore {

    public LumberAxeItem(Item.Properties properties) {
        super(properties, ToolDefinition.LUMBER_AXE, AoePattern.TREE);
    }
}
