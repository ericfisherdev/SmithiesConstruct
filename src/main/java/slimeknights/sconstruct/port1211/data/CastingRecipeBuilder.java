package slimeknights.sconstruct.port1211.data;

import java.util.Objects;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import slimeknights.sconstruct.port1211.smeltery.recipe.CastingRecipe;
import slimeknights.sconstruct.port1211.smeltery.recipe.FluidIngredient;

/**
 * Datagen builder for {@link CastingRecipe} JSONs (SMTCON-128). A casting recipe pours a fluid
 * onto a casting block to set an item; like {@link MeltingRecipeBuilder} it sits outside
 * vanilla's {@code RecipeBuilder} because it has no crafting grid and no unlock advancement.
 *
 * <p>The {@link #table} and {@link #basin} factories build the two common no-cast cases — pour
 * onto an empty casting table or basin. The cast-bearing recipes used by the tool-building flow
 * are deferred until the cast (mould) items they need are registered.
 */
public final class CastingRecipeBuilder {

    private final FluidIngredient fluid;
    private final Ingredient cast;
    private final boolean consumeCast;
    private final int coolingTime;
    private final ItemStack output;
    private final boolean basin;

    private CastingRecipeBuilder(FluidIngredient fluid, Ingredient cast, boolean consumeCast, int coolingTime, ItemStack output, boolean basin) {
        this.fluid = Objects.requireNonNull(fluid, "fluid");
        this.cast = Objects.requireNonNull(cast, "cast");
        this.consumeCast = consumeCast;
        if (coolingTime <= 0) {
            throw new IllegalArgumentException("coolingTime must be a positive tick count: " + coolingTime);
        }
        this.coolingTime = coolingTime;
        Objects.requireNonNull(output, "output");
        if (output.isEmpty()) {
            throw new IllegalArgumentException("output must be a non-empty item stack");
        }
        this.output = output.copy();
        this.basin = basin;
    }

    /**
     * A casting-table recipe: pour {@code fluid} onto an empty table to set {@code output}.
     *
     * @param fluid       the fluid ingredient that must be poured
     * @param output      the item produced
     * @param coolingTime the server ticks the cast takes to solidify
     * @return a builder ready to {@link #save}
     */
    public static CastingRecipeBuilder table(FluidIngredient fluid, ItemStack output, int coolingTime) {
        return new CastingRecipeBuilder(fluid, Ingredient.EMPTY, false, coolingTime, output, false);
    }

    /**
     * A casting-basin recipe: pour {@code fluid} into an empty basin to set {@code output}.
     *
     * @param fluid       the fluid ingredient that must be poured
     * @param output      the item produced
     * @param coolingTime the server ticks the cast takes to solidify
     * @return a builder ready to {@link #save}
     */
    public static CastingRecipeBuilder basin(FluidIngredient fluid, ItemStack output, int coolingTime) {
        return new CastingRecipeBuilder(fluid, Ingredient.EMPTY, false, coolingTime, output, true);
    }

    /** Writes the casting recipe to the data generator under {@code id}. */
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        recipeOutput.accept(id, new CastingRecipe(fluid, cast, consumeCast, coolingTime, output, basin), null);
    }
}
