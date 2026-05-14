package slimeknights.sconstruct.port1211.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import slimeknights.sconstruct.port1211.shared.SharedBlocks;
import slimeknights.sconstruct.port1211.shared.SharedMetals;

/**
 * Pinned-behaviour tests for the generated item-model JSONs. {@link TinkerItemModelProvider}
 * runs during {@code ./gradlew runData} and writes one JSON per registered item under
 * {@code src/generated/resources/assets/sconstruct/models/item/}. Two shapes coexist:
 * sprite items inherit {@code minecraft:item/generated} with a {@code layer0} texture
 * reference; BlockItems inherit the matching block model.
 */
class TinkerItemModelProviderTest {

    private static final Gson GSON = new Gson();
    private static final String ITEM_MODEL_ROOT = "assets/sconstruct/models/item/";

    @Test
    void spriteIngotsAndNuggetsAreParentGeneratedWithMatchingLayer0() {
        // Sprite items follow vanilla's flat-icon convention: parent generated + single
        // layer0 texture. A future refactor that flipped the parent (e.g. to a 3D model)
        // would break the inventory render of every metal item.
        assertAll(SharedMetals.ALL.stream().map(metal -> () -> {
            JsonObject ingot = load("ingot_" + metal.id() + ".json");
            JsonObject nugget = load("nugget_" + metal.id() + ".json");
            assertEquals("minecraft:item/generated", ingot.get("parent").getAsString(), "ingot_" + metal.id() + " parent");
            assertEquals("sconstruct:item/ingot_" + metal.id(), ingot.getAsJsonObject("textures").get("layer0").getAsString(), "ingot_" + metal.id() + " layer0");
            assertEquals("minecraft:item/generated", nugget.get("parent").getAsString(), "nugget_" + metal.id() + " parent");
            assertEquals("sconstruct:item/nugget_" + metal.id(), nugget.getAsJsonObject("textures").get("layer0").getAsString(), "nugget_" + metal.id() + " layer0");
        }));
    }

    @Test
    void slimeballsAndMiscItemsHaveParentGenerated() {
        assertAll(() -> assertSpriteShape("slimeball_blue.json", "sconstruct:item/slimeball_blue"), () -> assertSpriteShape("slimeball_purple.json", "sconstruct:item/slimeball_purple"),
                () -> assertSpriteShape("slimeball_blood.json", "sconstruct:item/slimeball_blood"), () -> assertSpriteShape("slimeball_magma.json", "sconstruct:item/slimeball_magma"),
                () -> assertSpriteShape("bacon.json", "sconstruct:item/bacon"), () -> assertSpriteShape("mudbrick.json", "sconstruct:item/mudbrick"),
                () -> assertSpriteShape("blood_bucket.json", "sconstruct:item/blood_bucket"));
    }

    @Test
    void blockItemsInheritFromTheirBlockModel() {
        // BlockItems' item model should parent the block model so inventory renders 3D. A
        // refactor that flipped them to the generated parent would render the BlockItem as
        // a flat icon — surprising at best, broken-looking for cube blocks.
        assertAll(SharedBlocks.METAL_BLOCKS.keySet().stream().map(id -> () -> {
            String blockPath = "block_" + id;
            JsonObject model = load(blockPath + ".json");
            assertEquals("sconstruct:block/" + blockPath, model.get("parent").getAsString(), blockPath + ": parent should be the block model");
        }));
    }

    @Test
    void decorativeBlockItemsInheritFromTheirBlockModel() {
        assertAll(() -> assertEquals("sconstruct:block/glow", load("glow.json").get("parent").getAsString()),
                () -> assertEquals("sconstruct:block/firewood", load("firewood.json").get("parent").getAsString()),
                () -> assertEquals("sconstruct:block/lavawood", load("lavawood.json").get("parent").getAsString()));
    }

    private static void assertSpriteShape(String fileName, String expectedTexture) {
        JsonObject model = load(fileName);
        assertEquals("minecraft:item/generated", model.get("parent").getAsString(), fileName + " parent");
        assertEquals(expectedTexture, model.getAsJsonObject("textures").get("layer0").getAsString(), fileName + " layer0");
    }

    @SuppressWarnings("PMD.UseProperClassLoader") // proper context loader checked first; fallback fires only when null
    private static JsonObject load(String fileName) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = TinkerItemModelProviderTest.class.getClassLoader();
        }
        try (InputStream stream = cl.getResourceAsStream(ITEM_MODEL_ROOT + fileName)) {
            assertNotNull(stream, ITEM_MODEL_ROOT + fileName + " missing from test classpath — did you re-run ./gradlew runData?");
            return GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + fileName, e);
        }
    }
}
