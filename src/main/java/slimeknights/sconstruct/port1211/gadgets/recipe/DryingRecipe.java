package slimeknights.sconstruct.port1211.gadgets.recipe;

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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Drying-rack recipe (SMTCON-137) — turns one item into another after a fixed number of server
 * ticks on a drying rack. The rack holds a single item; when that item matches a drying
 * recipe's {@link #input ingredient}, the rack's block entity counts up to {@link #dryTime} and
 * then swaps the slot's contents for the recipe {@link #output}.
 *
 * <p>Implemented as a record so the datapack {@link #CODEC} and network {@link #STREAM_CODEC}
 * are a direct mechanical mapping of the three fields.
 *
 * @param input   the item ingredient that dries
 * @param output  the item produced once drying completes
 * @param dryTime the number of server ticks the drying takes
 */
public record DryingRecipe(Ingredient input, ItemStack output, int dryTime) implements Recipe<SingleRecipeInput> {

    /**
     * Validates the recipe and defensively copies the mutable {@link ItemStack} output. The
     * datapack {@link #CODEC} already constrains its fields, but the {@link #STREAM_CODEC}
     * network path and direct construction do not — so the invariants are enforced here, at the
     * one point every construction path passes through.
     */
    public DryingRecipe {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        if (dryTime <= 0) {
            throw new IllegalArgumentException("dryTime must be a positive tick count: " + dryTime);
        }
        if (output.isEmpty()) {
            throw new IllegalArgumentException("output must be a non-empty item stack");
        }
        output = output.copy();
    }

    /** Datapack codec — loads a drying recipe from its JSON definition. */
    public static final MapCodec<DryingRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(Ingredient.CODEC.fieldOf("input").forGetter(DryingRecipe::input),
            ItemStack.CODEC.fieldOf("output").forGetter(DryingRecipe::output), ExtraCodecs.POSITIVE_INT.fieldOf("dry_time").forGetter(DryingRecipe::dryTime)).apply(builder, DryingRecipe::new));

    /** Network codec — syncs a drying recipe to clients. */
    public static final StreamCodec<RegistryFriendlyByteBuf, DryingRecipe> STREAM_CODEC = StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC, DryingRecipe::input, ItemStack.STREAM_CODEC,
            DryingRecipe::output, ByteBufCodecs.VAR_INT, DryingRecipe::dryTime, DryingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public RecipeSerializer<DryingRecipe> getSerializer() {
        return GadgetRecipes.DRYING_SERIALIZER.get();
    }

    @Override
    public RecipeType<DryingRecipe> getType() {
        return GadgetRecipes.DRYING_TYPE.get();
    }
}
