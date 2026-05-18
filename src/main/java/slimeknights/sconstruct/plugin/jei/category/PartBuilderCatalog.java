package slimeknights.sconstruct.plugin.jei.category;

import java.util.Arrays;
import java.util.List;

import slimeknights.sconstruct.tools.PartType;

/**
 * Builds the synthetic recipe list for the JEI {@link PartBuilderCategory} — exactly one
 * {@link PartBuilderEntry} per {@link PartType}. SMTCON-157 calls {@link #entries()} from the
 * JEI plugin's {@code registerRecipes} hook.
 *
 * <p>The per-material variety is shown by cycling each entry's output slot through the
 * registered materials, so the entry count is bounded to {@code PartType.values().length}
 * rather than a material × part-type cross-product.
 */
public final class PartBuilderCatalog {

    private PartBuilderCatalog() {
    }

    /** One {@link PartBuilderEntry} per {@link PartType}, in enum-declaration order. */
    public static List<PartBuilderEntry> entries() {
        return Arrays.stream(PartType.values()).map(PartBuilderEntry::new).toList();
    }
}
