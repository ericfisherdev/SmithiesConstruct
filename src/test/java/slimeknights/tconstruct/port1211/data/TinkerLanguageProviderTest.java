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

import slimeknights.tconstruct.port1211.shared.SharedBlocks;
import slimeknights.tconstruct.port1211.shared.SharedMetals;

/**
 * Pinned-behaviour tests for the generated {@code en_us.json}. {@link TinkerLanguageProvider}
 * runs during {@code ./gradlew runData} and writes one file at
 * {@code src/generated/resources/assets/tconstruct/lang/en_us.json}. The tests treat the
 * file as a snapshot read from the test classpath and check key presence + display values
 * for the canonical Phase-2 surfaces.
 */
class TinkerLanguageProviderTest {

    private static final Gson GSON = new Gson();
    private static final String LANG_FILE = "assets/tconstruct/lang/en_us.json";

    @Test
    void creativeTabTitleIsTinkersConstruct() {
        // The lang key here must match the Component.translatable("itemGroup.tconstruct")
        // wired by SharedTabs.GENERAL — a drifted key would render the tab as a raw key
        // string in-game.
        assertEquals("Tinkers' Construct", lang().get("itemGroup.tconstruct").getAsString());
    }

    @Test
    void everyMetalHasIngotAndNuggetEntries() {
        JsonObject lang = lang();
        assertAll(SharedMetals.ALL.stream().map(metal -> () -> {
            String ingotKey = "item.tconstruct.ingot_" + metal.id();
            String nuggetKey = "item.tconstruct.nugget_" + metal.id();
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
        assertAll(() -> assertEquals("Pig Iron Ingot", lang.get("item.tconstruct.ingot_pigiron").getAsString()),
                () -> assertEquals("Pig Iron Nugget", lang.get("item.tconstruct.nugget_pigiron").getAsString()),
                () -> assertEquals("Block of Pig Iron", lang.get("block.tconstruct.block_pigiron").getAsString()),
                () -> assertEquals("Aluminum Brass Ingot", lang.get("item.tconstruct.ingot_alubrass").getAsString()),
                () -> assertEquals("Aluminum Brass Nugget", lang.get("item.tconstruct.nugget_alubrass").getAsString()),
                () -> assertEquals("Block of Aluminum Brass", lang.get("block.tconstruct.block_alubrass").getAsString()));
    }

    @Test
    void metalBlockEntriesPresentOnlyForMetalsWithABlock() {
        // Lead and nickel have no storage block — they must NOT have a block.tconstruct.block_X
        // entry. Every other metal does.
        JsonObject lang = lang();
        for (String id : SharedBlocks.METAL_BLOCKS.keySet()) {
            String key = "block.tconstruct.block_" + id;
            assertTrue(lang.has(key), key + " missing");
            assertTrue(lang.get(key).getAsString().startsWith("Block of "), key + " should start with 'Block of '");
        }
        assertAll(() -> assertTrue(!lang.has("block.tconstruct.block_lead"), "lead has no block — no lang entry"),
                () -> assertTrue(!lang.has("block.tconstruct.block_nickel"), "nickel has no block — no lang entry"));
    }

    @Test
    void decorativeBlocksAndMiscItemsHaveExpectedDisplayNames() {
        JsonObject lang = lang();
        assertAll(() -> assertEquals("Glow", lang.get("block.tconstruct.glow").getAsString()), () -> assertEquals("Firewood", lang.get("block.tconstruct.firewood").getAsString()),
                () -> assertEquals("Lavawood", lang.get("block.tconstruct.lavawood").getAsString()), () -> assertEquals("Bacon", lang.get("item.tconstruct.bacon").getAsString()),
                () -> assertEquals("Mud Brick", lang.get("item.tconstruct.mudbrick").getAsString()), () -> assertEquals("Bucket of Blood", lang.get("item.tconstruct.blood_bucket").getAsString()),
                () -> assertEquals("Blood", lang.get("fluid.tconstruct.blood").getAsString()));
    }

    @Test
    void slimeballEntriesUseSlimeBallTwoWordCapitalisation() {
        // Vanilla writes "Slime Ball" (two words). The lang strings here should match so
        // tconstruct slimeballs read naturally next to vanilla ones in the recipe book.
        JsonObject lang = lang();
        assertAll(() -> assertEquals("Blue Slime Ball", lang.get("item.tconstruct.slimeball_blue").getAsString()),
                () -> assertEquals("Purple Slime Ball", lang.get("item.tconstruct.slimeball_purple").getAsString()),
                () -> assertEquals("Blood Slime Ball", lang.get("item.tconstruct.slimeball_blood").getAsString()),
                () -> assertEquals("Magma Slime Ball", lang.get("item.tconstruct.slimeball_magma").getAsString()));
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
            return GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + LANG_FILE, e);
        }
    }
}
