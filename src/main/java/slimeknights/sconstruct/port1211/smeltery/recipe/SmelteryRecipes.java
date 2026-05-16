package slimeknights.sconstruct.port1211.smeltery.recipe;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.TinkerRegistries;

/**
 * Registration hub for the smeltery's custom recipe types and serializers (SMTCON-120). Owns the
 * {@link RecipeType} and {@link RecipeSerializer} for each smeltery recipe — melting now, with
 * casting (SMTCON-121) and alloy (SMTCON-122) appending here as those land.
 *
 * <p>Mirrors the {@code SmelteryComponents} pattern: typed {@code public static final} holders
 * for direct downstream references and an {@link #init()} no-op that forces the static
 * initialisers to run during mod construction so the {@link TinkerRegistries} {@code
 * DeferredRegister}s see every entry before their registry events fire. Invoked from
 * {@code SConstruct} for now; the call relocates into the smeltery pulse at SMTCON-131.
 */
public final class SmelteryRecipes {

    /**
     * The melting recipe type — a {@link RecipeType#simple simple} type, since melting recipes
     * are looked up by iterating the type rather than by a structured recipe-book key.
     */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MeltingRecipe>> MELTING_TYPE = TinkerRegistries.RECIPE_TYPES.register("melting",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "melting")));

    /** The melting recipe serializer — pairs {@link MeltingRecipe#CODEC} with its stream codec. */
    public static final DeferredHolder<RecipeSerializer<?>, SmelteryRecipeSerializer<MeltingRecipe>> MELTING_SERIALIZER = TinkerRegistries.RECIPE_SERIALIZERS.register("melting",
            () -> new SmelteryRecipeSerializer<>(MeltingRecipe.CODEC, MeltingRecipe.STREAM_CODEC));

    /** The casting recipe type — one type serving both the casting table and the casting basin. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<CastingRecipe>> CASTING_TYPE = TinkerRegistries.RECIPE_TYPES.register("casting",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "casting")));

    /** The casting recipe serializer — pairs {@link CastingRecipe#CODEC} with its stream codec. */
    public static final DeferredHolder<RecipeSerializer<?>, SmelteryRecipeSerializer<CastingRecipe>> CASTING_SERIALIZER = TinkerRegistries.RECIPE_SERIALIZERS.register("casting",
            () -> new SmelteryRecipeSerializer<>(CastingRecipe.CODEC, CastingRecipe.STREAM_CODEC));

    private SmelteryRecipes() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        Objects.requireNonNull(MELTING_TYPE);
        Objects.requireNonNull(MELTING_SERIALIZER);
        Objects.requireNonNull(CASTING_TYPE);
        Objects.requireNonNull(CASTING_SERIALIZER);
    }
}
