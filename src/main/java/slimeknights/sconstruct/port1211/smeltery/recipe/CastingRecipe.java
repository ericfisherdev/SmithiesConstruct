package slimeknights.sconstruct.port1211.smeltery.recipe;

import java.util.Objects;

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
import net.minecraft.world.level.Level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Smeltery casting recipe (SMTCON-121) — the second custom smeltery recipe type. A casting
 * recipe consumes a poured fluid (and optionally a cast item) to produce an output item: pour
 * molten iron over a gold ingot cast on a casting table to get an iron ingot, or pour molten
 * iron into a casting basin with no cast to get an iron block.
 *
 * <p>One recipe type serves both casting blocks; {@link #isBasin()} pins each recipe to one of
 * them so a basin recipe cannot fire on a table and vice versa. {@link #cast()} is an
 * item ingredient — {@link Ingredient#EMPTY} for a recipe that needs no cast — and
 * {@link #consumeCast()} decides whether a matched cast is used up (a sand cast) or kept (a gold
 * ingot cast). {@link #coolingTime()} is how many server ticks the poured fluid takes to set.
 *
 * @param fluid       the fluid ingredient that must be poured
 * @param cast        the item ingredient required in the block, or {@link Ingredient#EMPTY}
 * @param consumeCast whether a matched cast item is consumed when the cast completes
 * @param coolingTime the number of server ticks the cast takes to solidify
 * @param output      the item produced
 * @param isBasin     {@code true} for a casting basin recipe, {@code false} for a casting table
 */
public record CastingRecipe(FluidIngredient fluid, Ingredient cast, boolean consumeCast, int coolingTime, ItemStack output, boolean isBasin) implements Recipe<CastingRecipeInput> {

    /** Datapack codec — loads a casting recipe from its JSON definition. */
    public static final MapCodec<CastingRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(FluidIngredient.CODEC.fieldOf("fluid").forGetter(CastingRecipe::fluid),
            Ingredient.CODEC.optionalFieldOf("cast", Ingredient.EMPTY).forGetter(CastingRecipe::cast), Codec.BOOL.optionalFieldOf("consume_cast", Boolean.FALSE).forGetter(CastingRecipe::consumeCast),
            ExtraCodecs.POSITIVE_INT.fieldOf("cooling_time").forGetter(CastingRecipe::coolingTime), ItemStack.CODEC.fieldOf("output").forGetter(CastingRecipe::output),
            Codec.BOOL.fieldOf("is_basin").forGetter(CastingRecipe::isBasin)).apply(builder, CastingRecipe::new));

    /** Network codec — syncs a casting recipe to clients. */
    public static final StreamCodec<RegistryFriendlyByteBuf, CastingRecipe> STREAM_CODEC = StreamCodec.composite(FluidIngredient.STREAM_CODEC, CastingRecipe::fluid, Ingredient.CONTENTS_STREAM_CODEC,
            CastingRecipe::cast, ByteBufCodecs.BOOL, CastingRecipe::consumeCast, ByteBufCodecs.VAR_INT, CastingRecipe::coolingTime, ItemStack.STREAM_CODEC, CastingRecipe::output, ByteBufCodecs.BOOL,
            CastingRecipe::isBasin, CastingRecipe::new);

    /** Validates the recipe and defensively copies the mutable {@link ItemStack} output. */
    public CastingRecipe {
        Objects.requireNonNull(fluid, "fluid");
        Objects.requireNonNull(cast, "cast");
        Objects.requireNonNull(output, "output");
        if (coolingTime <= 0) {
            throw new IllegalArgumentException("coolingTime must be a positive tick count: " + coolingTime);
        }
        if (output.isEmpty()) {
            throw new IllegalArgumentException("output must be a non-empty item stack");
        }
        output = output.copy();
    }

    @Override
    public boolean matches(CastingRecipeInput input, Level level) {
        return isBasin == input.isBasin() && fluid.test(input.fluid()) && cast.test(input.cast());
    }

    @Override
    public ItemStack assemble(CastingRecipeInput input, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        // Copy on read too — a caller (recipe book, JEI) must not be able to mutate the recipe's
        // stored output through the returned stack.
        return output.copy();
    }

    @Override
    public RecipeSerializer<CastingRecipe> getSerializer() {
        return SmelteryRecipes.CASTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<CastingRecipe> getType() {
        return SmelteryRecipes.CASTING_TYPE.get();
    }
}
