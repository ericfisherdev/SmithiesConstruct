package slimeknights.sconstruct.port1211.smeltery.recipe;

import java.util.List;
import java.util.Objects;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The {@link RecipeInput} the smeltery controller hands to the {@link AlloyRecipe} matcher
 * (SMTCON-122) — a snapshot of the smeltery's molten contents and its current temperature. An
 * alloy recipe combines fluids, so the controller offers every fluid stack currently in its tank
 * along with how hot it is running.
 *
 * <p>{@link RecipeInput} models an item inventory, but an alloy recipe consumes no items — so
 * {@link #size} is {@code 0} and {@link #getItem} never returns anything. The fluid contents and
 * temperature are read by {@link AlloyRecipe#matches} directly.
 *
 * @param tankContents      the fluid stacks currently held in the smeltery
 * @param currentTemperature the smeltery's current internal temperature in kelvin
 */
public record AlloyRecipeInput(List<FluidStack> tankContents, int currentTemperature) implements RecipeInput {

    /**
     * Defensively deep-copies the fluid-stack list — both the list and every {@link FluidStack}
     * in it — so the captured snapshot cannot change after the controller mutates its tank.
     */
    public AlloyRecipeInput {
        Objects.requireNonNull(tankContents, "tankContents");
        tankContents = tankContents.stream().map(FluidStack::copy).toList();
    }

    @Override
    public ItemStack getItem(int index) {
        throw new IndexOutOfBoundsException("an alloy recipe input holds no items, got index " + index);
    }

    @Override
    public int size() {
        return 0;
    }
}
