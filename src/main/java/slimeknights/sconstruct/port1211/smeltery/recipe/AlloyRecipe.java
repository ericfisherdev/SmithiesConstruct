package slimeknights.sconstruct.port1211.smeltery.recipe;

import java.util.List;
import java.util.Objects;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Smeltery alloy recipe (SMTCON-122) — the third custom smeltery recipe type. An alloy recipe
 * combines several molten input fluids into one molten output fluid once the smeltery is hot
 * enough: three parts molten copper plus one part molten zinc become four parts molten brass.
 *
 * <p>Unlike melting and casting, an alloy recipe runs continuously — the controller checks every
 * tick whether its tank holds the inputs and is at least {@link #temperature()} kelvin, and if so
 * drains the inputs and adds the output. {@link #matches} performs that check; it pairs each
 * {@link FluidIngredient} with a <em>distinct</em> tank fluid stack by backtracking, so two
 * inputs cannot both be satisfied by the same stack and a broad ingredient cannot starve a more
 * specific one of a stack it needs.
 *
 * <p>An alloy yields a fluid, not an item, so {@link #assemble} and {@link #getResultItem} return
 * {@link ItemStack#EMPTY}; the controller reads {@link #inputs()} and {@link #output()} directly
 * to apply the alloy to its tank.
 *
 * @param inputs      the fluid ingredients consumed, each paired with a distinct tank stack
 * @param output      the fluid produced
 * @param temperature the minimum smeltery temperature in kelvin required to run this alloy
 */
public record AlloyRecipe(List<FluidIngredient> inputs, FluidStack output, int temperature) implements Recipe<AlloyRecipeInput> {

    /** The fewest inputs an alloy recipe can have — alloying combines at least two fluids. */
    private static final int MIN_INPUTS = 2;

    /** Datapack codec — loads an alloy recipe from its JSON definition. */
    public static final MapCodec<AlloyRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(FluidIngredient.CODEC.listOf().fieldOf("inputs").forGetter(AlloyRecipe::inputs),
            FluidStack.CODEC.fieldOf("output").forGetter(AlloyRecipe::output), ExtraCodecs.POSITIVE_INT.fieldOf("temperature").forGetter(AlloyRecipe::temperature)).apply(builder, AlloyRecipe::new));

    /** Network codec — syncs an alloy recipe to clients. */
    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> STREAM_CODEC = StreamCodec.composite(FluidIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AlloyRecipe::inputs,
            FluidStack.STREAM_CODEC, AlloyRecipe::output, ByteBufCodecs.VAR_INT, AlloyRecipe::temperature, AlloyRecipe::new);

    /** Validates the recipe and defensively copies the input list and fluid output. */
    public AlloyRecipe {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(output, "output");
        if (inputs.size() < MIN_INPUTS) {
            throw new IllegalArgumentException("an alloy recipe needs at least two inputs to combine");
        }
        if (output.isEmpty()) {
            throw new IllegalArgumentException("output must be a non-empty fluid stack");
        }
        if (temperature <= 0) {
            throw new IllegalArgumentException("temperature must be a positive kelvin value: " + temperature);
        }
        inputs = List.copyOf(inputs);
        output = output.copy();
    }

    /**
     * Whether the smeltery is hot enough and holds every input fluid. Each input must be paired
     * with a <em>distinct</em> tank stack. The pairing is found by backtracking rather than
     * first-fit: a greedy match could spend a stack on an earlier broad ingredient and then fail
     * a later specific one even though a valid distinct assignment exists.
     */
    @Override
    public boolean matches(AlloyRecipeInput input, Level level) {
        if (input.currentTemperature() < temperature) {
            return false;
        }
        List<FluidStack> contents = input.tankContents();
        return assignInputs(0, new boolean[contents.size()], contents);
    }

    /**
     * Backtracking assignment of inputs to distinct tank stacks: tries to pair input
     * {@code ingredientIndex} with each not-yet-used stack it accepts and recurses, undoing the
     * choice if the remaining inputs cannot then be satisfied.
     */
    private boolean assignInputs(int ingredientIndex, boolean[] used, List<FluidStack> contents) {
        if (ingredientIndex == inputs.size()) {
            return true;
        }
        FluidIngredient ingredient = inputs.get(ingredientIndex);
        for (int stack = 0; stack < contents.size(); stack++) {
            if (!used[stack] && ingredient.test(contents.get(stack))) {
                used[stack] = true;
                if (assignInputs(ingredientIndex + 1, used, contents)) {
                    return true;
                }
                used[stack] = false;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(AlloyRecipeInput input, HolderLookup.Provider registries) {
        // An alloy yields a fluid, not an item — the controller applies output() to its tank.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<AlloyRecipe> getSerializer() {
        return SmelteryRecipes.ALLOY_SERIALIZER.get();
    }

    @Override
    public RecipeType<AlloyRecipe> getType() {
        return SmelteryRecipes.ALLOY_TYPE.get();
    }
}
