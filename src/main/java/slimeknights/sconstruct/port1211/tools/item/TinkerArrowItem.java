package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;

/**
 * Part-built arrow that material-roles like a tool — three-part recipe (arrowhead +
 * arrowshaft + fletching) built at the tool station against {@link ToolDefinition#ARROW}. The
 * stack carries the same {@link slimeknights.sconstruct.port1211.common.data.ToolMaterials}
 * and {@link slimeknights.sconstruct.port1211.common.data.ToolStats} components every other
 * {@link ToolCore} ships with, so {@link slimeknights.sconstruct.port1211.tools.ToolHelper#rebuildStats}
 * fills in the per-stack damage / durability snapshot when the materials are written.
 *
 * <p>{@link BowToolCore} recognises the item via {@link BowToolCore#isTinkerArrow} and spawns a
 * {@link slimeknights.sconstruct.port1211.tools.entity.TinkerArrowEntity} on release; the
 * entity carries the source stack so its on-hit damage scales against the per-stack stats
 * rather than the vanilla baseline.
 */
public class TinkerArrowItem extends ToolCore {

    public TinkerArrowItem(Item.Properties properties) {
        super(properties, ToolDefinition.ARROW);
    }
}
