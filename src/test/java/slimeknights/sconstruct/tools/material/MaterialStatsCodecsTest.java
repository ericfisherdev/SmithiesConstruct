package slimeknights.sconstruct.tools.material;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

/**
 * Pinned-behaviour tests for the {@link MaterialStats} sealed dispatch and each of the five
 * permits. Covers the SMTCON-69 acceptance criteria: sealed-interface compile contract, every
 * record round-trips, and the dispatch codec recognises every type discriminator.
 */
class MaterialStatsCodecsTest {

    @Test
    void typeEnumHasFiveValuesMatchingTheSealedPermitsList() {
        // The sealed permits list is closed against the five subtypes; the enum's value set
        // must agree, or the dispatch codec would silently miss a permit at decode time.
        assertEquals(5, MaterialStats.Type.values().length);
        assertEquals(Arrays.asList("head", "handle", "extra", "bow", "arrow"), Arrays.stream(MaterialStats.Type.values()).map(MaterialStats.Type::id).toList());
    }

    @Test
    void typeByIdRejectsUnknown() {
        assertThrows(IllegalArgumentException.class, () -> MaterialStats.Type.byId("not_a_type"));
    }

    @Test
    void headStatsRoundTripsThroughTheDispatchCodec() {
        HeadStats stats = new HeadStats(60, 2, 4.0f, 5.5f);
        JsonElement json = MaterialStatsCodecs.CODEC.encodeStart(JsonOps.INSTANCE, stats).getOrThrow();
        // Dispatch writes the discriminator into the same JSON object as the record fields.
        assertEquals("head", ((JsonObject) json).get("type").getAsString());
        MaterialStats decoded = MaterialStatsCodecs.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(stats, decoded);
        assertSame(MaterialStats.Type.HEAD, decoded.type());
    }

    @Test
    void handleStatsRoundTripsThroughTheDispatchCodec() {
        HandleStats stats = new HandleStats(1.2f, 0.9f, 1.0f);
        Tag encoded = MaterialStatsCodecs.CODEC.encodeStart(NbtOps.INSTANCE, stats).getOrThrow();
        assertEquals(stats, MaterialStatsCodecs.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void extraStatsRoundTripsThroughTheDispatchCodec() {
        ExtraStats stats = new ExtraStats(50);
        JsonElement json = MaterialStatsCodecs.CODEC.encodeStart(JsonOps.INSTANCE, stats).getOrThrow();
        assertEquals("extra", ((JsonObject) json).get("type").getAsString());
        assertEquals(stats, MaterialStatsCodecs.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void bowStatsRoundTripsThroughTheDispatchCodec() {
        BowStats stats = new BowStats(20, 1.4f, 0.5f);
        Tag encoded = MaterialStatsCodecs.CODEC.encodeStart(NbtOps.INSTANCE, stats).getOrThrow();
        assertEquals(stats, MaterialStatsCodecs.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void arrowStatsRoundTripsThroughTheDispatchCodec() {
        ArrowStats stats = new ArrowStats(2.5f, 8);
        JsonElement json = MaterialStatsCodecs.CODEC.encodeStart(JsonOps.INSTANCE, stats).getOrThrow();
        assertEquals("arrow", ((JsonObject) json).get("type").getAsString());
        assertEquals(stats, MaterialStatsCodecs.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void dispatchRejectsUnknownTypeField() {
        // A foreign mod could ship JSON with an unknown discriminator; the dispatch must fail
        // the parse rather than fall back to a default subtype.
        JsonObject foreign = new JsonObject();
        foreign.addProperty("type", "explosive");
        assertTrue(MaterialStatsCodecs.CODEC.parse(JsonOps.INSTANCE, foreign).isError(), "unknown discriminator must produce a DataResult error");
    }

    @Test
    void everyTypeReportsTheMatchingDiscriminator() {
        // Catches a permit whose type() accessor lies about which Type it claims — would
        // corrupt the dispatch encoding silently otherwise.
        assertAll(() -> assertSame(MaterialStats.Type.HEAD, new HeadStats(1, 0, 1.0f, 1.0f).type()), () -> assertSame(MaterialStats.Type.HANDLE, new HandleStats(1.0f, 1.0f, 1.0f).type()),
                () -> assertSame(MaterialStats.Type.EXTRA, new ExtraStats(1).type()), () -> assertSame(MaterialStats.Type.BOW, new BowStats(1, 1.0f, 0.0f).type()),
                () -> assertSame(MaterialStats.Type.ARROW, new ArrowStats(1.0f, 1).type()));
    }
}
