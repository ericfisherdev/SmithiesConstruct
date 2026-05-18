package slimeknights.sconstruct.plugin.patchouli;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Structural smoke test for the {@code materialsandyou} Patchouli book (SMTCON-170). Walks every
 * category and entry JSON the book ships and verifies it is well-formed: entries carry a name,
 * a known category, and a non-empty {@code pages} array whose pages each declare a recognised
 * {@code type}.
 *
 * <p>The book is hand-authored content, so a typo in a page {@code type} or a dangling category
 * reference would not fail compilation — it would silently render as a broken or missing page
 * in-game. This test turns that into a build failure instead.
 *
 * <p>It also pins the SMTCON-162/163 contract that each custom {@code sconstruct:*} page type is
 * actually exercised: every one must appear at least {@link #MIN_CUSTOM_PAGE_INSTANCES} times
 * across the book.
 */
class PatchouliBookValidationTest {

    /**
     * Classpath location of the book content, resolved at runtime by {@link #bookRoot()}.
     * Patchouli 1.20+ enforces resource-pack books: the content lives under {@code assets/}
     * (only {@code book.json} stays in {@code data/}), inside the {@code en_us} language folder.
     */
    private static final String BOOK_RESOURCE = "assets/sconstruct/patchouli_books/materialsandyou/en_us";

    private static final Gson GSON = new Gson();

    /** The five custom page types registered by {@code SmithiesPatchouliPlugin} (SMTCON-162/163). */
    private static final Set<String> CUSTOM_PAGE_TYPES = Set.of("sconstruct:melting", "sconstruct:casting", "sconstruct:alloy", "sconstruct:modifier", "sconstruct:tool_stats");

    /** Minimum number of instances each custom page type must have across the book. */
    private static final int MIN_CUSTOM_PAGE_INSTANCES = 3;

    @Test
    void everyCategoryDeclaresAName() {
        List<Path> categories = jsonFilesIn(bookRoot().resolve("categories"));
        assertFalse(categories.isEmpty(), "the book must ship at least one category");
        assertAll(categories.stream().map(path -> () -> {
            JsonObject category = parse(path);
            assertTrue(isStringField(category, "name"), path + " must declare a string name");
        }));
    }

    @Test
    void everyEntryIsWellFormed() {
        Set<String> categoryIds = categoryIds();
        List<Path> entries = jsonFilesIn(bookRoot().resolve("entries"));
        assertFalse(entries.isEmpty(), "the book must ship at least one entry");
        assertAll(entries.stream().map(path -> () -> {
            JsonObject entry = parse(path);
            assertTrue(isStringField(entry, "name"), path + " must declare a string name");

            assertTrue(isStringField(entry, "category"), path + " must declare a string category");
            String category = entry.get("category").getAsString();
            assertTrue(categoryIds.contains(category), path + " references unknown category '" + category + "'");

            assertTrue(entry.has("pages") && entry.get("pages").isJsonArray(), path + " must declare a pages array");
            assertFalse(entry.getAsJsonArray("pages").isEmpty(), path + " must declare at least one page");
            for (JsonElement page : entry.getAsJsonArray("pages")) {
                assertTrue(page.isJsonObject(), path + " has a non-object page");
                JsonObject pageObject = page.getAsJsonObject();
                assertTrue(isStringField(pageObject, "type"), path + " has a page with no string type");
                String type = pageObject.get("type").getAsString();
                assertFalse(type.isBlank(), path + " has a page with a blank type");
                // An sconstruct: page type must be one of the registered custom types; a typo
                // there would render as a broken page rather than fail loudly.
                if (type.startsWith("sconstruct:")) {
                    assertTrue(CUSTOM_PAGE_TYPES.contains(type), path + " uses unknown custom page type '" + type + "'");
                }
                assertVisualPageWellFormed(path, type, pageObject);
            }
        }));
    }

    /**
     * Verify the built-in visual page types (SMTCON-210) carry the field Patchouli needs to
     * render them: a {@code multiblock} page its {@code multiblock} object, a {@code crafting}
     * page its {@code recipe} id, a {@code spotlight} page its {@code item}. A page missing
     * these would silently render blank in-game rather than fail the build.
     */
    private static void assertVisualPageWellFormed(Path path, String type, JsonObject page) {
        switch (type) {
        case "patchouli:multiblock" -> assertTrue(page.has("multiblock") && page.get("multiblock").isJsonObject(), path + " multiblock page must declare a multiblock object");
        case "patchouli:crafting" -> assertTrue(isStringField(page, "recipe"), path + " crafting page must declare a string recipe");
        case "patchouli:spotlight" -> assertTrue(page.has("item"), path + " spotlight page must declare an item");
        default -> {
            // not a visual page type — nothing extra to assert
        }
        }
    }

    @Test
    void eachCustomPageTypeHasAtLeastThreeInstances() {
        Map<String, Integer> counts = new HashMap<>();
        for (Path path : jsonFilesIn(bookRoot().resolve("entries"))) {
            JsonElement pages = parse(path).get("pages");
            // Skip a malformed entry rather than crashing here — everyEntryIsWellFormed() is the
            // test that reports the structural problem, so this count stays a clean signal.
            if (pages == null || !pages.isJsonArray()) {
                continue;
            }
            for (JsonElement page : pages.getAsJsonArray()) {
                if (!page.isJsonObject() || !isStringField(page.getAsJsonObject(), "type")) {
                    continue;
                }
                String type = page.getAsJsonObject().get("type").getAsString();
                if (CUSTOM_PAGE_TYPES.contains(type)) {
                    counts.merge(type, 1, Integer::sum);
                }
            }
        }
        assertAll(CUSTOM_PAGE_TYPES.stream().map(type -> () -> assertTrue(counts.getOrDefault(type, 0) >= MIN_CUSTOM_PAGE_INSTANCES,
                type + " must have at least " + MIN_CUSTOM_PAGE_INSTANCES + " instances across the book, found " + counts.getOrDefault(type, 0))));
    }

    /** Category ids the book defines — {@code sconstruct:<file-name>} for each categories/ file. */
    private static Set<String> categoryIds() {
        return jsonFilesIn(bookRoot().resolve("categories")).stream().map(path -> "sconstruct:" + stripJson(path)).collect(java.util.stream.Collectors.toSet());
    }

    /**
     * The book content root, resolved from the test classpath — Gradle explodes the mod's main
     * resources into a directory, so the resource URL is a {@code file:} path that walks fine.
     */
    private static Path bookRoot() {
        URL url = PatchouliBookValidationTest.class.getResource("/" + BOOK_RESOURCE);
        assertTrue(url != null, "book content '" + BOOK_RESOURCE + "' is not on the test classpath");
        try {
            return Path.of(url.toURI());
        }
        catch (URISyntaxException e) {
            throw new IllegalStateException("book resource URL is not a valid path: " + url, e);
        }
    }

    /** Every {@code .json} file beneath {@code dir}, sorted, as a list. */
    private static List<Path> jsonFilesIn(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
        catch (IOException e) {
            throw new UncheckedIOException("failed to walk book directory " + dir, e);
        }
    }

    private static String stripJson(Path path) {
        String name = path.getFileName().toString();
        return name.substring(0, name.length() - ".json".length());
    }

    private static JsonObject parse(Path path) {
        JsonElement parsed;
        try {
            parsed = GSON.fromJson(Files.readString(path), JsonElement.class);
        }
        catch (IOException e) {
            throw new UncheckedIOException("failed to read book file " + path, e);
        }
        assertTrue(parsed != null && parsed.isJsonObject(), path + " is not a JSON object");
        return parsed.getAsJsonObject();
    }

    /** Whether {@code object} has {@code key} set to a JSON string (not a number or boolean). */
    private static boolean isStringField(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isString();
    }
}
