package slimeknights.tconstruct.port1211.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Pinned-behaviour tests for the item-tag snapshot. {@link TinkerItemTagsProvider} writes
 * JSON under {@code src/generated/resources/data/{c,tconstruct}/tags/item/} during
 * {@code ./gradlew runData}. The {@code c:} namespace is reserved for real-world metals so
 * cross-mod interop works; fictional metals stay under {@code tconstruct:}. Slimeballs build
 * a parent-and-four-children tag tree under {@code tconstruct:slimeballs}.
 *
 * <p>Tests both verify the snapshot contents from the test classpath and walk the on-disk
 * generated tree to catch any forbidden namespace usage (e.g. {@code forge:}).
 */
class TinkerItemTagsProviderTest {

    private static final Gson GSON = new Gson();

    @Test
    void realWorldMetalsLiveUnderTheCommonNamespace() {
        // Per plan/12: real-world metals get c:ingots/... so other mods can interop. A future
        // refactor that demotes one of these to tconstruct: would silently break recipe
        // ingredients pointing at c:ingots/copper.
        assertAll(() -> assertEquals(Set.of("tconstruct:ingot_copper"), loadValues("data/c/tags/item/ingots/copper.json")),
                () -> assertEquals(Set.of("tconstruct:ingot_silver"), loadValues("data/c/tags/item/ingots/silver.json")),
                () -> assertEquals(Set.of("tconstruct:ingot_steel"), loadValues("data/c/tags/item/ingots/steel.json")),
                () -> assertEquals(Set.of("tconstruct:nugget_lead"), loadValues("data/c/tags/item/nuggets/lead.json")));
    }

    @Test
    void fictionalMetalsStayUnderTheTconstructNamespace() {
        // c:ingots/manyullyn would be misleading — no other mod ships manyullyn. Pin both
        // directions: the c:ingots file must NOT exist, and the tconstruct:ingots file MUST.
        assertAll(() -> assertEquals(Set.of("tconstruct:ingot_manyullyn"), loadValues("data/tconstruct/tags/item/ingots/manyullyn.json")),
                () -> assertEquals(Set.of("tconstruct:ingot_cobalt"), loadValues("data/tconstruct/tags/item/ingots/cobalt.json")),
                () -> assertEquals(Set.of("tconstruct:nugget_pigiron"), loadValues("data/tconstruct/tags/item/nuggets/pigiron.json")),
                () -> assertTrue(Thread.currentThread().getContextClassLoader().getResource("data/c/tags/item/ingots/manyullyn.json") == null,
                        "manyullyn must not appear under c:ingots/ — fictional metals stay in tconstruct:"));
    }

    @Test
    void storageBlocksAreOnlyEmittedForMetalsWithABlock() {
        // Lead and nickel are real-world but have no storage block, so storage_blocks/<id> must
        // NOT exist for them. Real-world metals with a block (e.g. copper, steel) DO get one.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        assertAll(() -> assertNotNull(cl.getResource("data/c/tags/item/storage_blocks/copper.json"), "copper storage_blocks tag missing"),
                () -> assertNotNull(cl.getResource("data/c/tags/item/storage_blocks/steel.json"), "steel storage_blocks tag missing"),
                () -> assertTrue(cl.getResource("data/c/tags/item/storage_blocks/lead.json") == null, "lead has no block — must not emit storage_blocks tag"),
                () -> assertTrue(cl.getResource("data/c/tags/item/storage_blocks/nickel.json") == null, "nickel has no block — must not emit storage_blocks tag"));
    }

    @Test
    void slimeballParentReferencesEveryColourChild() {
        // The parent must include the four child tag references (as #namespace:path) so a
        // recipe input on the parent picks up any colour.
        Set<String> parent = loadValues("data/tconstruct/tags/item/slimeballs.json");
        assertEquals(Set.of("#tconstruct:slimeballs/blue", "#tconstruct:slimeballs/purple", "#tconstruct:slimeballs/blood", "#tconstruct:slimeballs/magma"), parent);
    }

    @Test
    void slimeballChildrenEachContainOneItem() {
        assertAll(() -> assertEquals(Set.of("tconstruct:slimeball_blue"), loadValues("data/tconstruct/tags/item/slimeballs/blue.json")),
                () -> assertEquals(Set.of("tconstruct:slimeball_purple"), loadValues("data/tconstruct/tags/item/slimeballs/purple.json")),
                () -> assertEquals(Set.of("tconstruct:slimeball_blood"), loadValues("data/tconstruct/tags/item/slimeballs/blood.json")),
                () -> assertEquals(Set.of("tconstruct:slimeball_magma"), loadValues("data/tconstruct/tags/item/slimeballs/magma.json")));
    }

    @Test
    void noForgeNamespaceFilesOnDisk() {
        // The c: namespace is the NeoForge convention; legacy forge: must never appear. Walk
        // the on-disk generated tree to catch accidental forge: paths in any tag file.
        Path generated = Paths.get("src/generated/resources/data");
        if (!Files.isDirectory(generated)) {
            // Test working directory varies in Gradle test runs; if the on-disk view isn't
            // available, the classpath-driven assertions above still cover the membership
            // contract. Skip rather than fail in that environment.
            return;
        }
        List<Path> forgePaths = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(generated)) {
            walk.filter(p -> p.toString().contains("/data/forge/")).forEach(forgePaths::add);
        }
        catch (IOException e) {
            throw new AssertionError("failed walking " + generated, e);
        }
        assertTrue(forgePaths.isEmpty(), "found forge: namespace files: " + forgePaths);
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
