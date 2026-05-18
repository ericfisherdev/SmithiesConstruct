package slimeknights.sconstruct.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import slimeknights.sconstruct.shared.SharedBlocks;

/**
 * Pinned-behaviour tests for the generated block loot tables. {@link SharedBlockLoot} runs
 * during {@code ./gradlew runData} and writes one JSON per shared-pulse block; each table
 * drops the block's own item with the standard {@code survives_explosion} condition (vanilla
 * default for {@code dropSelf}). The tests treat the JSONs as a snapshot read from the test
 * classpath.
 */
class SharedBlockLootTest {

    private static final Gson GSON = new Gson();
    private static final String LOOT_ROOT = "data/sconstruct/loot_table/blocks/";

    @Test
    void metalStorageBlocksDropSelf() {
        JsonObject loot = loadTable("block_cobalt.json");
        assertAll(() -> assertEquals("minecraft:block", loot.get("type").getAsString()),
                () -> assertEquals("sconstruct:block_cobalt", firstItemEntry(loot), "metal storage block should drop its own item"),
                () -> assertEquals("minecraft:survives_explosion", firstCondition(loot), "vanilla dropSelf wraps the pool in survives_explosion"));
    }

    @Test
    void decorativeBlocksDropSelf() {
        assertAll(() -> assertEquals("sconstruct:glow", firstItemEntry(loadTable("glow.json"))), () -> assertEquals("sconstruct:firewood", firstItemEntry(loadTable("firewood.json"))),
                () -> assertEquals("sconstruct:lavawood", firstItemEntry(loadTable("lavawood.json"))));
    }

    @Test
    void everyMetalStorageBlockHasItsLootTable() {
        // Drive the assertion from SharedBlocks.METAL_BLOCKS directly so the test stays in
        // sync when a metal is added or removed — no hardcoded id list to drift out of date.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String id : SharedBlocks.METAL_BLOCKS.keySet()) {
            assertNotNull(cl.getResource(LOOT_ROOT + "block_" + id + ".json"), "loot table for block_" + id + " missing — re-run ./gradlew runData?");
        }
    }

    @Test
    void leadAndNickelHaveNoLootTableBecauseTheyHaveNoBlock() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        assertAll(() -> assertNull(cl.getResource(LOOT_ROOT + "block_lead.json"), "lead has no storage block — must not emit a loot table"),
                () -> assertNull(cl.getResource(LOOT_ROOT + "block_nickel.json"), "nickel has no storage block — must not emit a loot table"));
    }

    private static String firstItemEntry(JsonObject loot) {
        return loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString();
    }

    private static String firstCondition(JsonObject loot) {
        return loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("conditions").get(0).getAsJsonObject().get("condition").getAsString();
    }

    private static JsonObject loadTable(String fileName) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = cl.getResourceAsStream(LOOT_ROOT + fileName)) {
            assertNotNull(stream, LOOT_ROOT + fileName + " missing from test classpath — did you re-run ./gradlew runData?");
            return GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + fileName, e);
        }
    }
}
