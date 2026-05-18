package slimeknights.sconstruct.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.world.block.SlimeColor;

/**
 * Pinned-behaviour tests for the Phase-3 world loot tables. {@link SlimeMobLoot} +
 * {@link SlimeIslandChestLoot} land their JSONs under {@code src/generated/resources/} during
 * {@code runData}; the test treats them as a snapshot and pins the drop quantities + table
 * shape so a future tuning drift surfaces here before a player ever cracks a chest open.
 */
class WorldLootTest {

    private static final Gson GSON = new Gson();
    private static final String BLUESLIME_LOOT = "data/sconstruct/loot_table/entities/blueslime.json";
    private static final String HUGESLIME_LOOT = "data/sconstruct/loot_table/entities/hugeslime.json";
    private static final String CHEST_LOOT = "data/sconstruct/loot_table/chests/slime_island.json";

    @Test
    void blueslimeDropsBlueSlimeballsInTheSpecRange() {
        JsonObject json = load(BLUESLIME_LOOT);
        JsonArray pools = json.getAsJsonArray("pools");
        assertEquals(1, pools.size(), "blueslime drops only the slimeball pool — no bone meal bonus pool like huge slime");
        JsonObject entry = pools.get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals(SConstruct.MOD_ID + ":slimeball_blue", entry.get("name").getAsString());
        // The setCount function pins UniformGenerator min/max. Drift in either bound would
        // silently change blueslime kill rewards across every save in the wild.
        JsonObject countFunction = entry.getAsJsonArray("functions").get(0).getAsJsonObject();
        assertEquals("minecraft:set_count", countFunction.get("function").getAsString());
        JsonObject countRange = countFunction.getAsJsonObject("count");
        assertEquals(1, countRange.get("min").getAsInt(), "blueslime min slimeballs");
        assertEquals(3, countRange.get("max").getAsInt(), "blueslime max slimeballs");
    }

    @Test
    void hugeSlimeDropsBlueSlimeballsAndBoneMealAcrossTwoPools() {
        JsonObject json = load(HUGESLIME_LOOT);
        JsonArray pools = json.getAsJsonArray("pools");
        assertEquals(2, pools.size(), "hugeslime drops slimeballs + bone meal in separate pools");

        // Pool 0: slimeballs 4-9
        JsonObject slimeballEntry = pools.get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals(SConstruct.MOD_ID + ":slimeball_blue", slimeballEntry.get("name").getAsString());
        JsonObject slimeballCount = slimeballEntry.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count");
        assertEquals(4, slimeballCount.get("min").getAsInt(), "hugeslime min slimeballs");
        assertEquals(9, slimeballCount.get("max").getAsInt(), "hugeslime max slimeballs");

        // Pool 1: bone meal 1-3
        JsonObject boneMealEntry = pools.get(1).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("minecraft:bone_meal", boneMealEntry.get("name").getAsString());
        JsonObject boneMealCount = boneMealEntry.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count");
        assertEquals(1, boneMealCount.get("min").getAsInt(), "hugeslime min bone meal");
        assertEquals(3, boneMealCount.get("max").getAsInt(), "hugeslime max bone meal");
    }

    @Test
    void islandChestLootHasFourPoolsCoveringSaplingsSlimeballsDyesAndFlavour() {
        JsonObject json = load(CHEST_LOOT);
        JsonArray pools = json.getAsJsonArray("pools");
        assertEquals(4, pools.size(), "chest has sapling + slimeball + dye + flavour pools");
    }

    @Test
    void islandChestSaplingPoolContainsOneEntryPerSlimeColor() {
        JsonObject json = load(CHEST_LOOT);
        JsonArray entries = json.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        assertEquals(SlimeColor.values().length, entries.size(), "one sapling entry per slime colour");
        java.util.Set<String> actualKeys = new java.util.HashSet<>();
        for (var element : entries) {
            actualKeys.add(element.getAsJsonObject().get("name").getAsString());
        }
        java.util.Set<String> expectedKeys = java.util.Arrays.stream(SlimeColor.values()).map(color -> SConstruct.MOD_ID + ":slime_" + color.id() + "_sapling")
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(expectedKeys, actualKeys, "sapling pool must contain every slime colour exactly once");
    }

    @Test
    void islandChestSlimeballPoolRolls2WithEveryColorAvailable() {
        JsonObject json = load(CHEST_LOOT);
        JsonObject slimeballPool = json.getAsJsonArray("pools").get(1).getAsJsonObject();
        // Constant-valued rolls serialize as a bare number (e.g. "rolls": 2.0), not as an
        // object with a "type": "constant" wrapper. Read the float value and compare.
        assertEquals(2.0d, slimeballPool.getAsJsonPrimitive("rolls").getAsDouble(), 1e-6d, "slimeball pool rolls twice per chest");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (var element : slimeballPool.getAsJsonArray("entries")) {
            seen.add(element.getAsJsonObject().get("name").getAsString());
        }
        java.util.Set<String> expected = java.util.Set.of(SConstruct.MOD_ID + ":slimeball_blue", SConstruct.MOD_ID + ":slimeball_purple", SConstruct.MOD_ID + ":slimeball_magma",
                SConstruct.MOD_ID + ":slimeball_blood");
        assertEquals(expected, seen, "slimeball pool must offer all four colours");
    }

    @Test
    void islandChestFlavourPoolHasAnEmptyEntryAlongsideModFlavourItems() {
        JsonObject json = load(CHEST_LOOT);
        JsonArray entries = json.getAsJsonArray("pools").get(3).getAsJsonObject().getAsJsonArray("entries");
        boolean hasEmpty = false;
        for (var element : entries) {
            JsonObject entry = element.getAsJsonObject();
            if (MINECRAFT_EMPTY_TYPE.equals(entry.get("type").getAsString())) {
                hasEmpty = true;
            }
        }
        assertTrue(hasEmpty, "flavour pool must include an empty entry so the slot is sometimes blank");
    }

    /** Vanilla loot-entry type emitted for an EmptyLootItem; extracted to a constant for the PMD AvoidLiteralsInIfCondition rule. */
    private static final String MINECRAFT_EMPTY_TYPE = "minecraft:empty";

    @SuppressWarnings("PMD.UseProperClassLoader") // proper context loader checked first; fallback fires only when null
    private static JsonObject load(String resourcePath) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = WorldLootTest.class.getClassLoader();
        }
        try (InputStream stream = cl.getResourceAsStream(resourcePath)) {
            assertNotNull(stream, resourcePath + " missing from classpath — did you re-run ./gradlew runData?");
            return GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + resourcePath, e);
        }
    }
}
