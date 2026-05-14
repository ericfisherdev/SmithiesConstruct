package slimeknights.sconstruct.port1211.tools.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;

/**
 * Pinned-behaviour tests for the {@link MaterialTrait} placeholder used by {@link Material}.
 * SMTCON-70 will grow the record; these tests lock in the SMTCON-68 surface (id + level pair
 * round-trips through JSON, NBT, and the stream codec).
 */
class MaterialTraitTest {

    private static final ResourceLocation ECOLOGICAL = ResourceLocation.fromNamespaceAndPath("tconstruct", "ecological");

    @Test
    void codecRoundTripsThroughJson() {
        MaterialTrait trait = new MaterialTrait(ECOLOGICAL, 3);
        JsonElement json = MaterialTrait.CODEC.encodeStart(JsonOps.INSTANCE, trait).getOrThrow();
        assertEquals(trait, MaterialTrait.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void codecLevelDefaultsToOneWhenOmitted() {
        // The JSON shorthand {"id":"..."} should decode to level=1 so material files don't
        // need a level field for vanilla level-1 traits.
        JsonElement json = JsonOps.INSTANCE.createMap(java.util.Map.of(JsonOps.INSTANCE.createString("id"), JsonOps.INSTANCE.createString(ECOLOGICAL.toString())));
        MaterialTrait decoded = MaterialTrait.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(new MaterialTrait(ECOLOGICAL, 1), decoded);
    }

    @Test
    void codecRoundTripsThroughNbt() {
        MaterialTrait trait = new MaterialTrait(ECOLOGICAL, 2);
        Tag encoded = MaterialTrait.CODEC.encodeStart(NbtOps.INSTANCE, trait).getOrThrow();
        assertEquals(trait, MaterialTrait.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void streamCodecRoundTrips() {
        MaterialTrait trait = new MaterialTrait(ECOLOGICAL, 4);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            MaterialTrait.STREAM_CODEC.encode(buf, trait);
            assertEquals(trait, MaterialTrait.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte");
        }
        finally {
            buf.release();
        }
    }
}
