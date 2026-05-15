package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;

/**
 * Heavy-pickaxe AOE tool bound to {@link ToolDefinition#HAMMER} (toughHandle + hammerHead +
 * largePlate × 2). Swings break a 3x3 perpendicular to the struck face via
 * {@link AoePattern#FULL3x3} — the legacy 1.12 hammer behaviour.
 */
public class HammerItem extends AoeToolCore {

    public HammerItem(Item.Properties properties) {
        super(properties, ToolDefinition.HAMMER, AoePattern.FULL3x3);
    }
}
