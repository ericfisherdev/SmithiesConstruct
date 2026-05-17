package slimeknights.sconstruct.port1211.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.HolderSet;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import slimeknights.sconstruct.port1211.smeltery.recipe.AlloyRecipe;
import slimeknights.sconstruct.port1211.smeltery.recipe.FluidIngredient;

/**
 * Datagen builder for {@link AlloyRecipe} JSONs (SMTCON-128). An alloy recipe combines two or
 * more molten input fluids into one molten output above a temperature threshold; the builder
 * collects the inputs with {@link #input} and writes the recipe with {@link #save}.
 */
public final class AlloyRecipeBuilder {

    private final List<FluidIngredient> inputs = new ArrayList<>();
    private final FluidStack output;
    private final int temperature;

    private AlloyRecipeBuilder(FluidStack output, int temperature) {
        Objects.requireNonNull(output, "output");
        if (output.isEmpty()) {
            throw new IllegalArgumentException("output must be a non-empty fluid stack");
        }
        if (temperature <= 0) {
            throw new IllegalArgumentException("temperature must be a positive kelvin value: " + temperature);
        }
        this.output = output.copy();
        this.temperature = temperature;
    }

    /**
     * Begins an alloy recipe.
     *
     * @param output      the molten fluid produced
     * @param temperature the minimum smeltery temperature in kelvin the alloy requires
     * @return a builder to add inputs to
     */
    public static AlloyRecipeBuilder alloy(FluidStack output, int temperature) {
        return new AlloyRecipeBuilder(output, temperature);
    }

    /**
     * Adds an input: {@code amount} millibuckets of {@code fluid}.
     *
     * @param fluid  the molten input fluid
     * @param amount the millibuckets of it the alloy consumes
     * @return this builder
     */
    public AlloyRecipeBuilder input(Fluid fluid, int amount) {
        Objects.requireNonNull(fluid, "fluid");
        if (amount <= 0) {
            throw new IllegalArgumentException("input amount must be a positive millibucket value: " + amount);
        }
        inputs.add(new FluidIngredient(HolderSet.direct(fluid.builtInRegistryHolder()), amount));
        return this;
    }

    /** The fewest inputs an alloy recipe combines. */
    private static final int MIN_INPUTS = 2;

    /** Writes the alloy recipe to the data generator under {@code id}. */
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        if (inputs.size() < MIN_INPUTS) {
            throw new IllegalStateException("an alloy recipe needs at least two inputs, got " + inputs.size());
        }
        recipeOutput.accept(id, new AlloyRecipe(List.copyOf(inputs), output, temperature), null);
    }
}
