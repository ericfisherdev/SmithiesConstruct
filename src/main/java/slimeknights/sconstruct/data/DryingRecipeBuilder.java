package slimeknights.sconstruct.data;

import java.util.Objects;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import slimeknights.sconstruct.gadgets.recipe.DryingRecipe;

/**
 * Datagen builder for {@link DryingRecipe} JSONs (SMTCON-143). A drying recipe has no
 * crafting-grid shape and no unlock advancement — it dries one item into another on a drying
 * rack after a fixed tick count — so this builder mirrors {@link MeltingRecipeBuilder}: a thin
 * value carrier that collects the three recipe fields and writes the recipe straight to the
 * {@link RecipeOutput}.
 *
 * <p>Kept outside vanilla's {@code RecipeBuilder} hierarchy on purpose, exactly as
 * {@link MeltingRecipeBuilder} and {@link CastingRecipeBuilder} are: that interface is built
 * around a crafting-grid result and an advancement criterion, neither of which a drying recipe
 * has.
 */
public final class DryingRecipeBuilder {

    private final Ingredient input;
    private final ItemStack output;
    private final int dryTime;

    private DryingRecipeBuilder(Ingredient input, ItemStack output, int dryTime) {
        this.input = Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        if (output.isEmpty()) {
            throw new IllegalArgumentException("output must be a non-empty item stack");
        }
        this.output = output.copy();
        if (dryTime <= 0) {
            throw new IllegalArgumentException("dryTime must be a positive tick count: " + dryTime);
        }
        this.dryTime = dryTime;
    }

    /**
     * Begins a drying recipe.
     *
     * @param input   the item ingredient that dries
     * @param output  the item produced once drying completes
     * @param dryTime the server ticks the drying takes
     * @return a builder ready to {@link #save}
     */
    public static DryingRecipeBuilder drying(Ingredient input, ItemStack output, int dryTime) {
        return new DryingRecipeBuilder(input, output, dryTime);
    }

    /**
     * Writes the drying recipe to the data generator under {@code id}. The recipe carries no
     * advancement, so {@code null} is passed for the advancement holder.
     *
     * @param recipeOutput the data-generator sink
     * @param id           the recipe id
     */
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        recipeOutput.accept(id, new DryingRecipe(input, output, dryTime), null);
    }
}
