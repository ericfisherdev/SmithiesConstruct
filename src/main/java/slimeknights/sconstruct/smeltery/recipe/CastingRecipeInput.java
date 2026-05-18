package slimeknights.sconstruct.smeltery.recipe;

import java.util.Objects;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The {@link RecipeInput} a casting block hands to the {@link CastingRecipe} matcher (SMTCON-121)
 * — the fluid that was poured, the optional cast item in the block, and which casting block is
 * asking. {@code isBasin} lets one recipe type serve both casting blocks: a table recipe only
 * matches a table's input and a basin recipe only matches a basin's.
 *
 * <p>{@link RecipeInput} models an item inventory, so {@link #getItem} exposes the single
 * {@link #cast} slot; the poured {@link #fluid} is read by {@link CastingRecipe#matches} directly.
 *
 * @param fluid   the fluid currently poured into the casting block
 * @param cast    the cast item in the block, or {@link ItemStack#EMPTY} when none
 * @param isBasin {@code true} for a casting basin, {@code false} for a casting table
 */
public record CastingRecipeInput(FluidStack fluid, ItemStack cast, boolean isBasin) implements RecipeInput {

    /** Validates the fluid and cast references. */
    public CastingRecipeInput {
        Objects.requireNonNull(fluid, "fluid");
        Objects.requireNonNull(cast, "cast");
    }

    @Override
    public ItemStack getItem(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("casting input has a single cast slot, got index " + index);
        }
        return cast;
    }

    @Override
    public int size() {
        return 1;
    }
}
