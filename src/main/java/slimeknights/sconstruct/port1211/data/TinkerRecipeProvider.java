package slimeknights.sconstruct.port1211.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.shared.Metal;
import slimeknights.sconstruct.port1211.shared.SharedBlocks;
import slimeknights.sconstruct.port1211.shared.SharedItems;
import slimeknights.sconstruct.port1211.shared.SharedMetals;
import slimeknights.sconstruct.port1211.tools.PartBuilderRegistry;
import slimeknights.sconstruct.port1211.tools.PatternChestRegistry;
import slimeknights.sconstruct.port1211.tools.StencilTableRegistry;
import slimeknights.sconstruct.port1211.tools.ToolStationRegistry;
import slimeknights.sconstruct.port1211.world.WorldBlocks;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

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

        // Phase-3 slime blocks: 4 slimeballs → 1 coloured slime block (shaped 2×2), and the
        // reverse 1 block → 4 slimeballs (shapeless). Mirrors vanilla's slime-block recipe
        // pair, replicated per colour. The colour-to-slimeball mapping is centralised in
        // {@link #slimeballFor} so a future fifth colour lights up by extending SlimeColor
        // and that one switch — no recipe edit required.
        for (SlimeColor color : SlimeColor.values()) {
            Block slimeBlock = WorldBlocks.SLIME_BLOCKS.get(color).get();
            Item slimeball = slimeballFor(color).get();
            addSlimeBlockConversions(recipeOutput, color.id(), slimeball, slimeBlock.asItem());
        }

        // SMTCON-95: crafting recipes for the 5 tool-pulse station blocks. Each follows the
        // same "self-describing ingredient" rule of thumb — the station's own pattern hints
        // at how it's made, and the unlock criterion fires on the simplest precursor item the
        // player would already have. Recipe ids are auto-derived from the result's registry
        // path; collisions are impossible because each station has a unique block id.
        addStationRecipes(recipeOutput);
    }

    /**
     * Emit the 5 station crafting recipes. See in-line javadoc on each helper for the chosen
     * shape and the reasoning behind it.
     */
    private void addStationRecipes(RecipeOutput output) {
        Item blankPattern = StencilTableRegistry.BLANK_PATTERN.get();

        // Pattern Chest = vanilla chest + 1 blank pattern (shapeless). The chest provides the
        // 32-slot inventory shape; the pattern flags it as a "this stores patterns" container
        // semantically. Shapeless because the order doesn't matter for a 2-ingredient combine.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, PatternChestRegistry.PATTERN_CHEST.get().asItem()).requires(Items.CHEST).requires(blankPattern)
                .unlockedBy("has_blank_pattern", has(blankPattern)).save(output);

        // Stencil Table = blank pattern on top of a plank table.
        //   Row 0: blank_pattern in centre slot.
        //   Row 1: three planks (any wood — drawn from the planks item tag).
        // Unlocks on first blank-pattern pickup (which is itself crafted from sticks, so the
        // player can reach it before owning any of the other stations).
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, StencilTableRegistry.STENCIL_TABLE.get().asItem()).pattern(" B ").pattern("PPP").define('B', blankPattern).define('P', ItemTags.PLANKS)
                .unlockedBy("has_blank_pattern", has(blankPattern)).save(output);

        // Part Builder = the second "wooden" station — built from blank patterns flanked by
        // planks, on a plank base. The two patterns on the top row evoke the part-template
        // workflow this station drives.
        //   Row 0: pattern, plank, pattern.
        //   Row 1: three planks.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, PartBuilderRegistry.PART_BUILDER.get().asItem()).pattern("BPB").pattern("PPP").define('B', blankPattern).define('P', ItemTags.PLANKS)
                .unlockedBy("has_blank_pattern", has(blankPattern)).save(output);

        // Tool Station = "workbench for tools": blank pattern on top of a vanilla crafting
        // table (shapeless). The crafting-table ingredient anchors the recipe at a point in
        // progression every player has already reached.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ToolStationRegistry.TOOL_STATION.get().asItem()).requires(Items.CRAFTING_TABLE).requires(blankPattern)
                .unlockedBy("has_crafting_table", has(Items.CRAFTING_TABLE)).save(output);

        // Tool Forge = upgrade of the tool station, reinforced with iron. Iron edges, station
        // in centre, iron corners — pictures the legacy "forge" upgrade where the station
        // gains an iron frame.
        //   Row 0: iron, iron, iron.
        //   Row 1: iron, tool_station, iron.
        //   Row 2: iron, iron, iron.
        // Unlock criterion fires on first iron ingot — the player will already have iron by
        // the time they need the forge.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ToolStationRegistry.TOOL_FORGE.get().asItem()).pattern("III").pattern("ISI").pattern("III").define('I', Items.IRON_INGOT)
                .define('S', ToolStationRegistry.TOOL_STATION.get().asItem()).unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(output);
    }

    private static DeferredItem<Item> slimeballFor(SlimeColor color) {
        return switch (color) {
        case BLUE -> SharedItems.SLIMEBALL_BLUE;
        case PURPLE -> SharedItems.SLIMEBALL_PURPLE;
        case MAGMA -> SharedItems.SLIMEBALL_MAGMA;
        case BLOOD -> SharedItems.SLIMEBALL_BLOOD;
        };
    }

    private void addSlimeBlockConversions(RecipeOutput output, String colorId, Item slimeball, Item blockItem) {
        // 4 slimeballs → 1 slime block. BUILDING_BLOCKS category so the result lands in the
        // building-blocks recipe-book tab alongside vanilla slime block. Auto-id is the
        // block's registry path so no explicit save id.
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, blockItem).pattern("SS").pattern("SS").define('S', slimeball).unlockedBy("has_slimeball", has(slimeball)).save(output);
        // 1 slime block → 4 slimeballs. Explicit recipe id ("slimeball_<color>_from_block")
        // because the slimeball item's auto-id would collide if any other recipe also produced
        // it (none today, but the explicit id keeps the recipe self-describing).
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, slimeball, 4).requires(blockItem).unlockedBy("has_slime_block", has(blockItem)).save(output,
                ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "slimeball_" + colorId + "_from_block"));
    }

    private void addBlockConversions(RecipeOutput output, String metalId, Item ingot, Item blockItem) {
        // 9 ingots → 1 storage block. Categorised as BUILDING_BLOCKS so the result lands in
        // the right vanilla recipe-book tab; auto-id is the block's path so no explicit save id.
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, blockItem).pattern("###").pattern("###").pattern("###").define('#', ingot).unlockedBy("has_ingot", has(ingot)).save(output);
        // 1 storage block → 9 ingots. Explicit recipe id ("ingot_<metal>_from_block") because
        // the 3×3 nugget→ingot recipe below also outputs the ingot — without distinct ids the
        // two recipes would collide on the result's auto-id.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ingot, 9).requires(blockItem).unlockedBy("has_block", has(blockItem)).save(output,
                ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "ingot_" + metalId + "_from_block"));
    }

    private void addNuggetConversions(RecipeOutput output, String metalId, Item ingot, Item nugget) {
        // 9 nuggets → 1 ingot. Disambiguated from the block→9 ingots recipe with an explicit
        // "_from_nuggets" suffix.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ingot).pattern("###").pattern("###").pattern("###").define('#', nugget).unlockedBy("has_nugget", has(nugget)).save(output,
                ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "ingot_" + metalId + "_from_nuggets"));
        // 1 ingot → 9 nuggets. Auto-id is the nugget's path (nugget_<metal>) — no other
        // recipe targets the nugget, so no collision.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, nugget, 9).requires(ingot).unlockedBy("has_ingot", has(ingot)).save(output);
    }
}
