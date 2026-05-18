package slimeknights.sconstruct.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import slimeknights.sconstruct.smeltery.MoltenMetal;
import slimeknights.sconstruct.smeltery.MoltenMetals;
import slimeknights.sconstruct.world.block.SlimeColor;

/**
 * Pinned-behaviour tests for the generated crafting-table recipe JSONs. Validates the four
 * recipe shapes emitted by {@link TinkerRecipeProvider} per metal — block↔ingot and
 * ingot↔nugget — by snapshot-loading representative files from the test classpath. Coverage
 * counts pin the totals so a future refactor that drops a metal or duplicates a recipe id
 * trips the test before CI.
 */
class TinkerRecipeProviderTest {

    private static final Gson GSON = new Gson();
    private static final String RECIPE_ROOT = "data/sconstruct/recipe/";

    @Test
    void blockFromIngotsIsAShaped3x3GroupedAsBuildingBlocks() {
        JsonObject recipe = loadRecipe("block_cobalt.json");
        assertAll(() -> assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString()), () -> assertEquals("building", recipe.get("category").getAsString()),
                () -> assertEquals(3, recipe.getAsJsonArray("pattern").size(), "shaped recipe must declare 3 rows"),
                () -> assertEquals("###", recipe.getAsJsonArray("pattern").get(0).getAsString(), "row 0 must fill the 3×3 grid"),
                () -> assertEquals("###", recipe.getAsJsonArray("pattern").get(1).getAsString(), "row 1 must fill the 3×3 grid"),
                () -> assertEquals("###", recipe.getAsJsonArray("pattern").get(2).getAsString(), "row 2 must fill the 3×3 grid"),
                () -> assertEquals("sconstruct:ingot_cobalt", recipe.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString()),
                () -> assertEquals("sconstruct:block_cobalt", recipe.getAsJsonObject("result").get("id").getAsString()),
                () -> assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt()));
    }

    @Test
    void ingotFromBlockIsAShapelessYieldingNine() {
        JsonObject recipe = loadRecipe("ingot_steel_from_block.json");
        assertAll(() -> assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString()),
                () -> assertEquals(1, recipe.getAsJsonArray("ingredients").size(), "shapeless block→ingot recipe takes exactly one input"),
                () -> assertEquals("sconstruct:block_steel", recipe.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString()),
                () -> assertEquals("sconstruct:ingot_steel", recipe.getAsJsonObject("result").get("id").getAsString()),
                () -> assertEquals(9, recipe.getAsJsonObject("result").get("count").getAsInt()));
    }

    @Test
    void ingotFromNuggetsIsAShaped3x3OfNuggets() {
        JsonObject recipe = loadRecipe("ingot_lead_from_nuggets.json");
        assertAll(() -> assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString()),
                () -> assertEquals(3, recipe.getAsJsonArray("pattern").size(), "shaped recipe must declare 3 rows"),
                () -> assertEquals("###", recipe.getAsJsonArray("pattern").get(0).getAsString(), "row 0 must fill the 3×3 grid"),
                () -> assertEquals("###", recipe.getAsJsonArray("pattern").get(1).getAsString(), "row 1 must fill the 3×3 grid"),
                () -> assertEquals("###", recipe.getAsJsonArray("pattern").get(2).getAsString(), "row 2 must fill the 3×3 grid"),
                () -> assertEquals("sconstruct:nugget_lead", recipe.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString()),
                () -> assertEquals("sconstruct:ingot_lead", recipe.getAsJsonObject("result").get("id").getAsString()));
    }

    @Test
    void nineNuggetsFromOneIngotIsAShapeless() {
        JsonObject recipe = loadRecipe("nugget_lead.json");
        assertAll(() -> assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString()),
                () -> assertEquals(1, recipe.getAsJsonArray("ingredients").size(), "shapeless ingot→nuggets recipe takes exactly one input"),
                () -> assertEquals("sconstruct:ingot_lead", recipe.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString()),
                () -> assertEquals("sconstruct:nugget_lead", recipe.getAsJsonObject("result").get("id").getAsString()),
                () -> assertEquals(9, recipe.getAsJsonObject("result").get("count").getAsInt()));
    }

    @Test
    void leadAndNickelLackBlockRecipesButHaveNuggetRecipes() {
        // The two skipped-from-blocks metals must not have block↔ingot recipes (their block
        // doesn't exist), but they DO get ingot↔nugget recipes.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        assertAll(() -> assertNull(cl.getResource(RECIPE_ROOT + "block_lead.json"), "lead has no block — must not emit block_<metal> recipe"),
                () -> assertNull(cl.getResource(RECIPE_ROOT + "ingot_lead_from_block.json"), "lead has no block — must not emit ingot_<metal>_from_block recipe"),
                () -> assertNull(cl.getResource(RECIPE_ROOT + "block_nickel.json")), () -> assertNull(cl.getResource(RECIPE_ROOT + "ingot_nickel_from_block.json")),
                () -> assertNotNull(cl.getResource(RECIPE_ROOT + "ingot_lead_from_nuggets.json")), () -> assertNotNull(cl.getResource(RECIPE_ROOT + "nugget_lead.json")),
                () -> assertNotNull(cl.getResource(RECIPE_ROOT + "ingot_nickel_from_nuggets.json")), () -> assertNotNull(cl.getResource(RECIPE_ROOT + "nugget_nickel.json")));
    }

    @Test
    void slimeBlockFromSlimeballsIsAShaped2x2GroupedAsBuildingBlocks() {
        // Pin one colour's shaped recipe — the rest are emitted from the same provider loop so
        // a one-colour check is sufficient to catch the shape/category drift, and the
        // everySlimeColorHasItsBlockSlimeballPair test below covers per-colour presence.
        JsonObject recipe = loadRecipe("slime_blue_block.json");
        assertAll(() -> assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString()), () -> assertEquals("building", recipe.get("category").getAsString()),
                () -> assertEquals(2, recipe.getAsJsonArray("pattern").size(), "slime block recipe must declare 2 rows"),
                () -> assertEquals("SS", recipe.getAsJsonArray("pattern").get(0).getAsString()), () -> assertEquals("SS", recipe.getAsJsonArray("pattern").get(1).getAsString()),
                () -> assertEquals("sconstruct:slimeball_blue", recipe.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString()),
                () -> assertEquals("sconstruct:slime_blue_block", recipe.getAsJsonObject("result").get("id").getAsString()),
                () -> assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt()));
    }

    @Test
    void slimeballsFromSlimeBlockIsAShapelessYieldingFour() {
        JsonObject recipe = loadRecipe("slimeball_magma_from_block.json");
        assertAll(() -> assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString()),
                () -> assertEquals(1, recipe.getAsJsonArray("ingredients").size(), "shapeless block→slimeballs recipe takes exactly one input"),
                () -> assertEquals("sconstruct:slime_magma_block", recipe.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString()),
                () -> assertEquals("sconstruct:slimeball_magma", recipe.getAsJsonObject("result").get("id").getAsString()),
                () -> assertEquals(4, recipe.getAsJsonObject("result").get("count").getAsInt()));
    }

    @Test
    void everySlimeColorHasItsBlockSlimeballPair() {
        // Per-colour resource-existence check. Mirrors everyMetalHasItsIngotNuggetPair: every
        // colour in SlimeColor lights up both the shaped (slimeballs→block) and shapeless
        // (block→slimeballs) recipe files. A future fifth colour would fail here first if its
        // entry in TinkerRecipeProvider#slimeballFor is missed.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (SlimeColor color : SlimeColor.values()) {
            String id = color.id();
            assertNotNull(cl.getResource(RECIPE_ROOT + "slime_" + id + "_block.json"), "slime_" + id + "_block.json missing");
            assertNotNull(cl.getResource(RECIPE_ROOT + "slimeball_" + id + "_from_block.json"), "slimeball_" + id + "_from_block.json missing");
        }
    }

    @Test
    void everyMetalHasItsIngotNuggetPair() {
        // Spot-check the per-metal recipe coverage: every metal in the canonical list has both
        // ingot↔nugget recipes regardless of block presence. This is a tighter guard than
        // counting alone — a future renamed metal id would fail the resource lookup directly.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String[] metalIds = { "cobalt", "ardite", "manyullyn", "knightslime", "pigiron", "silver", "tin", "zinc", "brass", "alubrass", "electrum", "steel", "lead", "nickel" };
        for (String id : metalIds) {
            assertNotNull(cl.getResource(RECIPE_ROOT + "ingot_" + id + "_from_nuggets.json"), "ingot_" + id + "_from_nuggets.json missing");
            assertNotNull(cl.getResource(RECIPE_ROOT + "nugget_" + id + ".json"), "nugget_" + id + ".json missing");
        }
    }

    @Test
    void meltingRecipesMeltTaggedInputsIntoTheMatchingMoltenFluid() {
        // SMTCON-127: an ingot melt yields 144 mB of the metal's molten fluid at its
        // temperature; an ore melt yields double — the smeltery ore bonus.
        JsonObject ingot = loadRecipe("melting_iron_ingot.json");
        assertAll(() -> assertEquals("sconstruct:melting", ingot.get("type").getAsString()), () -> assertEquals("c:ingots/iron", ingot.getAsJsonObject("input").get("tag").getAsString()),
                () -> assertEquals("sconstruct:molten_iron", ingot.getAsJsonObject("output").get("id").getAsString()),
                () -> assertEquals(144, ingot.getAsJsonObject("output").get("amount").getAsInt()), () -> assertEquals(1500, ingot.get("temperature").getAsInt()));

        JsonObject ore = loadRecipe("melting_iron_ore.json");
        assertEquals(288, ore.getAsJsonObject("output").get("amount").getAsInt(), "an ore melt yields twice the ingot volume");
    }

    @Test
    void everyMoltenMetalHasItsMeltingForms() {
        // Iterate the canonical metal list directly so this stays correct as metals are
        // added or removed — each emits ingot / block / nugget / ore melting recipes. Copper
        // is the exception: it is a vanilla material with no copper nugget, so it ships only
        // the ingot / block / ore forms.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (MoltenMetal metal : MoltenMetals.ALL) {
            boolean isCopper = "copper".equals(metal.id());
            for (String form : new String[] { "ingot", "block", "nugget", "ore" }) {
                String recipe = RECIPE_ROOT + "melting_" + metal.id() + "_" + form + ".json";
                if (isCopper && "nugget".equals(form)) {
                    assertNull(cl.getResource(recipe), "copper has no nugget — melting_copper_nugget must not be generated");
                }
                else {
                    assertNotNull(cl.getResource(recipe), "melting_" + metal.id() + "_" + form + ".json missing");
                }
            }
        }
    }

    @Test
    void castingRecipesPourMoltenMetalIntoTheMatchingItem() {
        // SMTCON-128: a table casting recipe pours 144 mB of molten metal into the metal's ingot.
        JsonObject ingot = loadRecipe("casting_cobalt_ingot.json");
        assertAll(() -> assertEquals("sconstruct:casting", ingot.get("type").getAsString()), () -> assertFalse(ingot.get("is_basin").getAsBoolean()),
                () -> assertEquals("sconstruct:molten_cobalt", ingot.getAsJsonObject("fluid").get("fluid").getAsString()),
                () -> assertEquals(144, ingot.getAsJsonObject("fluid").get("amount").getAsInt()),
                () -> assertEquals(40, ingot.get("cooling_time").getAsInt(), "an ingot cast cools in the table cooling time"),
                () -> assertEquals("sconstruct:ingot_cobalt", ingot.getAsJsonObject("output").get("id").getAsString()));

        JsonObject block = loadRecipe("casting_cobalt_block.json");
        assertAll(() -> assertTrue(block.get("is_basin").getAsBoolean(), "a block cast is a basin recipe"), () -> assertEquals(1296, block.getAsJsonObject("fluid").get("amount").getAsInt()));
    }

    @Test
    void alloyRecipesCombineMoltenInputsIntoANewMoltenMetal() {
        // SMTCON-128: brass alloys from three parts molten copper and one part molten zinc.
        JsonObject brass = loadRecipe("alloy_brass.json");
        JsonObject copper = brass.getAsJsonArray("inputs").get(0).getAsJsonObject();
        JsonObject zinc = brass.getAsJsonArray("inputs").get(1).getAsJsonObject();
        assertAll(() -> assertEquals("sconstruct:alloy", brass.get("type").getAsString()), () -> assertEquals(2, brass.getAsJsonArray("inputs").size(), "brass alloys from two inputs"),
                () -> assertEquals("sconstruct:molten_copper", copper.get("fluid").getAsString()), () -> assertEquals(432, copper.get("amount").getAsInt(), "three parts molten copper"),
                () -> assertEquals("sconstruct:molten_zinc", zinc.get("fluid").getAsString()), () -> assertEquals(144, zinc.get("amount").getAsInt(), "one part molten zinc"),
                () -> assertEquals("sconstruct:molten_brass", brass.getAsJsonObject("output").get("id").getAsString()),
                () -> assertEquals(576, brass.getAsJsonObject("output").get("amount").getAsInt()),
                () -> assertEquals(950, brass.get("temperature").getAsInt(), "brass alloys at the brass temperature"));
    }

    private static JsonObject loadRecipe(String fileName) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = cl.getResourceAsStream(RECIPE_ROOT + fileName)) {
            assertNotNull(stream, RECIPE_ROOT + fileName + " missing from test classpath — did you re-run ./gradlew runData?");
            return GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + fileName, e);
        }
    }
}
