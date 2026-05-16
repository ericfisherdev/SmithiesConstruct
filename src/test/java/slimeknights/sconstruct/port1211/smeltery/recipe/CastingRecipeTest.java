package slimeknights.sconstruct.port1211.smeltery.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.core.HolderSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CastingRecipe#matches} (SMTCON-121). The fluid ingredient is built from a
 * vanilla fluid holder; the cast ingredient and the non-empty output are Mockito mocks so the
 * matcher runs without the frozen item registry.
 */
class CastingRecipeTest {

    private static final HolderSet<Fluid> WATER = HolderSet.direct(Fluids.WATER.builtInRegistryHolder());

    @Test
    void matchesAcceptsAMatchingTableInput() {
        CastingRecipe recipe = tableRecipe(acceptingCast());
        CastingRecipeInput input = new CastingRecipeInput(new FluidStack(Fluids.WATER, 288), ItemStack.EMPTY, false);

        assertTrue(recipe.matches(input, mock(Level.class)), "a table recipe matches a table input with enough fluid");
    }

    @Test
    void matchesRejectsABasinInputForATableRecipe() {
        CastingRecipe recipe = tableRecipe(acceptingCast());
        CastingRecipeInput basinInput = new CastingRecipeInput(new FluidStack(Fluids.WATER, 288), ItemStack.EMPTY, true);

        assertFalse(recipe.matches(basinInput, mock(Level.class)), "a table recipe must not fire in a basin");
    }

    @Test
    void matchesRejectsInsufficientFluid() {
        CastingRecipe recipe = tableRecipe(acceptingCast());
        CastingRecipeInput input = new CastingRecipeInput(new FluidStack(Fluids.WATER, 100), ItemStack.EMPTY, false);

        assertFalse(recipe.matches(input, mock(Level.class)), "too little poured fluid fails the match");
    }

    @Test
    void matchesRejectsAnInputWhoseCastTheIngredientDoesNotAccept() {
        Ingredient cast = mock(Ingredient.class);
        when(cast.test(any(ItemStack.class))).thenReturn(false);
        CastingRecipe recipe = tableRecipe(cast);
        CastingRecipeInput input = new CastingRecipeInput(new FluidStack(Fluids.WATER, 288), ItemStack.EMPTY, false);

        assertFalse(recipe.matches(input, mock(Level.class)), "a non-matching cast fails the match");
    }

    /** A table casting recipe needing 288 mB of water and the supplied cast ingredient. */
    private static CastingRecipe tableRecipe(Ingredient cast) {
        return new CastingRecipe(new FluidIngredient(WATER, 288), cast, false, 100, nonEmptyOutput(), false);
    }

    /**
     * A mocked non-empty {@link ItemStack} the casting recipe's canonical constructor accepts.
     * {@code copy()} returns a distinct non-empty mock, so the constructor's defensive copy
     * yields a separate instance rather than aliasing the original.
     */
    private static ItemStack nonEmptyOutput() {
        ItemStack copy = mock(ItemStack.class);
        when(copy.isEmpty()).thenReturn(false);
        when(copy.copy()).thenReturn(copy);
        ItemStack output = mock(ItemStack.class);
        when(output.isEmpty()).thenReturn(false);
        when(output.copy()).thenReturn(copy);
        return output;
    }

    /** An ingredient that accepts any cast item. */
    private static Ingredient acceptingCast() {
        Ingredient cast = mock(Ingredient.class);
        when(cast.test(any(ItemStack.class))).thenReturn(true);
        return cast;
    }
}
