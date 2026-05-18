package slimeknights.sconstruct.common.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;

/**
 * Pinned-behaviour tests for {@link ToolBroken}. Trivial in shape but ships the same three
 * round-trip checks as the other data components so a future codec regression on a primitive
 * type would still surface here.
 */
class ToolBrokenTest {

    @Test
    void roundTripsTrueThroughEveryCodec() {
        ToolBroken broken = ToolBroken.BROKEN;
        // JSON
        JsonElement asJson = ToolBroken.CODEC.encodeStart(JsonOps.INSTANCE, broken).getOrThrow();
        assertEquals(broken, ToolBroken.CODEC.parse(JsonOps.INSTANCE, asJson).getOrThrow());
        // NBT (the production save path)
        Tag asNbt = ToolBroken.CODEC.encodeStart(NbtOps.INSTANCE, broken).getOrThrow();
        assertEquals(broken, ToolBroken.CODEC.parse(NbtOps.INSTANCE, asNbt).getOrThrow());
        // Stream
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolBroken.STREAM_CODEC.encode(buf, broken);
            assertEquals(broken, ToolBroken.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte");
        }
        finally {
            buf.release();
        }
    }

    @Test
    void roundTripsFalseThroughEveryCodec() {
        ToolBroken intact = ToolBroken.intact();
        // JSON
        JsonElement asJson = ToolBroken.CODEC.encodeStart(JsonOps.INSTANCE, intact).getOrThrow();
        assertEquals(intact, ToolBroken.CODEC.parse(JsonOps.INSTANCE, asJson).getOrThrow());
        // NBT
        Tag asNbt = ToolBroken.CODEC.encodeStart(NbtOps.INSTANCE, intact).getOrThrow();
        assertEquals(intact, ToolBroken.CODEC.parse(NbtOps.INSTANCE, asNbt).getOrThrow());
        // Stream
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolBroken.STREAM_CODEC.encode(buf, intact);
            assertEquals(intact, ToolBroken.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte");
        }
        finally {
            buf.release();
        }
    }

    @Test
    void intactFactoryReturnsTheCanonicalSingleton() {
        ToolBroken first = ToolBroken.intact();
        ToolBroken second = ToolBroken.intact();
        assertSame(first, second, "intact() must reuse the canonical singleton");
        assertFalse(first.broken(), "intact() must return broken=false");
    }

    @Test
    void brokenConstantHoldsTheTrueState() {
        // BROKEN is a public constant rather than a factory because the natural factory name
        // collides with the record accessor. Same singleton contract though.
        assertTrue(ToolBroken.BROKEN.broken(), "BROKEN constant must hold broken=true");
    }

    @Test
    void intactAndBrokenAreDistinct() {
        assertNotEquals(ToolBroken.intact(), ToolBroken.BROKEN, "intact() and BROKEN must compare as different values");
        assertNotEquals(ToolBroken.intact().hashCode(), ToolBroken.BROKEN.hashCode(), "intact() and BROKEN should have different hashCodes (true/false hash differently)");
    }
}
