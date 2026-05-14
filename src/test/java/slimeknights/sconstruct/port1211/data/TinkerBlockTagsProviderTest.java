package slimeknights.sconstruct.port1211.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Pinned-behaviour tests for the generated block-tag JSONs. {@link TinkerBlockTagsProvider}
 * writes JSON under {@code src/generated/resources/data/minecraft/tags/block/} during
 * {@code ./gradlew runData}; this test treats those files as a snapshot and asserts the
 * expected entries are present. The generated files are wired as a resource srcDir in
 * {@code build.gradle}, so they appear on the test classpath at {@code data/minecraft/tags/block/...}.
 *
 * <p>The intent is to keep the committed snapshot honest: if a future refactor adds a new
 * metal block and forgets to re-run {@code runData}, this test points at the drift before CI
 * does.
 */
class TinkerBlockTagsProviderTest {

    private static final Gson GSON = new Gson();

    @Test
    void mineableWithPickaxeIncludesEveryMetalBlock() {
        Set<String> expected = Set.of("sconstruct:block_cobalt", "sconstruct:block_ardite", "sconstruct:block_manyullyn", "sconstruct:block_knightslime", "sconstruct:block_pigiron",
                "sconstruct:block_silver", "sconstruct:block_copper", "sconstruct:block_tin", "sconstruct:block_zinc", "sconstruct:block_brass", "sconstruct:block_alubrass",
                "sconstruct:block_electrum", "sconstruct:block_steel");
        assertEquals(expected, loadValues("data/minecraft/tags/block/mineable/pickaxe.json"));
    }

    @Test
    void mineableWithAxeIncludesGlowFirewoodLavawood() {
        Set<String> values = loadValues("data/minecraft/tags/block/mineable/axe.json");
        assertEquals(Set.of("sconstruct:glow", "sconstruct:firewood", "sconstruct:lavawood"), values);
    }

    @Test
    void needsDiamondToolListsOnlyTheNetherTrio() {
        // Plan/03 line 142: cobalt, ardite, manyullyn are the only diamond-tier metals.
        Set<String> values = loadValues("data/minecraft/tags/block/needs_diamond_tool.json");
        assertEquals(Set.of("sconstruct:block_cobalt", "sconstruct:block_ardite", "sconstruct:block_manyullyn"), values);
    }

    @Test
    void needsIronToolListsEverythingExceptTheNetherTrio() {
        // Exact set: 13 total metal blocks - 3 diamond-tier = 10 iron-tier. Pinning the full
        // set also pins mutual exclusion with the diamond-tier set — a future refactor that
        // double-tagged would let cobalt also satisfy NEEDS_IRON_TOOL, fail this assertion,
        // and surface the wasted JSON entry before it shipped.
        Set<String> expected = Set.of("sconstruct:block_knightslime", "sconstruct:block_pigiron", "sconstruct:block_silver", "sconstruct:block_copper", "sconstruct:block_tin", "sconstruct:block_zinc",
                "sconstruct:block_brass", "sconstruct:block_alubrass", "sconstruct:block_electrum", "sconstruct:block_steel");
        assertEquals(expected, loadValues("data/minecraft/tags/block/needs_iron_tool.json"));
    }

    private static Set<String> loadValues(String classpathResource) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = cl.getResourceAsStream(classpathResource)) {
            assertNotNull(stream, classpathResource + " missing from test classpath — did you re-run ./gradlew runData?");
            JsonObject json = GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
            List<String> entries = new ArrayList<>();
            json.getAsJsonArray("values").forEach(element -> entries.add(element.getAsString()));
            return Set.copyOf(entries);
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + classpathResource, e);
        }
    }
}
