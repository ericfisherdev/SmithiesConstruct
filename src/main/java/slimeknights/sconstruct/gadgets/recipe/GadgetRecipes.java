package slimeknights.sconstruct.gadgets.recipe;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.common.TinkerRegistries;

/**
 * Registration hub for the gadgets' custom recipe types and serializers. SMTCON-137 registers
 * the first: the {@link DryingRecipe} type used by the drying rack.
 *
 * <p>Mirrors the smeltery's {@code SmelteryRecipes} hub — typed {@code public static final}
 * holders and an {@link #init()} no-op that forces the static initialisers to run during mod
 * construction so the {@link TinkerRegistries} {@code DeferredRegister}s see every entry before
 * their registry events fire. Invoked from {@code SConstruct} for now; the call relocates into
 * the gadgets pulse when that pulse is wired.
 */
public final class GadgetRecipes {

    /** The drying recipe type — a simple type, looked up by iterating the type. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<DryingRecipe>> DRYING_TYPE = TinkerRegistries.RECIPE_TYPES.register("drying",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "drying")));

    /** The drying recipe serializer — pairs {@link DryingRecipe#CODEC} with its stream codec. */
    public static final DeferredHolder<RecipeSerializer<?>, GadgetRecipeSerializer<DryingRecipe>> DRYING_SERIALIZER = TinkerRegistries.RECIPE_SERIALIZERS.register("drying",
            () -> new GadgetRecipeSerializer<>(DryingRecipe.CODEC, DryingRecipe.STREAM_CODEC));

    private GadgetRecipes() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        Objects.requireNonNull(DRYING_TYPE);
        Objects.requireNonNull(DRYING_SERIALIZER);
    }
}
