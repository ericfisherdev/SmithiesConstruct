package slimeknights.sconstruct.port1211.smeltery.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.HolderSet;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FluidIngredient} (SMTCON-121). The fluid set is built directly from
 * vanilla {@link Fluids} holders, so the matcher is exercised without the frozen fluid registry.
 */
class FluidIngredientTest {

    private static final HolderSet<Fluid> WATER = HolderSet.direct(Fluids.WATER.builtInRegistryHolder());

    @Test
    void testAcceptsAMatchingFluidAtOrAboveTheRequiredAmount() {
        FluidIngredient ingredient = new FluidIngredient(WATER, 100);

        assertTrue(ingredient.test(new FluidStack(Fluids.WATER, 100)), "an exact amount of the matching fluid passes");
        assertTrue(ingredient.test(new FluidStack(Fluids.WATER, 250)), "more than the required amount passes");
    }

    @Test
    void testRejectsTooLittleFluid() {
        FluidIngredient ingredient = new FluidIngredient(WATER, 100);

        assertFalse(ingredient.test(new FluidStack(Fluids.WATER, 99)), "less than the required amount fails");
    }

    @Test
    void testRejectsAFluidOutsideTheSet() {
        FluidIngredient ingredient = new FluidIngredient(WATER, 100);

        assertFalse(ingredient.test(new FluidStack(Fluids.LAVA, 1000)), "a fluid outside the set fails regardless of amount");
    }

    @Test
    void testRejectsAnEmptyStack() {
        assertFalse(new FluidIngredient(WATER, 100).test(FluidStack.EMPTY), "an empty stack never matches");
    }

    @Test
    void constructorRejectsANonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> new FluidIngredient(WATER, 0), "a fluid ingredient must require a positive amount");
        assertThrows(IllegalArgumentException.class, () -> new FluidIngredient(WATER, -1), "a fluid ingredient must reject a negative amount");
    }
}
