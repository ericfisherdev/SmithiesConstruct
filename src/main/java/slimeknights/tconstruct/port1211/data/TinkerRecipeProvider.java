package slimeknights.tconstruct.port1211.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

/**
 * Phase-1 stub for tconstruct recipes. Empty body — {@link #buildRecipes} is a no-op so the
 * provider produces zero files; Phase 2+ tools/smeltery pulses fill {@link #buildRecipes} with
 * shaped/shapeless and forthcoming smeltery-specific recipe calls.
 */
public final class TinkerRecipeProvider extends RecipeProvider {

    public TinkerRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        // No recipes in Phase 1 — Phase 2+ tools/smeltery pulses append here.
    }
}
