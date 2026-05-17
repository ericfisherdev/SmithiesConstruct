package slimeknights.sconstruct.port1211.plugin.jei.category;

import java.util.Objects;

import slimeknights.sconstruct.port1211.tools.PartType;

/**
 * One synthetic "recipe" of the JEI {@link PartBuilderCategory} — a single buildable tool part.
 * There is no datapack recipe for part building (a part is shaped from a typed pattern and a
 * raw material at the Part Builder, not crafted), so JEI is fed one {@code PartBuilderEntry} per
 * {@link PartType} instead of a {@code RecipeHolder}.
 *
 * <p>Per the Phase-7 risk note, there is deliberately <em>one</em> entry per part type — never a
 * material × part-type cross-product. {@link PartBuilderCategory} cycles the output slot through
 * every registered material, so the JEI panel count stays at {@code PartType.values().length}
 * regardless of how many materials are registered.
 *
 * @param part the part type this entry builds
 */
public record PartBuilderEntry(PartType part) {

    public PartBuilderEntry {
        Objects.requireNonNull(part, "part");
    }
}
