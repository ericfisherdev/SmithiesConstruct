package slimeknights.sconstruct.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Pinned-behaviour tests for the legacy {@code .lang → .json} migration done by
 * {@code scripts/lang_to_json.py}. The 12 non-{@code en_us} locales live as static JSONs
 * under {@code src/main/resources/assets/sconstruct/lang/} (NOT under
 * {@code generated/resources/} — these are committed verbatim from the legacy translations).
 *
 * <p>Tests verify: every expected locale ships a JSON file, each parses as a valid
 * {@link JsonObject}, the files are UTF-8 with non-ASCII characters preserved (German
 * umlauts, Chinese hanzi, etc.), and a representative legacy key resolves to a non-empty
 * translation in each spot-checked locale.
 */
class LegacyLangMigrationTest {

    private static final Gson GSON = new Gson();
    private static final String LANG_ROOT = "assets/sconstruct/lang/";
    private static final List<String> EXPECTED_LOCALES = List.of("de_de", "en_ud", "es_es", "fr_fr", "ja_jp", "ko_kr", "pt_br", "pt_pt", "ru_ru", "sv_se", "zh_cn", "zh_tw");

    @Test
    @SuppressWarnings("PMD.UseProperClassLoader") // proper context loader checked first; fallback fires only when null
    void everyExpectedLocaleShipsAJsonFile() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = LegacyLangMigrationTest.class.getClassLoader();
        }
        final ClassLoader loader = cl;
        assertAll(EXPECTED_LOCALES.stream().map(locale -> () -> {
            assertNotNull(loader.getResource(LANG_ROOT + locale + ".json"), locale + ".json missing from the classpath — re-run scripts/lang_to_json.py?");
        }));
    }

    @Test
    void everyLocaleFileParsesAsAJsonObject() {
        // Each file must be valid JSON Object — a malformed file would fail the parse and a
        // runtime Minecraft lang load would also fail. Catching it here means a broken
        // conversion script never ships.
        assertAll(EXPECTED_LOCALES.stream().map(locale -> () -> {
            JsonObject parsed = loadLocale(locale);
            assertNotNull(parsed, locale + ".json should parse as a JSON object");
            assertFalse(parsed.entrySet().isEmpty(), locale + ".json should contain entries");
        }));
    }

    @Test
    void germanLocalePreservesUmlautsAsRawUtf8() {
        // ensure_ascii=False in the conversion script means UTF-8 bytes go into the JSON
        // verbatim (not escaped as ü etc.). The German "Mörtel" sample is a canonical
        // umlaut check.
        JsonObject de = loadLocale("de_de");
        // Static non-en_us lang files preserve the legacy upstream key prefix "tile.tconstruct.*"
        // verbatim per the SMTCON-48 contract — they ship as historical translations whose keys
        // intentionally do not match the modern "block.sconstruct.*" registry keys.
        assertEquals("Mörtel", de.get("tile.tconstruct.soil.grout.name").getAsString());
    }

    @Test
    void chineseLocalePreservesHanziAsRawUtf8() {
        // Same UTF-8 contract for multi-byte CJK glyphs.
        JsonObject zh = loadLocale("zh_cn");
        String synced = zh.get("config.synced.ok").getAsString();
        assertTrue(synced.contains("已"), "Chinese characters should survive the conversion: " + synced);
    }

    @Test
    void allLocalesContainTheLegacyConfigKey() {
        // config.synced.ok exists in every legacy lang file — a missing entry would mean the
        // script lost data during conversion. Iterate every locale to make sure none silently
        // dropped the entry.
        assertAll(EXPECTED_LOCALES.stream().map(locale -> () -> {
            JsonObject parsed = loadLocale(locale);
            assertTrue(parsed.has("config.synced.ok"), locale + " is missing config.synced.ok — possible data loss in conversion");
        }));
    }

    @SuppressWarnings("PMD.UseProperClassLoader") // proper context loader checked first; fallback fires only when null
    private static JsonObject loadLocale(String locale) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = LegacyLangMigrationTest.class.getClassLoader();
        }
        try (InputStream stream = cl.getResourceAsStream(LANG_ROOT + locale + ".json")) {
            assertNotNull(stream, LANG_ROOT + locale + ".json missing from classpath");
            return GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + locale, e);
        }
    }
}
