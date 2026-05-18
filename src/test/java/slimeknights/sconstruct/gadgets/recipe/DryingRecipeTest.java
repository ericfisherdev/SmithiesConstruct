package slimeknights.sconstruct.gadgets.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DryingRecipe}'s registry-independent behaviour (SMTCON-137) — the
 * record accessors, the {@link DryingRecipe#matches} delegation, the {@link DryingRecipe#assemble}
 * output, and the compact-constructor invariants. The ingredient is a Mockito mock so the
 * matcher is exercised without the frozen item registry; codec round-trips and datapack loading
 * are registry-bound and verified in-game.
 */
class DryingRecipeTest {

    private static final ItemStack OUTPUT = new ItemStack(Items.STICK);

    @Test
    void recipeExposesItsConfiguredFields() {
        Ingredient input = mock(Ingredient.class);
        DryingRecipe recipe = new DryingRecipe(input, OUTPUT, 200);

        assertEquals(input, recipe.input(), "the recipe exposes its ingredient");
        assertEquals(200, recipe.dryTime(), "the recipe exposes its dry time");
        assertTrue(ItemStack.isSameItem(OUTPUT, recipe.output()), "the recipe exposes its item output");
    }

    @Test
    void matchesDelegatesToTheIngredient() {
        Ingredient input = mock(Ingredient.class);
        when(input.test(any(ItemStack.class))).thenReturn(true);
        DryingRecipe recipe = new DryingRecipe(input, OUTPUT, 200);

        assertTrue(recipe.matches(new SingleRecipeInput(ItemStack.EMPTY), mock(Level.class)), "matches accepts an input the ingredient tests true");
    }

    @Test
    void matchesRejectsAnInputTheIngredientDoesNotAccept() {
        Ingredient input = mock(Ingredient.class);
        when(input.test(any(ItemStack.class))).thenReturn(false);
        DryingRecipe recipe = new DryingRecipe(input, OUTPUT, 200);

        assertFalse(recipe.matches(new SingleRecipeInput(ItemStack.EMPTY), mock(Level.class)), "matches rejects an input the ingredient tests false");
    }

    @Test
    void assembleYieldsTheConfiguredOutput() {
        DryingRecipe recipe = new DryingRecipe(mock(Ingredient.class), OUTPUT, 200);
        HolderLookup.Provider registries = mock(HolderLookup.Provider.class);

        assertTrue(ItemStack.isSameItem(OUTPUT, recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY), registries)), "assemble yields the recipe output");
        assertTrue(ItemStack.isSameItem(OUTPUT, recipe.getResultItem(registries)), "getResultItem yields the recipe output");
    }

    @Test
    void dryingIsNotConstrainedByACraftingGrid() {
        DryingRecipe recipe = new DryingRecipe(mock(Ingredient.class), OUTPUT, 200);

        assertTrue(recipe.canCraftInDimensions(0, 0), "drying is not constrained by a crafting grid");
    }

    @Test
    void compactConstructorRejectsInvalidRecipes() {
        assertThrows(IllegalArgumentException.class, () -> new DryingRecipe(mock(Ingredient.class), OUTPUT, 0), "dryTime must be positive");
        assertThrows(IllegalArgumentException.class, () -> new DryingRecipe(mock(Ingredient.class), ItemStack.EMPTY, 200), "output must be non-empty");
    }
}
