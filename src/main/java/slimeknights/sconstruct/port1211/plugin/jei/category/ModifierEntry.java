package slimeknights.sconstruct.port1211.plugin.jei.category;

import java.util.Objects;

import slimeknights.sconstruct.port1211.tools.modifier.Modifier;

/**
 * One synthetic "recipe" of the JEI {@link ModifierCategory} — a single tool modifier. Modifiers
 * are datapack-registry entries with no crafting recipe, so JEI is fed one {@code ModifierEntry}
 * per registered modifier instead of a {@code RecipeHolder}.
 *
 * <p>{@link ModifierCategory} reads the modifier's {@link Modifier#description description},
 * {@link Modifier#maxLevel level cap}, and {@link Modifier#slotCost slot cost} straight off the
 * wrapped instance, so this record carries the {@link Modifier} itself rather than copying
 * those fields.
 *
 * @param modifier the modifier this entry describes
 */
public record ModifierEntry(Modifier modifier) {

    public ModifierEntry {
        Objects.requireNonNull(modifier, "modifier");
    }
}
