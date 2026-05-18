package slimeknights.sconstruct.plugin.jei.category;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import slimeknights.sconstruct.common.data.TinkerDataComponents;
import slimeknights.sconstruct.tools.PartType;
import slimeknights.sconstruct.tools.item.ToolParts;
import slimeknights.sconstruct.tools.material.client.MaterialClientCache;

/**
 * Shared helper for the JEI tool-building and part-building categories: builds the list of
 * material-stamped part stacks a slot cycles through.
 *
 * <p>Material ids come from {@link MaterialClientCache} — JEI renders client-side, where the
 * server-only {@code MaterialRegistry} cache is never populated on a remote client.
 */
final class PartMaterialPreview {

    private PartMaterialPreview() {
    }

    /**
     * The part item for {@code part}, stamped once per registered material so a JEI slot cycles
     * through every material. Falls back to a single unstamped part stack when no materials are
     * loaded (before the first registry sync) — JEI never shows a blank slot, and the part
     * item's own default material ({@code tconstruct:wood}) keeps the preview meaningful.
     */
    static List<ItemStack> stampedVariants(PartType part) {
        var partItem = ToolParts.get(part).get();
        List<ResourceLocation> materials = MaterialClientCache.materialIds();
        if (materials.isEmpty()) {
            return List.of(new ItemStack(partItem));
        }
        List<ItemStack> variants = new ArrayList<>(materials.size());
        for (ResourceLocation material : materials) {
            ItemStack stack = new ItemStack(partItem);
            stack.set(TinkerDataComponents.PART_MATERIAL.get(), material);
            variants.add(stack);
        }
        return variants;
    }
}
