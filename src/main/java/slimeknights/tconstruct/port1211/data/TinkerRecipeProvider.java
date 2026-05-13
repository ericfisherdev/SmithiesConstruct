package slimeknights.tconstruct.port1211.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.tconstruct.port1211.TConstruct;
import slimeknights.tconstruct.port1211.shared.Metal;
import slimeknights.tconstruct.port1211.shared.SharedBlocks;
import slimeknights.tconstruct.port1211.shared.SharedItems;
import slimeknights.tconstruct.port1211.shared.SharedMetals;

/**
 * Recipe data provider. Writes the standard per-metal crafting-table conversion recipes —
 * the 3×3/shapeless pairs that turn nuggets into ingots, ingots into storage blocks, and
 * back. Every metal in {@link SharedMetals#ALL} gets the ingot↔nugget pair; only metals
 * with a registered storage block (lead and nickel excluded — see
 * {@link SharedBlocks#skippedIds()}) get the ingot↔block pair.
 *
 * <p>The AC for SMTCON-43 envisions a separate {@code SharedRecipes.add(this)} helper, but
 * the {@link RecipeProvider#has(ItemLike)} criterion factory is {@code protected} on the
 * provider — extracting the recipe logic into a sibling class would require either
 * re-implementing the criterion inline or threading the protected method through a lambda.
 * Inlining the recipe pass into {@link #buildRecipes} keeps the criterion call simple and
 * the recipe id space owned by a single class.
 */
public final class TinkerRecipeProvider extends RecipeProvider {

    public TinkerRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        // Pin the three driver lists in size at the top of the pass so a desync turns into a
        // clear datagen failure rather than a silent mis-recipe — same defence the
        // TinkerItemTagsProvider pass applies.
        if (SharedItems.INGOTS.size() != SharedMetals.ALL.size() || SharedItems.NUGGETS.size() != SharedMetals.ALL.size()) {
            throw new IllegalStateException(
                    "Shared metals/items table mismatch: metals=" + SharedMetals.ALL.size() + ", ingots=" + SharedItems.INGOTS.size() + ", nuggets=" + SharedItems.NUGGETS.size());
        }
        for (int i = 0; i < SharedMetals.ALL.size(); i++) {
            Metal metal = SharedMetals.ALL.get(i);
            Item ingot = SharedItems.INGOTS.get(i).get();
            Item nugget = SharedItems.NUGGETS.get(i).get();

            DeferredBlock<Block> blockHolder = SharedBlocks.METAL_BLOCKS.get(metal.id());
            if (blockHolder != null) {
                Item blockItem = blockHolder.get().asItem();
                addBlockConversions(recipeOutput, metal.id(), ingot, blockItem);
            }
            addNuggetConversions(recipeOutput, metal.id(), ingot, nugget);
        }
    }

    private void addBlockConversions(RecipeOutput output, String metalId, Item ingot, Item blockItem) {
        // 9 ingots → 1 storage block. Categorised as BUILDING_BLOCKS so the result lands in
        // the right vanilla recipe-book tab; auto-id is the block's path so no explicit save id.
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, blockItem).pattern("###").pattern("###").pattern("###").define('#', ingot).unlockedBy("has_ingot", has(ingot)).save(output);
        // 1 storage block → 9 ingots. Explicit recipe id ("ingot_<metal>_from_block") because
        // the 3×3 nugget→ingot recipe below also outputs the ingot — without distinct ids the
        // two recipes would collide on the result's auto-id.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ingot, 9).requires(blockItem).unlockedBy("has_block", has(blockItem)).save(output,
                ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, "ingot_" + metalId + "_from_block"));
    }

    private void addNuggetConversions(RecipeOutput output, String metalId, Item ingot, Item nugget) {
        // 9 nuggets → 1 ingot. Disambiguated from the block→9 ingots recipe with an explicit
        // "_from_nuggets" suffix.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ingot).pattern("###").pattern("###").pattern("###").define('#', nugget).unlockedBy("has_nugget", has(nugget)).save(output,
                ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, "ingot_" + metalId + "_from_nuggets"));
        // 1 ingot → 9 nuggets. Auto-id is the nugget's path (nugget_<metal>) — no other
        // recipe targets the nugget, so no collision.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, nugget, 9).requires(ingot).unlockedBy("has_ingot", has(ingot)).save(output);
    }
}
