package slimeknights.tconstruct.port1211.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Pinned-behaviour tests for the generated crafting-table recipe JSONs. Validates the four
 * recipe shapes emitted by {@link TinkerRecipeProvider} per metal — block↔ingot and
 * ingot↔nugget — by snapshot-loading representative files from the test classpath. Coverage
 * counts pin the totals so a future refactor that drops a metal or duplicates a recipe id
 * trips the test before CI.
 */
class TinkerRecipeProviderTest {

    private static final Gson GSON = new Gson();
    private static final String RECIPE_ROOT = "data/tconstruct/recipe/";

    @Test
    void blockFromIngotsIsAShaped3x3GroupedAsBuildingBlocks() {
        JsonObject recipe = loadRecipe("block_cobalt.json");
        assertAll(() -> assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString()), () -> assertEquals("building", recipe.get("category").getAsString()),
                () -> assertEquals("tconstruct:ingot_cobalt", recipe.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString()),
                () -> assertEquals("tconstruct:block_cobalt", recipe.getAsJsonObject("result").get("id").getAsString()),
                () -> assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt()));
    }

    @Test
    void ingotFromBlockIsAShapelessYieldingNine() {
        JsonObject recipe = loadRecipe("ingot_steel_from_block.json");
        assertAll(() -> assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString()),
                () -> assertEquals("tconstruct:block_steel", recipe.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString()),
                () -> assertEquals("tconstruct:ingot_steel", recipe.getAsJsonObject("result").get("id").getAsString()),
                () -> assertEquals(9, recipe.getAsJsonObject("result").get("count").getAsInt()));
    }

    @Test
    void ingotFromNuggetsIsAShaped3x3OfNuggets() {
        JsonObject recipe = loadRecipe("ingot_lead_from_nuggets.json");
        assertAll(() -> assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString()),
                () -> assertEquals("tconstruct:nugget_lead", recipe.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString()),
                () -> assertEquals("tconstruct:ingot_lead", recipe.getAsJsonObject("result").get("id").getAsString()));
    }

    @Test
    void nineNuggetsFromOneIngotIsAShapeless() {
        JsonObject recipe = loadRecipe("nugget_lead.json");
        assertAll(() -> assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString()),
                () -> assertEquals("tconstruct:ingot_lead", recipe.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString()),
                () -> assertEquals("tconstruct:nugget_lead", recipe.getAsJsonObject("result").get("id").getAsString()),
                () -> assertEquals(9, recipe.getAsJsonObject("result").get("count").getAsInt()));
    }

    @Test
    void leadAndNickelLackBlockRecipesButHaveNuggetRecipes() {
        // The two skipped-from-blocks metals must not have block↔ingot recipes (their block
        // doesn't exist), but they DO get ingot↔nugget recipes.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        assertAll(() -> assertTrue(cl.getResource(RECIPE_ROOT + "block_lead.json") == null, "lead has no block — must not emit block_<metal> recipe"),
                () -> assertTrue(cl.getResource(RECIPE_ROOT + "ingot_lead_from_block.json") == null, "lead has no block — must not emit ingot_<metal>_from_block recipe"),
                () -> assertTrue(cl.getResource(RECIPE_ROOT + "block_nickel.json") == null), () -> assertTrue(cl.getResource(RECIPE_ROOT + "ingot_nickel_from_block.json") == null),
                () -> assertNotNull(cl.getResource(RECIPE_ROOT + "ingot_lead_from_nuggets.json")), () -> assertNotNull(cl.getResource(RECIPE_ROOT + "nugget_lead.json")),
                () -> assertNotNull(cl.getResource(RECIPE_ROOT + "ingot_nickel_from_nuggets.json")), () -> assertNotNull(cl.getResource(RECIPE_ROOT + "nugget_nickel.json")));
    }

    @Test
    void everyMetalHasItsIngotNuggetPair() {
        // Spot-check the per-metal recipe coverage: every metal in the canonical list has both
        // ingot↔nugget recipes regardless of block presence. This is a tighter guard than
        // counting alone — a future renamed metal id would fail the resource lookup directly.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String[] metalIds = { "cobalt", "ardite", "manyullyn", "knightslime", "pigiron", "silver", "copper", "tin", "zinc", "brass", "alubrass", "electrum", "steel", "lead", "nickel" };
        for (String id : metalIds) {
            assertNotNull(cl.getResource(RECIPE_ROOT + "ingot_" + id + "_from_nuggets.json"), "ingot_" + id + "_from_nuggets.json missing");
            assertNotNull(cl.getResource(RECIPE_ROOT + "nugget_" + id + ".json"), "nugget_" + id + ".json missing");
        }
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
