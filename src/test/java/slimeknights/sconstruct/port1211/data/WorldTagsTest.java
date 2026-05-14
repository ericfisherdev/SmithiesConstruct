package slimeknights.sconstruct.port1211.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * Pinned-behaviour tests for the SMTCON-61 tag JSONs. {@link TinkerBlockTagsProvider},
 * {@link TinkerItemTagsProvider}, {@link TinkerFluidTagsProvider},
 * {@link WorldBiomeTagsProvider}, and {@link WorldEntityTagsProvider} all emit their tag
 * JSONs during {@code ./gradlew runData}; the test treats them as a snapshot and pins the
 * member sets so a future drift surfaces here before a player hits a missing-tag warning.
 */
class WorldTagsTest {

    private static final Gson GSON = new Gson();

    @Test
    void slimegrassBlockTagListsAllFourColorVariants() {
        Set<String> actual = loadValues("data/sconstruct/tags/block/slimegrass.json");
        Set<String> expected = java.util.Arrays.stream(SlimeColor.values()).map(color -> SConstruct.MOD_ID + ":slime_" + color.id() + "_grass").collect(Collectors.toSet());
        assertEquals(expected, actual, "sconstruct:slimegrass block tag must enumerate every coloured grass exactly once");
    }

    @Test
    void slimelogsBlockTagListsBothNormalAndStrippedVariantsAcrossColors() {
        Set<String> actual = loadValues("data/sconstruct/tags/block/slimelogs.json");
        Set<String> expected = new HashSet<>();
        for (SlimeColor color : SlimeColor.values()) {
            expected.add(SConstruct.MOD_ID + ":slime_" + color.id() + "_log");
            expected.add(SConstruct.MOD_ID + ":stripped_slime_" + color.id() + "_log");
        }
        assertEquals(expected, actual, "sconstruct:slimelogs must contain every slime log variant (normal + stripped) exactly once");
    }

    @Test
    void itemTagsCopyBlockTagsForGrassAndLogs() {
        // The provider uses copy() to mirror the block tag's entries onto the item tag.
        Set<String> grass = loadValues("data/sconstruct/tags/item/slimegrass.json");
        Set<String> logs = loadValues("data/sconstruct/tags/item/slimelogs.json");
        Set<String> expectedGrass = java.util.Arrays.stream(SlimeColor.values()).map(color -> SConstruct.MOD_ID + ":slime_" + color.id() + "_grass").collect(Collectors.toSet());
        assertEquals(expectedGrass, grass, "item-side sconstruct:slimegrass must match the block tag");
        Set<String> expectedLogs = new HashSet<>();
        for (SlimeColor color : SlimeColor.values()) {
            expectedLogs.add(SConstruct.MOD_ID + ":slime_" + color.id() + "_log");
            expectedLogs.add(SConstruct.MOD_ID + ":stripped_slime_" + color.id() + "_log");
        }
        assertEquals(expectedLogs, logs, "item-side sconstruct:slimelogs must match the block tag");
    }

    @Test
    void commonSlimeFluidTagListsSourceAndFlowingForEveryColor() {
        Set<String> actual = loadValues("data/c/tags/fluid/slime.json");
        Set<String> expected = new HashSet<>();
        for (SlimeColor color : SlimeColor.values()) {
            expected.add(SConstruct.MOD_ID + ":slime_" + color.id());
            expected.add(SConstruct.MOD_ID + ":flowing_slime_" + color.id());
        }
        assertEquals(expected, actual, "c:slime fluid tag must contain source + flowing variants for every colour");
    }

    @Test
    void slimesEntityTagListsBlueslimeAndHugeslime() {
        Set<String> actual = loadValues("data/sconstruct/tags/entity_type/slimes.json");
        Set<String> expected = Set.of(SConstruct.MOD_ID + ":blueslime", SConstruct.MOD_ID + ":hugeslime");
        assertEquals(expected, actual, "sconstruct:slimes entity tag must contain both slime mob types");
    }

    @Test
    void slimeIslandsBiomeTagAliasesIsOverworld() {
        // The biome tag delegates to the vanilla IS_OVERWORLD tag via addTag() — placeholder
        // wiring until a curated biome list lands (open question 12 in the SMTCON-58/59 plan).
        // Pinning the tag-reference shape here makes the eventual migration to an explicit
        // biome list surface as a deliberate test edit rather than a silent change.
        Set<String> actual = loadValues("data/sconstruct/tags/worldgen/biome/slime_islands.json");
        assertEquals(Set.of("#minecraft:is_overworld"), actual, "sconstruct:slime_islands must currently alias #minecraft:is_overworld");
    }

    @SuppressWarnings("PMD.UseProperClassLoader") // proper context loader checked first; fallback fires only when null
    private static Set<String> loadValues(String resourcePath) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = WorldTagsTest.class.getClassLoader();
        }
        try (InputStream stream = cl.getResourceAsStream(resourcePath)) {
            assertNotNull(stream, resourcePath + " missing from classpath — did you re-run ./gradlew runData?");
            JsonObject json = GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
            JsonArray values = json.getAsJsonArray("values");
            // A tag JSON without a "values" array would otherwise NPE inside forEach with a
            // stack trace that points at the helper rather than the offending file. The
            // diagnostic message names the path so a future drift in the provider output
            // (or a hand-edited override) lands on the right file at the first re-run.
            assertNotNull(values, resourcePath + " is missing the 'values' array — provider output drift?");
            Set<String> result = new HashSet<>();
            values.forEach(element -> result.add(element.getAsString()));
            // Duplicate JSON entries would silently dedupe into the Set and pass the
            // per-tag equality checks above. Comparing source-array size vs. deduped-set
            // size catches the drift instead of letting a "blue logged twice" slip through.
            assertEquals(values.size(), result.size(), resourcePath + " contains duplicate entries in the 'values' array");
            return result;
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + resourcePath, e);
        }
    }
}
