package slimeknights.sconstruct.port1211.smeltery.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Smeltery melting recipe (SMTCON-120) — the first of the three custom smeltery recipe types.
 * It melts a single item ingredient into a fluid: an iron ore becomes molten iron, a sand block
 * becomes molten glass, and so on.
 *
 * <p>The recipe carries a {@code temperature} threshold in kelvin. {@link #matches} only checks
 * the ingredient — the smeltery controller is responsible for comparing its current internal
 * temperature against {@link #temperature()} before it actually starts a melt, so a recipe that
 * matches an item still will not run in a smeltery that is not hot enough.
 *
 * <p>Implemented as a record so the datapack {@link #CODEC} and network {@link #STREAM_CODEC}
 * are a direct mechanical mapping of the four fields. A melting recipe produces a fluid, not an
 * item, so {@link #assemble} and {@link #getResultItem} return {@link ItemStack#EMPTY}; the
 * fluid {@link #output()} is read directly by the controller.
 *
 * @param input       the item ingredient that melts
 * @param output      the fluid produced, with its amount in millibuckets
 * @param temperature the minimum smeltery temperature in kelvin required to run this melt
 * @param time        the number of server ticks the melt takes to complete
 */
public record MeltingRecipe(Ingredient input, FluidStack output, int temperature, int time) implements Recipe<SingleRecipeInput> {

    /** Datapack codec — loads a melting recipe from its JSON definition. */
    public static final MapCodec<MeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder
            .group(Ingredient.CODEC.fieldOf("input").forGetter(MeltingRecipe::input), FluidStack.CODEC.fieldOf("output").forGetter(MeltingRecipe::output),
                    ExtraCodecs.POSITIVE_INT.fieldOf("temperature").forGetter(MeltingRecipe::temperature), ExtraCodecs.POSITIVE_INT.fieldOf("time").forGetter(MeltingRecipe::time))
            .apply(builder, MeltingRecipe::new));

    /** Network codec — syncs a melting recipe to clients. */
    public static final StreamCodec<RegistryFriendlyByteBuf, MeltingRecipe> STREAM_CODEC = StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC, MeltingRecipe::input, FluidStack.STREAM_CODEC,
            MeltingRecipe::output, ByteBufCodecs.VAR_INT, MeltingRecipe::temperature, ByteBufCodecs.VAR_INT, MeltingRecipe::time, MeltingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        // A melt yields a fluid, not an item — the controller reads output() directly.
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
    public RecipeSerializer<MeltingRecipe> getSerializer() {
        return SmelteryRecipes.MELTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<MeltingRecipe> getType() {
        return SmelteryRecipes.MELTING_TYPE.get();
    }
}
