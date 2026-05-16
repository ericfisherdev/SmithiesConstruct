package slimeknights.sconstruct.port1211.smeltery.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MeltingRecipe}'s registry-independent behaviour (SMTCON-120) — the
 * record accessors and the {@link MeltingRecipe#matches} delegation. The ingredient is a Mockito
 * mock so the matcher can be exercised without the frozen item registry; the codec round-trip
 * and datapack loading are registry-bound and are covered by SMTCON-172 / verified in-game.
 */
class MeltingRecipeTest {

    private static final FluidStack OUTPUT = new FluidStack(Fluids.WATER, 144);

    @Test
    void recipeExposesItsConfiguredFields() {
        Ingredient input = mock(Ingredient.class);
        MeltingRecipe recipe = new MeltingRecipe(input, OUTPUT, 1500, 100);

        assertEquals(input, recipe.input(), "the recipe exposes its ingredient");
        assertEquals(1500, recipe.temperature(), "the recipe exposes its temperature threshold");
        assertEquals(100, recipe.time(), "the recipe exposes its melt time");
        assertEquals(144, recipe.output().getAmount(), "the recipe exposes its fluid output amount");
    }

    @Test
    void matchesDelegatesToTheIngredient() {
        Ingredient input = mock(Ingredient.class);
        when(input.test(any(ItemStack.class))).thenReturn(true);
        MeltingRecipe recipe = new MeltingRecipe(input, OUTPUT, 1500, 100);

        assertTrue(recipe.matches(new SingleRecipeInput(ItemStack.EMPTY), mock(Level.class)), "matches accepts an input the ingredient tests true");
    }

    @Test
    void matchesRejectsAnInputTheIngredientDoesNotAccept() {
        Ingredient input = mock(Ingredient.class);
        when(input.test(any(ItemStack.class))).thenReturn(false);
        MeltingRecipe recipe = new MeltingRecipe(input, OUTPUT, 1500, 100);

        assertFalse(recipe.matches(new SingleRecipeInput(ItemStack.EMPTY), mock(Level.class)), "matches rejects an input the ingredient tests false");
    }

    @Test
    void aMeltYieldsNoItemResult() {
        MeltingRecipe recipe = new MeltingRecipe(mock(Ingredient.class), OUTPUT, 1500, 100);
        HolderLookup.Provider registries = mock(HolderLookup.Provider.class);

        assertTrue(recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY), registries).isEmpty(), "a melt assembles no item — its product is the fluid output");
        assertTrue(recipe.getResultItem(registries).isEmpty(), "a melt has no item result");
        assertTrue(recipe.canCraftInDimensions(0, 0), "melting is not constrained by a crafting grid");
    }
}
