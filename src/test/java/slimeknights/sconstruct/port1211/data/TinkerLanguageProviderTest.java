package slimeknights.sconstruct.port1211.data;

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

import slimeknights.sconstruct.port1211.shared.SharedBlocks;
import slimeknights.sconstruct.port1211.shared.SharedMetals;

/**
 * Pinned-behaviour tests for the generated {@code en_us.json}. {@link TinkerLanguageProvider}
 * runs during {@code ./gradlew runData} and writes one file at
 * {@code src/generated/resources/assets/sconstruct/lang/en_us.json}. The tests treat the
 * file as a snapshot read from the test classpath and check key presence + display values
 * for the canonical Phase-2 surfaces.
 */
class TinkerLanguageProviderTest {

    private static final Gson GSON = new Gson();
    private static final String LANG_FILE = "assets/sconstruct/lang/en_us.json";

    @Test
    void creativeTabTitleIsSmithiesConstruct() {
        // The lang key here must match the Component.translatable("itemGroup.sconstruct")
        // wired by SharedTabs.GENERAL — a drifted key would render the tab as a raw key
        // string in-game.
        assertEquals("Smithies' Construct", lang().get("itemGroup.sconstruct").getAsString());
    }

    @Test
    void everyMetalHasIngotAndNuggetEntries() {
        JsonObject lang = lang();
        assertAll(SharedMetals.ALL.stream().map(metal -> () -> {
            String ingotKey = "item.sconstruct.ingot_" + metal.id();
            String nuggetKey = "item.sconstruct.nugget_" + metal.id();
            assertTrue(lang.has(ingotKey), ingotKey + " missing");
            assertTrue(lang.has(nuggetKey), nuggetKey + " missing");
            assertTrue(lang.get(ingotKey).getAsString().endsWith(" Ingot"), ingotKey + " should end with ' Ingot'");
            assertTrue(lang.get(nuggetKey).getAsString().endsWith(" Nugget"), nuggetKey + " should end with ' Nugget'");
        }));
    }

    @Test
    void metalDisplayNameOverridesApplied() {
        // pigiron and alubrass have irregular display names that override the default
        // title-case derivation. A future refactor that drops the override map would render
        // them as "Pigiron Ingot" / "Alubrass Ingot" — pin the friendly forms.
        JsonObject lang = lang();
        assertAll(() -> assertEquals("Pig Iron Ingot", lang.get("item.sconstruct.ingot_pigiron").getAsString()),
                () -> assertEquals("Pig Iron Nugget", lang.get("item.sconstruct.nugget_pigiron").getAsString()),
                () -> assertEquals("Block of Pig Iron", lang.get("block.sconstruct.block_pigiron").getAsString()),
                () -> assertEquals("Aluminum Brass Ingot", lang.get("item.sconstruct.ingot_alubrass").getAsString()),
                () -> assertEquals("Aluminum Brass Nugget", lang.get("item.sconstruct.nugget_alubrass").getAsString()),
                () -> assertEquals("Block of Aluminum Brass", lang.get("block.sconstruct.block_alubrass").getAsString()));
    }

    @Test
    void metalBlockEntriesPresentOnlyForMetalsWithABlock() {
        // Lead and nickel have no storage block — they must NOT have a block.sconstruct.block_X
        // entry. Every other metal does.
        JsonObject lang = lang();
        for (String id : SharedBlocks.METAL_BLOCKS.keySet()) {
            String key = "block.sconstruct.block_" + id;
            assertTrue(lang.has(key), key + " missing");
            assertTrue(lang.get(key).getAsString().startsWith("Block of "), key + " should start with 'Block of '");
        }
        assertAll(() -> assertTrue(!lang.has("block.sconstruct.block_lead"), "lead has no block — no lang entry"),
                () -> assertTrue(!lang.has("block.sconstruct.block_nickel"), "nickel has no block — no lang entry"));
    }

    @Test
    void decorativeBlocksAndMiscItemsHaveExpectedDisplayNames() {
        JsonObject lang = lang();
        assertAll(() -> assertEquals("Glow", lang.get("block.sconstruct.glow").getAsString()), () -> assertEquals("Firewood", lang.get("block.sconstruct.firewood").getAsString()),
                () -> assertEquals("Lavawood", lang.get("block.sconstruct.lavawood").getAsString()), () -> assertEquals("Bacon", lang.get("item.sconstruct.bacon").getAsString()),
                () -> assertEquals("Mud Brick", lang.get("item.sconstruct.mudbrick").getAsString()), () -> assertEquals("Bucket of Blood", lang.get("item.sconstruct.blood_bucket").getAsString()),
                () -> assertEquals("Blood", lang.get("fluid.sconstruct.blood").getAsString()));
    }

    @Test
    void slimeballEntriesUseSlimeBallTwoWordCapitalisation() {
        // Vanilla writes "Slime Ball" (two words). The lang strings here should match so
        // sconstruct slimeballs read naturally next to vanilla ones in the recipe book.
        JsonObject lang = lang();
        assertAll(() -> assertEquals("Blue Slime Ball", lang.get("item.sconstruct.slimeball_blue").getAsString()),
                () -> assertEquals("Purple Slime Ball", lang.get("item.sconstruct.slimeball_purple").getAsString()),
                () -> assertEquals("Blood Slime Ball", lang.get("item.sconstruct.slimeball_blood").getAsString()),
                () -> assertEquals("Magma Slime Ball", lang.get("item.sconstruct.slimeball_magma").getAsString()));
    }

    @Test
    void totalEntryCountMatchesExpectedCoverage() {
        // 1 tab + 13 metal blocks + 3 decoratives + 15 ingots + 15 nuggets + 4 slimeballs
        // + bacon + mudbrick + blood bucket + blood fluid = 55.
        assertEquals(55, lang().entrySet().size());
    }

    @SuppressWarnings("PMD.UseProperClassLoader") // proper context loader checked first; fallback fires only when null
    private static JsonObject lang() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = TinkerLanguageProviderTest.class.getClassLoader();
        }
        try (InputStream stream = cl.getResourceAsStream(LANG_FILE)) {
            assertNotNull(stream, LANG_FILE + " missing from test classpath — did you re-run ./gradlew runData?");
            JsonObject parsed = GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
            assertNotNull(parsed, LANG_FILE + " is not valid JSON");
            return parsed;
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + LANG_FILE, e);
        }
    }
}
