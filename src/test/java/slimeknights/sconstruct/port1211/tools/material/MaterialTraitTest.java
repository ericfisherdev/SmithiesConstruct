package slimeknights.sconstruct.port1211.tools.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import slimeknights.sconstruct.port1211.tools.PartType;

import io.netty.buffer.Unpooled;

/**
 * Pinned-behaviour tests for {@link MaterialTrait}. Locks in the SMTCON-70 shape — a
 * {@code (traitId, slot)} pair that round-trips through JSON, NBT, and the stream codec — and
 * pins the JSON field names ({@code trait}, {@code slot}) so shipped material files don't
 * break on a future field rename.
 */
class MaterialTraitTest {

    private static final ResourceLocation ECOLOGICAL = ResourceLocation.fromNamespaceAndPath("tconstruct", "ecological");

    @Test
    void codecRoundTripsThroughJson() {
        MaterialTrait trait = new MaterialTrait(ECOLOGICAL, PartType.PICKHEAD);
        JsonElement json = MaterialTrait.CODEC.encodeStart(JsonOps.INSTANCE, trait).getOrThrow();
        assertEquals(trait, MaterialTrait.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void codecJsonShapeUsesTraitAndSlotFields() {
        // Pin the JSON field names — datapacks depend on them, and a rename to e.g. "id" or
        // "type" would break every shipped material file silently.
        MaterialTrait trait = new MaterialTrait(ECOLOGICAL, PartType.HANDLE);
        com.google.gson.JsonObject json = (com.google.gson.JsonObject) MaterialTrait.CODEC.encodeStart(JsonOps.INSTANCE, trait).getOrThrow();
        assertEquals(ECOLOGICAL.toString(), json.get("trait").getAsString());
        assertEquals("handle", json.get("slot").getAsString());
    }

    @Test
    void codecRoundTripsThroughNbt() {
        MaterialTrait trait = new MaterialTrait(ECOLOGICAL, PartType.BINDING);
        Tag encoded = MaterialTrait.CODEC.encodeStart(NbtOps.INSTANCE, trait).getOrThrow();
        assertEquals(trait, MaterialTrait.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void streamCodecRoundTrips() {
        MaterialTrait trait = new MaterialTrait(ECOLOGICAL, PartType.ARROW_HEAD);
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
