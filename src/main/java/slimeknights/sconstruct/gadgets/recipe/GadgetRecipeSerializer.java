package slimeknights.sconstruct.gadgets.recipe;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import com.mojang.serialization.MapCodec;

/**
 * Generic {@link RecipeSerializer} for the gadgets' custom recipe types — a record that pairs a
 * recipe's datapack {@link MapCodec} with its network {@link StreamCodec}. Mirrors the
 * smeltery's {@code SmelteryRecipeSerializer}: every gadget recipe is a plain record carrying
 * its own two codecs, so one shared serializer record removes the boilerplate of a
 * near-identical serializer class per recipe type.
 *
 * @param codec       the datapack codec used to load the recipe from JSON
 * @param streamCodec the network codec used to sync the recipe to clients
 * @param <R>         the recipe type this serializer handles
 */
public record GadgetRecipeSerializer<R extends Recipe<?>>(MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> streamCodec) implements RecipeSerializer<R> {
}
