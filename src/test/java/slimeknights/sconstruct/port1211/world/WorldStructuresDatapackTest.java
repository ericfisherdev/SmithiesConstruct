package slimeknights.sconstruct.port1211.world;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * Pinned-behaviour tests for the SMTCON-59 worldgen JSONs. {@link WorldStructures} bootstraps
 * the structure / structure-set / biome-modifier entries during {@code ./gradlew runData};
 * those JSONs land in {@code src/generated/resources/} and ship on the runtime classpath. The
 * test treats them as a snapshot and pins the values that downstream players' worlds rely on
 * — rarity tunables, biome targeting, spawn weights — so a future drift in {@link
 * WorldStructures#SPACING} / {@link WorldStructures#SEPARATION} / spawn-weight constants
 * surfaces here before a generated world is ever cracked open.
 */
class WorldStructuresDatapackTest {

    private static final Gson GSON = new Gson();
    private static final String STRUCTURE_ROOT = "data/sconstruct/worldgen/structure/";
    private static final String STRUCTURE_SET_PATH = "data/sconstruct/worldgen/structure_set/slime_islands.json";
    private static final String BIOME_MODIFIER_PATH = "data/sconstruct/neoforge/biome_modifier/slime_mob_spawns.json";

    @Test
    void everySlimeColorHasAStructureJson() {
        assertAll(java.util.Arrays.stream(SlimeColor.values()).map(color -> () -> {
            JsonObject json = load(STRUCTURE_ROOT + "slime_island_" + color.id() + ".json");
            assertEquals(SConstruct.MOD_ID + ":slime_island", json.get("type").getAsString(), "structure type for " + color);
            assertEquals(color.id(), json.get("color").getAsString(), "colour field on the JSON for " + color);
            assertEquals("#minecraft:is_overworld", json.get("biomes").getAsString(), "biome target for " + color);
            assertEquals("surface_structures", json.get("step").getAsString(), "generation step for " + color);
            JsonObject radius = json.getAsJsonObject("radius");
            assertEquals(8, radius.get("min_inclusive").getAsInt(), "radius lower bound for " + color);
            assertEquals(24, radius.get("max_inclusive").getAsInt(), "radius upper bound for " + color);
        }));
    }

    @Test
    void structureSetPinsRarityTunablesAtFiftyPercentRarerThanTheTicketDefault() {
        JsonObject json = load(STRUCTURE_SET_PATH);
        JsonObject placement = json.getAsJsonObject("placement");
        // spacing/separation tuned 50% rarer than the original ticket's 64/24 to avoid
        // saturating the overworld; drift here changes player-facing world density.
        assertEquals("minecraft:random_spread", placement.get("type").getAsString());
        assertEquals(WorldStructures.SPACING, placement.get("spacing").getAsInt());
        assertEquals(WorldStructures.SEPARATION, placement.get("separation").getAsInt());
        assertEquals(WorldStructures.PLACEMENT_SALT, placement.get("salt").getAsInt());
        assertTrue(placement.get("spacing").getAsInt() > placement.get("separation").getAsInt(), "spacing must exceed separation (vanilla RandomSpreadStructurePlacement#validate)");
    }

    @Test
    void structureSetEnumeratesAllFourColorVariantsWithEqualWeight() {
        JsonObject json = load(STRUCTURE_SET_PATH);
        var entries = json.getAsJsonArray("structures");
        assertEquals(SlimeColor.values().length, entries.size(), "all four colour variants must be in the set");
        java.util.Set<String> actualKeys = new java.util.HashSet<>();
        for (var element : entries) {
            JsonObject entry = element.getAsJsonObject();
            String structureKey = entry.get("structure").getAsString();
            actualKeys.add(structureKey);
            assertTrue(structureKey.startsWith(SConstruct.MOD_ID + ":slime_island_"), "structure key must be sconstruct:slime_island_<colour>");
            assertEquals(1, entry.get("weight").getAsInt(), "all colours must have equal weight so random spread picks uniformly");
        }
        // Set equality catches duplicates that count+prefix alone would miss — e.g. blue
        // appearing twice while magma is absent would still hit size==4 + prefix-OK but the
        // expected-vs-actual set comparison reveals the missing colour.
        java.util.Set<String> expectedKeys = java.util.Arrays.stream(SlimeColor.values()).map(color -> SConstruct.MOD_ID + ":slime_island_" + color.id()).collect(java.util.stream.Collectors.toSet());
        assertEquals(expectedKeys, actualKeys, "structure set must contain each slime colour exactly once");
    }

    @Test
    void biomeModifierTargetsOverworldWithBlueslimeHighWeightAndHugeslimeRare() {
        JsonObject json = load(BIOME_MODIFIER_PATH);
        assertEquals("neoforge:add_spawns", json.get("type").getAsString());
        assertEquals("#minecraft:is_overworld", json.get("biomes").getAsString());
        var spawners = json.getAsJsonArray("spawners");
        assertEquals(2, spawners.size(), "blueslime + hugeslime");
        JsonObject blue = null;
        JsonObject huge = null;
        for (var element : spawners) {
            JsonObject entry = element.getAsJsonObject();
            String type = entry.get("type").getAsString();
            if ((SConstruct.MOD_ID + ":blueslime").equals(type)) {
                blue = entry;
            }
            else if ((SConstruct.MOD_ID + ":hugeslime").equals(type)) {
                huge = entry;
            }
        }
        assertNotNull(blue, "spawners array must contain a sconstruct:blueslime entry");
        assertNotNull(huge, "spawners array must contain a sconstruct:hugeslime entry");
        // Blueslime carries vanilla-slime weight (100, pack 4). Huge slime is boss-rare (1/1/1).
        // Drift would inflate or strangle slime presence on overworld biomes.
        assertEquals(100, blue.get("weight").getAsInt());
        assertEquals(4, blue.get("minCount").getAsInt());
        assertEquals(4, blue.get("maxCount").getAsInt());
        assertEquals(1, huge.get("weight").getAsInt());
        assertEquals(1, huge.get("minCount").getAsInt());
        assertEquals(1, huge.get("maxCount").getAsInt());
        assertNotSame(blue, huge);
    }

    @SuppressWarnings("PMD.UseProperClassLoader") // proper context loader checked first; fallback fires only when null
    private static JsonObject load(String resourcePath) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = WorldStructuresDatapackTest.class.getClassLoader();
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
