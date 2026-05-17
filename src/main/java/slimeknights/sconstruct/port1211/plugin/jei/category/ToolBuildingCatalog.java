package slimeknights.sconstruct.port1211.plugin.jei.category;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;
import slimeknights.sconstruct.port1211.tools.item.ToolCore;
import slimeknights.sconstruct.port1211.tools.item.ToolItems;

/**
 * Builds the synthetic recipe list for the JEI {@link ToolBuildingCategory} — exactly one
 * {@link ToolBuildingEntry} per {@link ToolDefinition} in {@link ToolDefinition#ALL_ADVANCED}
 * (the full Tool-Forge roster). SMTCON-157 calls {@link #entries()} from the JEI plugin's
 * {@code registerRecipes} hook.
 *
 * <p>Each definition is paired with its built tool item by reading {@link ToolCore#definition}
 * off every registered {@link ToolItems#ALL_TOOLS} item: the item registry is the single source
 * of truth for which {@code ToolCore} carries which definition, so the pairing never drifts out
 * of sync with a hand-maintained map. A definition with no matching item is a registration bug
 * and fails loudly rather than dropping the tool silently from JEI.
 */
public final class ToolBuildingCatalog {

    private ToolBuildingCatalog() {
    }

    /**
     * One {@link ToolBuildingEntry} per advanced tool definition, in
     * {@link ToolDefinition#ALL_ADVANCED} order. Built fresh on each call — the caller (JEI's
     * one-shot {@code registerRecipes}) invokes it once, so caching would only add lifecycle
     * questions about registry freshness for no measurable gain.
     *
     * @throws IllegalStateException if a definition has no registered {@link ToolCore} item
     */
    public static List<ToolBuildingEntry> entries() {
        Map<ToolDefinition, ToolCore> toolsByDefinition = new IdentityHashMap<>();
        for (var deferred : ToolItems.ALL_TOOLS) {
            ToolCore tool = deferred.get();
            toolsByDefinition.put(tool.definition, tool);
        }

        List<ToolBuildingEntry> entries = new ArrayList<>(ToolDefinition.ALL_ADVANCED.size());
        for (ToolDefinition definition : ToolDefinition.ALL_ADVANCED) {
            ToolCore tool = toolsByDefinition.get(definition);
            if (tool == null) {
                throw new IllegalStateException("no registered ToolCore item carries ToolDefinition '" + definition.id() + "' — tool item registration regressed");
            }
            entries.add(new ToolBuildingEntry(definition, tool));
        }
        return List.copyOf(entries);
    }
}
