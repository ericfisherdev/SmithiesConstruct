package slimeknights.tconstruct.port1211.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import slimeknights.tconstruct.port1211.shared.SharedBlocks;

/**
 * Pinned-behaviour tests for the generated blockstate + cube-all model JSONs.
 * {@link TinkerBlockStateProvider} runs during {@code ./gradlew runData} and writes one pair
 * (blockstate + model) per shared-pulse block under
 * {@code src/generated/resources/assets/tconstruct/{blockstates,models/block}/}. The tests
 * treat the JSONs as a snapshot read from the test classpath.
 */
class TinkerBlockStateProviderTest {

    private static final Gson GSON = new Gson();
    private static final String BLOCKSTATE_ROOT = "assets/tconstruct/blockstates/";
    private static final String MODEL_ROOT = "assets/tconstruct/models/block/";

    @Test
    void everyMetalBlockstateHasASingleVariantPointingAtMatchingModel() {
        // Drive the structural assertion per-metal so a wrong variants[""] model on any one
        // metal trips here instead of slipping past the two-anchor previous check. A future
        // state property (e.g. a "facing" variant on glow) would have to update this loop
        // deliberately.
        assertAll(SharedBlocks.METAL_BLOCKS.keySet().stream().map(id -> () -> {
            String blockPath = "block_" + id;
            JsonObject blockstate = load(BLOCKSTATE_ROOT + blockPath + ".json");
            JsonObject variants = blockstate.getAsJsonObject("variants");
            assertEquals(1, variants.entrySet().size(), blockPath + ": cube_all blockstate has exactly one variant");
            assertEquals("tconstruct:block/" + blockPath, variants.getAsJsonObject("").get("model").getAsString(), blockPath + ": empty-selector variant model");
        }));
    }

    @Test
    void everyMetalModelIsCubeAllReferencingItsBlockTexture() {
        assertAll(SharedBlocks.METAL_BLOCKS.keySet().stream().map(id -> () -> {
            String blockPath = "block_" + id;
            JsonObject model = load(MODEL_ROOT + blockPath + ".json");
            assertEquals("minecraft:block/cube_all", model.get("parent").getAsString(), blockPath + ": parent");
            assertEquals("tconstruct:block/" + blockPath, model.getAsJsonObject("textures").get("all").getAsString(), blockPath + ": all-texture");
        }));
    }

    @Test
    void decorativeBlocksHaveBlockstateAndModelPairs() {
        // Pin parent + all-texture for every decorative so a regression on any of them
        // (wrong parent, missing texture key, drifted path) trips the assertion at the same
        // strength as the glow check.
        assertAll(() -> assertNotNull(load(BLOCKSTATE_ROOT + "glow.json")), () -> assertNotNull(load(MODEL_ROOT + "glow.json")),
                () -> assertEquals("minecraft:block/cube_all", load(MODEL_ROOT + "glow.json").get("parent").getAsString()),
                () -> assertEquals("tconstruct:block/glow", load(MODEL_ROOT + "glow.json").getAsJsonObject("textures").get("all").getAsString()),

                () -> assertNotNull(load(BLOCKSTATE_ROOT + "firewood.json")), () -> assertNotNull(load(MODEL_ROOT + "firewood.json")),
                () -> assertEquals("minecraft:block/cube_all", load(MODEL_ROOT + "firewood.json").get("parent").getAsString()),
                () -> assertEquals("tconstruct:block/firewood", load(MODEL_ROOT + "firewood.json").getAsJsonObject("textures").get("all").getAsString()),

                () -> assertNotNull(load(BLOCKSTATE_ROOT + "lavawood.json")), () -> assertNotNull(load(MODEL_ROOT + "lavawood.json")),
                () -> assertEquals("minecraft:block/cube_all", load(MODEL_ROOT + "lavawood.json").get("parent").getAsString()),
                () -> assertEquals("tconstruct:block/lavawood", load(MODEL_ROOT + "lavawood.json").getAsJsonObject("textures").get("all").getAsString()));
    }

    private static JsonObject load(String classpathResource) {
        // Gradle's test JVM always sets the thread context classloader, but fall back to the
        // test class's own loader if the harness ever runs without one — the alternative is
        // an NPE before our assertNotNull below can fire a useful message.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = TinkerBlockStateProviderTest.class.getClassLoader();
        }
        try (InputStream stream = cl.getResourceAsStream(classpathResource)) {
            assertNotNull(stream, classpathResource + " missing from test classpath — did you re-run ./gradlew runData?");
            return GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + classpathResource, e);
        }
    }
}
