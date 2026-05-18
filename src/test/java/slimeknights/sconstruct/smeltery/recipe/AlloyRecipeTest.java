package slimeknights.sconstruct.smeltery.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import net.minecraft.core.HolderSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AlloyRecipe#matches} (SMTCON-122). Fluid sets are built directly from
 * vanilla {@link Fluids} holders, so the matcher runs without the frozen fluid registry. Water
 * and lava stand in for the two molten-metal inputs of an alloy.
 */
class AlloyRecipeTest {

    private static final HolderSet<Fluid> WATER = HolderSet.direct(Fluids.WATER.builtInRegistryHolder());
    private static final HolderSet<Fluid> LAVA = HolderSet.direct(Fluids.LAVA.builtInRegistryHolder());
    private static final FluidStack OUTPUT = new FluidStack(Fluids.LAVA, 576);

    @Test
    void matchesWhenHotEnoughAndEveryInputIsPresent() {
        AlloyRecipe recipe = new AlloyRecipe(List.of(new FluidIngredient(WATER, 100), new FluidIngredient(LAVA, 50)), OUTPUT, 800);
        AlloyRecipeInput input = new AlloyRecipeInput(List.of(new FluidStack(Fluids.WATER, 144), new FluidStack(Fluids.LAVA, 60)), 800);

        assertTrue(recipe.matches(input, mock(Level.class)), "a hot smeltery holding both inputs runs the alloy");
    }

    @Test
    void doesNotMatchBelowTheTemperatureThreshold() {
        AlloyRecipe recipe = new AlloyRecipe(List.of(new FluidIngredient(WATER, 100), new FluidIngredient(LAVA, 50)), OUTPUT, 800);
        AlloyRecipeInput tooCold = new AlloyRecipeInput(List.of(new FluidStack(Fluids.WATER, 144), new FluidStack(Fluids.LAVA, 60)), 799);

        assertFalse(recipe.matches(tooCold, mock(Level.class)), "an alloy will not run below its temperature threshold");
    }

    @Test
    void doesNotMatchWhenAnInputFluidIsMissing() {
        AlloyRecipe recipe = new AlloyRecipe(List.of(new FluidIngredient(WATER, 100), new FluidIngredient(LAVA, 50)), OUTPUT, 800);
        AlloyRecipeInput missingLava = new AlloyRecipeInput(List.of(new FluidStack(Fluids.WATER, 144)), 800);

        assertFalse(recipe.matches(missingLava, mock(Level.class)), "a missing input fluid fails the match");
    }

    @Test
    void doesNotMatchWhenAnInputAmountIsTooLow() {
        AlloyRecipe recipe = new AlloyRecipe(List.of(new FluidIngredient(WATER, 100), new FluidIngredient(LAVA, 50)), OUTPUT, 800);
        AlloyRecipeInput notEnoughWater = new AlloyRecipeInput(List.of(new FluidStack(Fluids.WATER, 99), new FluidStack(Fluids.LAVA, 60)), 800);

        assertFalse(recipe.matches(notEnoughWater, mock(Level.class)), "an input below its required amount fails the match");
    }

    @Test
    void eachInputConsumesADistinctTankStack() {
        // Two inputs of the same fluid must be satisfied by two separate stacks, not one.
        AlloyRecipe recipe = new AlloyRecipe(List.of(new FluidIngredient(WATER, 100), new FluidIngredient(WATER, 100)), OUTPUT, 800);
        AlloyRecipeInput oneStack = new AlloyRecipeInput(List.of(new FluidStack(Fluids.WATER, 250)), 800);
        AlloyRecipeInput twoStacks = new AlloyRecipeInput(List.of(new FluidStack(Fluids.WATER, 144), new FluidStack(Fluids.WATER, 144)), 800);

        assertFalse(recipe.matches(oneStack, mock(Level.class)), "one stack cannot satisfy two same-fluid inputs");
        assertTrue(recipe.matches(twoStacks, mock(Level.class)), "two stacks satisfy two same-fluid inputs");
    }

    @Test
    void pairingBacktracksSoABroadInputDoesNotStarveASpecificOne() {
        // First input accepts water OR lava; second input accepts only water. If the broad
        // input greedily takes the water stack, the water-only input is left with lava and a
        // first-fit matcher would wrongly reject — backtracking finds the valid assignment.
        HolderSet<Fluid> waterOrLava = HolderSet.direct(Fluids.WATER.builtInRegistryHolder(), Fluids.LAVA.builtInRegistryHolder());
        AlloyRecipe recipe = new AlloyRecipe(List.of(new FluidIngredient(waterOrLava, 100), new FluidIngredient(WATER, 100)), OUTPUT, 800);
        AlloyRecipeInput input = new AlloyRecipeInput(List.of(new FluidStack(Fluids.WATER, 144), new FluidStack(Fluids.LAVA, 144)), 800);

        assertTrue(recipe.matches(input, mock(Level.class)), "backtracking finds the distinct assignment a greedy match would miss");
    }

    @Test
    void constructorRejectsFewerThanTwoInputs() {
        assertThrows(IllegalArgumentException.class, () -> new AlloyRecipe(List.of(new FluidIngredient(WATER, 100)), OUTPUT, 800), "an alloy recipe needs at least two inputs to combine");
    }

    @Test
    void constructorRejectsAnEmptyOutput() {
        assertThrows(IllegalArgumentException.class, () -> new AlloyRecipe(List.of(new FluidIngredient(WATER, 100), new FluidIngredient(LAVA, 50)), FluidStack.EMPTY, 800),
                "an alloy recipe must produce a non-empty output");
    }

    @Test
    void constructorRejectsANonPositiveTemperature() {
        List<FluidIngredient> inputs = List.of(new FluidIngredient(WATER, 100), new FluidIngredient(LAVA, 50));
        assertThrows(IllegalArgumentException.class, () -> new AlloyRecipe(inputs, OUTPUT, 0), "an alloy recipe temperature must be positive");
        assertThrows(IllegalArgumentException.class, () -> new AlloyRecipe(inputs, OUTPUT, -1), "an alloy recipe temperature must reject negative values");
    }
}
