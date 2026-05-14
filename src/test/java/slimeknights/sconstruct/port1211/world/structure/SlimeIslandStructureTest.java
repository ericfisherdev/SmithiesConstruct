package slimeknights.sconstruct.port1211.world.structure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.world.WorldStructures;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * Pinned-behaviour tests for {@link SlimeIslandStructure}. The full {@code findGenerationPoint}
 * pipeline requires a live worldgen {@code GenerationContext} which only exists during chunk
 * generation; tests here exercise the codec round-trip and registry-id surface, plus the
 * {@link SlimeColor#CODEC} that the structure's {@code color} field reads.
 */
class SlimeIslandStructureTest {

    @Test
    void structureTypeRegistersUnderTheSconstructNamespace() {
        assertNotNull(WorldStructures.SLIME_ISLAND_TYPE);
        assertEquals(SConstruct.MOD_ID, WorldStructures.SLIME_ISLAND_TYPE.getId().getNamespace());
        assertEquals("slime_island", WorldStructures.SLIME_ISLAND_TYPE.getId().getPath());
    }

    @Test
    void structurePieceTypeRegistersUnderTheSconstructNamespace() {
        assertNotNull(WorldStructures.SLIME_ISLAND_PIECE);
        assertEquals(SConstruct.MOD_ID, WorldStructures.SLIME_ISLAND_PIECE.getId().getNamespace());
        assertEquals("slime_island", WorldStructures.SLIME_ISLAND_PIECE.getId().getPath());
    }

    @Test
    void slimeColorCodecRoundTripsLowerSnakeCaseSerializedNames() {
        // Each enum constant's serialized name matches its id() — JSON surface reads
        // "color": "blue" / "purple" / "magma" / "blood" rather than the UPPERCASE constant
        // name. Test by encoding each constant to JSON via the codec and asserting the
        // raw-string result, then decoding it back.
        assertAll(java.util.Arrays.stream(SlimeColor.values()).map(color -> () -> {
            DataResult<JsonElement> encoded = SlimeColor.CODEC.encodeStart(JsonOps.INSTANCE, color);
            JsonElement element = encoded.result().orElseThrow(() -> new AssertionError("encode " + color + " failed: " + encoded));
            assertTrue(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString(), "encoded form should be a string primitive for " + color);
            assertEquals(color.getSerializedName(), element.getAsString(), "serialized name mismatch for " + color);
            DataResult<com.mojang.datafixers.util.Pair<SlimeColor, JsonElement>> decoded = SlimeColor.CODEC.decode(JsonOps.INSTANCE, element);
            SlimeColor restored = decoded.result().orElseThrow(() -> new AssertionError("decode " + color + " failed: " + decoded)).getFirst();
            assertEquals(color, restored, "round-trip mismatch for " + color);
        }));
    }

    @Test
    void slimeColorJsonParsesEveryLowerSnakeCaseId() {
        // Spot-check the inverse path: parsing a hand-written JSON string with each colour id
        // resolves to the matching enum constant. The structure's MapCodec reads the field
        // value via this path when a datapack JSON instance is loaded.
        assertAll(java.util.Arrays.stream(SlimeColor.values()).map(color -> () -> {
            JsonElement element = JsonParser.parseString("\"" + color.id() + "\"");
            SlimeColor decoded = SlimeColor.CODEC.decode(JsonOps.INSTANCE, element).result().orElseThrow().getFirst();
            assertEquals(color, decoded);
        }));
    }

    @Test
    void radiusBoundsMatchTheTicketSpec() {
        // The codec range gates the JSON value at parse time. Drift here would silently accept
        // a 1-block or 100-block island JSON that the piece's bounding-box / postProcess loop
        // is not tuned for.
        assertEquals(8, SlimeIslandStructure.RADIUS_MIN);
        assertEquals(24, SlimeIslandStructure.RADIUS_MAX);
    }
}
