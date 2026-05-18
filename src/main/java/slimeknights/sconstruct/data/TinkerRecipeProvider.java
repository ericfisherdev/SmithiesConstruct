package slimeknights.sconstruct.data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.gadgets.GadgetBlocks;
import slimeknights.sconstruct.gadgets.GadgetItems;
import slimeknights.sconstruct.shared.Metal;
import slimeknights.sconstruct.shared.SharedBlocks;
import slimeknights.sconstruct.shared.SharedFluids;
import slimeknights.sconstruct.shared.SharedItems;
import slimeknights.sconstruct.shared.SharedMetals;
import slimeknights.sconstruct.smeltery.MoltenMetal;
import slimeknights.sconstruct.smeltery.MoltenMetals;
import slimeknights.sconstruct.smeltery.SmelteryFluids;
import slimeknights.sconstruct.smeltery.recipe.FluidIngredient;
import slimeknights.sconstruct.tools.PartBuilderRegistry;
import slimeknights.sconstruct.tools.PatternChestRegistry;
import slimeknights.sconstruct.tools.StencilTableRegistry;
import slimeknights.sconstruct.tools.ToolStationRegistry;
import slimeknights.sconstruct.world.WorldBlocks;
import slimeknights.sconstruct.world.block.SlimeColor;

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

        // SMTCON-107: the blank-pattern crafting recipe. This is the single cheapest entry
        // point into the whole tool system — without it the stencil table, part builder, and
        // every part template are unreachable from a fresh world.
        addPatternRecipe(recipeOutput);

        // SMTCON-127: smeltery melting recipes — one per (ingot, block, nugget, ore) form of
        // every molten metal.
        addMeltingRecipes(recipeOutput);

        // SMTCON-128: casting recipes (molten metal → ingot / nugget / block item) and the
        // metal-alloying recipes.
        addCastingRecipes(recipeOutput);
        addAlloyRecipes(recipeOutput);

        // SMTCON-143: Phase-6 gadget crafting recipes (slimeslings, throwballs, piggyback, glow
        // ball, slime armor, the three gadget blocks) and the drying-rack drying recipes.
        addGadgetCraftingRecipes(recipeOutput);
        addDryingRecipes(recipeOutput);
    }

    /**
     * Emit the gadget crafting-table recipes (SMTCON-143). Each follows the legacy 1.12 shape as
     * closely as the available ingredients allow; the unlock criterion fires on the simplest
     * precursor item the recipe consumes.
     */
    private void addGadgetCraftingRecipes(RecipeOutput output) {
        // 4 slimeslings — leather + a matching-colour slimeball + string, in an L shape evoking
        // a sling: leather and string on the top row, the slimeball pouch below the string.
        // The colour-to-slimeball pairing matches the sling for each colour exactly.
        addSlingRecipe(output, "blue", SharedItems.SLIMEBALL_BLUE.get(), GadgetItems.SLING_BLUE.get());
        addSlingRecipe(output, "purple", SharedItems.SLIMEBALL_PURPLE.get(), GadgetItems.SLING_PURPLE.get());
        addSlingRecipe(output, "magma", SharedItems.SLIMEBALL_MAGMA.get(), GadgetItems.SLING_MAGMA.get());
        addSlingRecipe(output, "blood", SharedItems.SLIMEBALL_BLOOD.get(), GadgetItems.SLING_BLOOD.get());

        // 4 throwballs — 4 matching-colour slimeballs in a 2×2. Shaped 2×2 (not shapeless) so the
        // recipe reads as "pack four slimeballs into a ball" in the recipe book.
        addThrowballRecipe(output, SharedItems.SLIMEBALL_BLUE.get(), GadgetItems.THROWBALL_BLUE.get());
        addThrowballRecipe(output, SharedItems.SLIMEBALL_PURPLE.get(), GadgetItems.THROWBALL_PURPLE.get());
        addThrowballRecipe(output, SharedItems.SLIMEBALL_MAGMA.get(), GadgetItems.THROWBALL_MAGMA.get());
        addThrowballRecipe(output, SharedItems.SLIMEBALL_BLOOD.get(), GadgetItems.THROWBALL_BLOOD.get());

        // Piggyback — leather over a saddle (shapeless). The saddle is the "carry" component;
        // the leather pads it. Unlocks on the saddle, the rarer of the two ingredients.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, GadgetItems.PIGGYBACK.get()).requires(Items.LEATHER).requires(Items.SADDLE).unlockedBy("has_saddle", has(Items.SADDLE)).save(output);

        // Glow ball — 4 glowstone dust in a 2×2, mirroring vanilla's glowstone-block ratio at a
        // smaller scale. One ball per craft keeps the thrown light source a cheap consumable.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GadgetItems.GLOW_BALL.get()).pattern("GG").pattern("GG").define('G', Items.GLOWSTONE_DUST)
                .unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST)).save(output);

        // Slime armor — the four pieces use vanilla armor crafting shapes (helmet 5, chestplate
        // 8, leggings 7, boots 4). Congealed slime does not exist in this mod, so blue slimeballs
        // (SLIMEBALL_BLUE) stand in as the crafting material — the standard slime crafting unit.
        Item slime = SharedItems.SLIMEBALL_BLUE.get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, GadgetItems.SLIME_HELMET.get()).pattern("SSS").pattern("S S").define('S', slime).unlockedBy("has_slimeball", has(slime)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, GadgetItems.SLIME_CHESTPLATE.get()).pattern("S S").pattern("SSS").pattern("SSS").define('S', slime).unlockedBy("has_slimeball", has(slime))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, GadgetItems.SLIME_LEGGINGS.get()).pattern("SSS").pattern("S S").pattern("S S").define('S', slime).unlockedBy("has_slimeball", has(slime))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, GadgetItems.SLIME_BOOTS.get()).pattern("S S").pattern("S S").define('S', slime).unlockedBy("has_slimeball", has(slime)).save(output);

        // Drying rack — 4 sticks framing a single plank, in an H shape evoking the rack's slats.
        // Planks drawn from the vanilla ItemTags.PLANKS so any wood works.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GadgetBlocks.DRYING_RACK_ITEM.get()).pattern("SPS").pattern("SSS").define('S', Items.STICK).define('P', ItemTags.PLANKS)
                .unlockedBy("has_stick", has(Items.STICK)).save(output);

        // Wooden hopper — the vanilla hopper shape (V of planks around a centred chest), built
        // entirely from wood: a pre-iron-tier hopper, so no iron in the recipe.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, GadgetBlocks.WOODEN_HOPPER_ITEM.get()).pattern("P P").pattern("PCP").pattern(" P ").define('P', ItemTags.PLANKS).define('C', Items.CHEST)
                .unlockedBy("has_chest", has(Items.CHEST)).save(output);

        // Stone ladder — 7 stone slabs in the vanilla ladder shape, yielding 3 like a vanilla
        // ladder. Stone slabs make it the stone-tier sibling of the wooden ladder.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GadgetBlocks.STONE_LADDER_ITEM.get(), 3).pattern("S S").pattern("SSS").pattern("S S").define('S', Items.STONE_SLAB)
                .unlockedBy("has_stone_slab", has(Items.STONE_SLAB)).save(output);

        // Wither head — intentionally has no crafting recipe. It is a decorative item (SMTCON-140)
        // with no behaviour; the legacy 1.12 mod did not ship a recipe for it either, so it is
        // creative-tab / loot only.

        // Dried clay / dried clay brick — intentionally have no crafting recipe here. They are
        // decoration blocks produced by the drying mechanic in a future task (mud-brick → dried
        // clay drying chain); shipping a placeholder crafting recipe now would have to be removed
        // when that chain lands. They remain creative-tab obtainable.
    }

    /** Emit one slimesling crafting recipe — leather + string + a matching-colour slimeball. */
    private void addSlingRecipe(RecipeOutput output, String colorId, Item slimeball, Item sling) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, sling).pattern("LT").pattern(" B").define('L', Items.LEATHER).define('T', Items.STRING).define('B', slimeball)
                .unlockedBy("has_slimeball", has(slimeball)).save(output, ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "slimesling_" + colorId));
    }

    /** Emit one throwball crafting recipe — 4 matching-colour slimeballs in a 2×2. */
    private void addThrowballRecipe(RecipeOutput output, Item slimeball, Item throwball) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, throwball).pattern("SS").pattern("SS").define('S', slimeball).unlockedBy("has_slimeball", has(slimeball)).save(output);
    }

    /** Server ticks the two default drying recipes take — a slow, decorative-paced dry. */
    private static final int DRY_TIME_TICKS = 400;

    /**
     * Emit the two default drying-rack recipes (SMTCON-143). Both use only vanilla items so they
     * genuinely function the moment the rack is placed: a wet sponge dries into a sponge, and
     * kelp dries into dried kelp — satisfying SMTCON-137's deferred "default drying recipes work"
     * acceptance criterion.
     */
    private void addDryingRecipes(RecipeOutput output) {
        DryingRecipeBuilder.drying(Ingredient.of(Items.WET_SPONGE), new ItemStack(Items.SPONGE), DRY_TIME_TICKS).save(output,
                ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "drying_sponge"));
        DryingRecipeBuilder.drying(Ingredient.of(Items.KELP), new ItemStack(Items.DRIED_KELP), DRY_TIME_TICKS).save(output, ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "drying_kelp"));
    }

    /** Server ticks a cast item solidifies in over on a casting table. */
    private static final int TABLE_COOLING_TICKS = 40;
    /** Server ticks a nugget cast solidifies in — quicker, being a smaller pour. */
    private static final int NUGGET_COOLING_TICKS = 20;
    /** Server ticks a block cast solidifies in on a casting basin. */
    private static final int BASIN_COOLING_TICKS = 200;

    /** The molten metal driver entries, keyed by metal id, resolved once for the casting pass. */
    private Map<String, MoltenMetal> moltenByIdResolved;

    /** Lazily builds the {@code metal id → MoltenMetal} lookup the casting/alloy passes share. */
    private Map<String, MoltenMetal> moltenById() {
        if (moltenByIdResolved == null) {
            Map<String, MoltenMetal> map = new HashMap<>();
            for (MoltenMetal metal : MoltenMetals.ALL) {
                map.put(metal.id(), metal);
            }
            moltenByIdResolved = map;
        }
        return moltenByIdResolved;
    }

    /**
     * Emit casting recipes for every shared metal that has a molten fluid: pour 144&nbsp;mB onto
     * a table for an ingot, 16&nbsp;mB for a nugget, and 1296&nbsp;mB into a basin for a storage
     * block (only where the metal has a storage block). The cast-bearing tool-part casting
     * recipes are deferred until the cast (mould) items they need exist.
     */
    private void addCastingRecipes(RecipeOutput recipeOutput) {
        for (int i = 0; i < SharedMetals.ALL.size(); i++) {
            Metal metal = SharedMetals.ALL.get(i);
            MoltenMetal molten = moltenById().get(metal.id());
            if (molten == null) {
                // No molten fluid for this metal (e.g. electrum, nickel) — nothing to cast from.
                continue;
            }
            Fluid fluid = SmelteryFluids.get(molten).source().get();
            CastingRecipeBuilder.table(fluidIngredient(fluid, INGOT_MB), new ItemStack(SharedItems.INGOTS.get(i).get()), TABLE_COOLING_TICKS).save(recipeOutput,
                    smelteryId("casting_" + metal.id() + "_ingot"));
            CastingRecipeBuilder.table(fluidIngredient(fluid, NUGGET_MB), new ItemStack(SharedItems.NUGGETS.get(i).get()), NUGGET_COOLING_TICKS).save(recipeOutput,
                    smelteryId("casting_" + metal.id() + "_nugget"));
            DeferredBlock<Block> block = SharedBlocks.METAL_BLOCKS.get(metal.id());
            if (block != null) {
                CastingRecipeBuilder.basin(fluidIngredient(fluid, BLOCK_MB), new ItemStack(block.get()), BASIN_COOLING_TICKS).save(recipeOutput, smelteryId("casting_" + metal.id() + "_block"));
            }
        }
    }

    /**
     * Emit the metal-alloying recipes — the molten-fluid combinations the smeltery turns into a
     * new molten metal. Only alloys whose every input and output molten fluid is registered are
     * emitted; alubrass and electrum are deferred pending their missing fluids.
     */
    private void addAlloyRecipes(RecipeOutput recipeOutput) {
        Fluid copper = moltenFluid("copper");
        Fluid zinc = moltenFluid("zinc");
        Fluid tin = moltenFluid("tin");
        Fluid iron = moltenFluid("iron");
        Fluid emerald = moltenFluid("emerald");
        Fluid blood = SharedFluids.BLOOD.get();

        AlloyRecipeBuilder.alloy(new FluidStack(moltenFluid("brass"), INGOT_MB * 4), moltenMetal("brass").temperature()).input(copper, INGOT_MB * 3).input(zinc, INGOT_MB).save(recipeOutput,
                smelteryId("alloy_brass"));
        AlloyRecipeBuilder.alloy(new FluidStack(moltenFluid("bronze"), INGOT_MB * 4), moltenMetal("bronze").temperature()).input(copper, INGOT_MB * 3).input(tin, INGOT_MB).save(recipeOutput,
                smelteryId("alloy_bronze"));
        AlloyRecipeBuilder.alloy(new FluidStack(moltenFluid("manyullyn"), INGOT_MB * 2), moltenMetal("manyullyn").temperature()).input(moltenFluid("cobalt"), INGOT_MB)
                .input(moltenFluid("ardite"), INGOT_MB).save(recipeOutput, smelteryId("alloy_manyullyn"));
        AlloyRecipeBuilder.alloy(new FluidStack(moltenFluid("pigiron"), INGOT_MB), moltenMetal("pigiron").temperature()).input(iron, INGOT_MB).input(blood, INGOT_MB).input(emerald, INGOT_MB)
                .save(recipeOutput, smelteryId("alloy_pigiron"));
    }

    /**
     * The molten metal driver entry for the given id. Fails loudly during datagen if no such
     * molten metal exists, rather than letting a {@code null} surface as an opaque NPE deeper in
     * the recipe build.
     */
    private MoltenMetal moltenMetal(String metalId) {
        MoltenMetal metal = moltenById().get(metalId);
        if (metal == null) {
            throw new IllegalStateException("no molten metal registered for id '" + metalId + "' — alloy recipe datagen cannot proceed");
        }
        return metal;
    }

    /** The registered molten source fluid for the metal of the given id. */
    private Fluid moltenFluid(String metalId) {
        return SmelteryFluids.get(moltenMetal(metalId)).source().get();
    }

    /** A {@link FluidIngredient} requiring {@code amount} mB of exactly {@code fluid}. */
    private static FluidIngredient fluidIngredient(Fluid fluid, int amount) {
        return new FluidIngredient(HolderSet.direct(fluid.builtInRegistryHolder()), amount);
    }

    /** A recipe id under the mod namespace. */
    private static ResourceLocation smelteryId(String path) {
        return ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, path);
    }

    /** Millibuckets a single ingot melts into — the smeltery's base unit. */
    private static final int INGOT_MB = 144;
    /** Millibuckets a storage block melts into (nine ingots). */
    private static final int BLOCK_MB = INGOT_MB * 9;
    /** Millibuckets a nugget melts into (one-ninth of an ingot). */
    private static final int NUGGET_MB = INGOT_MB / 9;
    /** Millibuckets an ore melts into — twice the ingot yield, the smeltery's ore bonus. */
    private static final int ORE_MB = INGOT_MB * 2;
    /** Floor on a melt's duration so a nugget melt is not instantaneous. */
    private static final int MIN_MELT_TICKS = 20;
    /** Divisor turning an input's millibucket mass into its melt duration in ticks. */
    private static final int MELT_MB_PER_TICK = 2;

    /**
     * Emit a melting recipe for every form of every {@link MoltenMetals#ALL molten metal}: the
     * common {@code c:ingots}, {@code c:storage_blocks}, {@code c:nuggets}, and {@code c:ores}
     * tag of each metal melted into the matching amount of its molten fluid. An ore yields twice
     * the ingot volume. The melt temperature is the metal's own; the duration scales with the
     * input's millibucket mass so a block takes far longer than a nugget.
     */
    private void addMeltingRecipes(RecipeOutput recipeOutput) {
        for (MoltenMetal metal : MoltenMetals.ALL) {
            Fluid molten = SmelteryFluids.get(metal).source().get();
            int temperature = metal.temperature();
            meltingRecipe(recipeOutput, metal, "ingot", "ingots", molten, INGOT_MB, temperature);
            meltingRecipe(recipeOutput, metal, "block", "storage_blocks", molten, BLOCK_MB, temperature);
            meltingRecipe(recipeOutput, metal, "nugget", "nuggets", molten, NUGGET_MB, temperature);
            meltingRecipe(recipeOutput, metal, "ore", "ores", molten, ORE_MB, temperature);
        }
    }

    /** Emit one melting recipe consuming the {@code c:<tagGroup>/<metal>} tag. */
    private void meltingRecipe(RecipeOutput recipeOutput, MoltenMetal metal, String form, String tagGroup, Fluid molten, int amount, int temperature) {
        TagKey<Item> inputTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", tagGroup + "/" + metal.id()));
        int time = Math.max(MIN_MELT_TICKS, amount / MELT_MB_PER_TICK);
        MeltingRecipeBuilder.melting(Ingredient.of(inputTag), new FluidStack(molten, amount), temperature, time).save(recipeOutput,
                ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "melting_" + metal.id() + "_" + form));
    }

    /**
     * Emit the blank-pattern recipe: 4 sticks in a 2×2 → 4 blank patterns. The 1-stick-per-
     * pattern ratio is deliberately trivial — blank patterns are a bulk consumable (one is
     * spent per part template carved at the stencil table), so the recipe is priced to never
     * be a progression gate. BUILDING_BLOCKS would be wrong here; MISC keeps it in the
     * miscellaneous recipe-book tab alongside the other tool-pulse intermediates.
     */
    private void addPatternRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, StencilTableRegistry.BLANK_PATTERN.get(), 4).pattern("SS").pattern("SS").define('S', Items.STICK).unlockedBy("has_stick", has(Items.STICK))
                .save(recipeOutput);
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
