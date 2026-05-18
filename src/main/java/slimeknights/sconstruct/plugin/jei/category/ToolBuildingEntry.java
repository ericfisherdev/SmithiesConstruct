package slimeknights.sconstruct.plugin.jei.category;

import java.util.Objects;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.tools.ToolDefinition;

/**
 * One synthetic "recipe" of the JEI {@link ToolBuildingCategory} — a single buildable tool. The
 * mod has no datapack recipe for tool building (a tool is assembled from part items at the Tool
 * Station / Tool Forge, not crafted), so JEI is fed one {@code ToolBuildingEntry} per
 * {@link ToolDefinition} instead of a {@code RecipeHolder}.
 *
 * <p>Per the Phase-7 risk note in {@code plan/08}, there is deliberately <em>one</em> entry per
 * tool — never a material × tool cross-product. The per-part material variety is shown by
 * cycling each part slot through the registered materials in {@link ToolBuildingCategory}, so
 * the JEI panel count stays at the tool-definition count regardless of how many materials are
 * registered.
 *
 * @param definition the tool's part-slot roster, surfaced as the category's input slots
 * @param tool       the built tool item, surfaced as the category's output slot
 */
public record ToolBuildingEntry(ToolDefinition definition, Item tool) {

    public ToolBuildingEntry {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(tool, "tool");
    }
}
