package slimeknights.sconstruct.port1211.tools;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;

import io.netty.buffer.Unpooled;

/**
 * Pinned-behaviour tests for {@link PartType}. The enum is the taxonomy every later tools task
 * keys off, so this suite locks in: the closed set of values, the stable {@code id()} string
 * per value (the codec serialised form), and the JSON/NBT/stream round-trip contract.
 */
class PartTypeTest {

    @Test
    void exactlyEighteenValuesShipInTheExpectedOrder() {
        // Order is part of the public surface: appending is non-breaking, reordering would
        // change Enum#ordinal and break any caller that ever stores ordinals (forbidden by
        // contract, but pinned here so the next reviewer sees it). WIDEGUARD/LARGEPLATE
        // appended in SMTCON-73 to support legacy-accurate ToolDefinition entries
        // (BroadSword, Hammer, LumberAxe).
        assertEquals(18, PartType.values().length);
        assertEquals(PartType.PICKHEAD, PartType.values()[0]);
        assertEquals(PartType.FLETCHING, PartType.values()[15]);
        assertEquals(PartType.WIDEGUARD, PartType.values()[16]);
        assertEquals(PartType.LARGEPLATE, PartType.values()[17]);
    }

    @Test
    void everyValueHasAUniqueLowercaseId() {
        // Hyphen/uppercase would break the resource-path contract (lang key suffix, registry
        // path). Duplicate ids would corrupt the codec round-trip silently.
        long unique = Arrays.stream(PartType.values()).map(PartType::id).distinct().count();
        assertEquals(PartType.values().length, unique, "ids must be unique");
        for (PartType part : PartType.values()) {
            assertTrue(part.id().matches("[a-z_]+"), () -> "id must be lowercase + optional underscore: " + part.id());
        }
    }

    @Test
    void arrowHeadKeepsItsUnderscoreToMatchTheLegacyRegistryPath() {
        // Single value where the underscore differs from the enum-name convention — pinned so a
        // future rename to {@code ARROWHEAD} (cleaner enum name) doesn't silently break the
        // resource path round-trip against legacy {@code arrow_head} part stacks.
        assertEquals("arrow_head", PartType.ARROW_HEAD.id());
    }

    @Test
    void translationKeyUsesTheModNamespaceAndIdSuffix() {
        for (PartType part : PartType.values()) {
            assertEquals("part." + SConstruct.MOD_ID + "." + part.id(), part.translationKey());
        }
    }

    @Test
    void codecRoundTripsEveryValueThroughNbt() {
        // NbtOps is the production save path used by DataComponentType.persistent.
        for (PartType part : PartType.values()) {
            Tag encoded = PartType.CODEC.encodeStart(NbtOps.INSTANCE, part).getOrThrow();
            PartType decoded = PartType.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
            assertEquals(part, decoded);
        }
    }

    @Test
    void streamCodecRoundTripsEveryValue() {
        // Encode the whole enum into one buffer, then decode in order. Catches any per-value
        // wire-format regression (e.g. truncation, extra padding) without a 16-buffer setup.
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (PartType part : PartType.values()) {
                PartType.STREAM_CODEC.encode(buf, part);
            }
            for (PartType part : PartType.values()) {
                assertEquals(part, PartType.STREAM_CODEC.decode(buf));
            }
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte");
        }
        finally {
            buf.release();
        }
    }

    @Test
    void byIdResolvesKnownIdsAndRejectsUnknown() {
        // The stream codec routes decode through byId, so an unknown id must throw rather than
        // silently return null (a null PartType would crash later attribute resolution with a
        // less actionable error site).
        assertAll(() -> assertEquals(PartType.HANDLE, PartType.byId("handle")), () -> assertEquals(PartType.ARROW_HEAD, PartType.byId("arrow_head")),
                () -> assertThrows(IllegalArgumentException.class, () -> PartType.byId("not_a_part")));
    }

    @Test
    void getSerializedNameMatchesId() {
        // StringRepresentable contract: the codec built from getSerializedName must agree with
        // the codec built from id(). If these diverged, the JSON form and the internal id()
        // could disagree silently.
        for (PartType part : PartType.values()) {
            assertNotNull(part.getSerializedName());
            assertEquals(part.id(), part.getSerializedName());
        }
    }
}
