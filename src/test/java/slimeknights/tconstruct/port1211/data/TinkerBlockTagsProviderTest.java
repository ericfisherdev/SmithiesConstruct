package slimeknights.tconstruct.port1211.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Set<String> values = loadValues("data/minecraft/tags/block/mineable/pickaxe.json");
        assertEquals(13, values.size(), "all 13 metal storage blocks should be pickaxe-mineable");
        assertTrue(values.contains("tconstruct:block_cobalt"));
        assertTrue(values.contains("tconstruct:block_steel"));
        assertTrue(values.contains("tconstruct:block_alubrass"));
    }

    @Test
    void mineableWithAxeIncludesGlowFirewoodLavawood() {
        Set<String> values = loadValues("data/minecraft/tags/block/mineable/axe.json");
        assertEquals(Set.of("tconstruct:glow", "tconstruct:firewood", "tconstruct:lavawood"), values);
    }

    @Test
    void needsDiamondToolListsOnlyTheNetherTrio() {
        // Plan/03 line 142: cobalt, ardite, manyullyn are the only diamond-tier metals.
        Set<String> values = loadValues("data/minecraft/tags/block/needs_diamond_tool.json");
        assertEquals(Set.of("tconstruct:block_cobalt", "tconstruct:block_ardite", "tconstruct:block_manyullyn"), values);
    }

    @Test
    void needsIronToolListsEverythingExceptTheNetherTrio() {
        // 10 iron-tier blocks: 13 total metal blocks - 3 diamond-tier.
        Set<String> values = loadValues("data/minecraft/tags/block/needs_iron_tool.json");
        assertEquals(10, values.size());
        assertTrue(values.contains("tconstruct:block_steel"));
        assertTrue(values.contains("tconstruct:block_copper"));
        // Pin mutual exclusion with the diamond-tier set — a future refactor that double-tagged
        // would let cobalt also satisfy NEEDS_IRON_TOOL, which is technically harmless but
        // wastes a JSON entry and confuses tag-driven tooling code.
        for (String diamond : List.of("tconstruct:block_cobalt", "tconstruct:block_ardite", "tconstruct:block_manyullyn")) {
            assertTrue(!values.contains(diamond), diamond + " should not appear in needs_iron_tool");
        }
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
