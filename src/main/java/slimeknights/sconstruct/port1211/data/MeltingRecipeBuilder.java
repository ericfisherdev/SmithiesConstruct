package slimeknights.sconstruct.port1211.data;

import java.util.Objects;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

import slimeknights.sconstruct.port1211.smeltery.recipe.MeltingRecipe;

/**
 * Datagen builder for {@link MeltingRecipe} JSONs (SMTCON-127). A melting recipe has no
 * crafting-grid shape and no unlock advancement, so this builder is a thin value carrier — it
 * collects the four recipe fields and writes the recipe straight to the {@link RecipeOutput}.
 *
 * <p>Kept separate from vanilla's {@code RecipeBuilder} hierarchy on purpose: that interface is
 * built around an item result and an advancement criterion, neither of which a fluid-producing
 * melting recipe has.
 */
public final class MeltingRecipeBuilder {

    private final Ingredient input;
    private final FluidStack output;
    private final int temperature;
    private final int time;

    private MeltingRecipeBuilder(Ingredient input, FluidStack output, int temperature, int time) {
        this.input = Objects.requireNonNull(input, "input");
        this.output = Objects.requireNonNull(output, "output");
        this.temperature = temperature;
        this.time = time;
    }

    /**
     * Begins a melting recipe.
     *
     * @param input       the item ingredient that melts
     * @param output      the fluid produced
     * @param temperature the minimum smeltery temperature in kelvin the melt requires
     * @param time        the server ticks the melt takes
     * @return a builder ready to {@link #save}
     */
    public static MeltingRecipeBuilder melting(Ingredient input, FluidStack output, int temperature, int time) {
        return new MeltingRecipeBuilder(input, output, temperature, time);
    }

    /**
     * Writes the melting recipe to the data generator under {@code id}. The recipe carries no
     * advancement, so {@code null} is passed for the advancement holder.
     *
     * @param recipeOutput the data-generator sink
     * @param id           the recipe id
     */
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        recipeOutput.accept(id, new MeltingRecipe(input, output, temperature, time), null);
    }
}
