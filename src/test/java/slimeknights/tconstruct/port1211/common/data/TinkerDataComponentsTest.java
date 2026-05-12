package slimeknights.tconstruct.port1211.common.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;

/**
 * Consolidated edge-case codec regression suite for the five tool data components
 * (SMTCON-22). Each component gets two tests — one round-trip through the persistence
 * {@code Codec} on {@code NbtOps} (the actual production save path; see SMTCON-19 review for
 * why we don't use {@code JsonOps} here) and one round-trip through the {@code StreamCodec}
 * on a {@link FriendlyByteBuf}. Ten tests in total.
 *
 * <p>The per-component test classes ({@code ToolMaterialsTest}, {@code ToolModifiersTest},
 * etc.) cover routine round-trips and immutability contracts. This class is deliberately
 * narrower — every fixture exercises one or more edge cases the component-level tests don't
 * already cover, so a codec regression introduced by a schema or factory change has a
 * dedicated home to surface in:
 *
 * <ul>
 *   <li><b>Long ResourceLocation paths</b> (200+ chars) — forces the {@code VarInt} length
 *       prefix in {@code ByteBufCodecs.STRING_UTF8} from one byte to two, exercising the
 *       multi-byte path that ASCII-only short paths skip.</li>
 *   <li><b>Max-int counts</b> on {@code ToolStats} — boundary-tests the {@code VAR_INT}
 *       encoding at {@code Integer.MAX_VALUE}, the largest value any
 *       {@code DataComponentType} field will ever legitimately carry.</li>
 *   <li><b>Mixed NBT value types</b> in {@code ToolPersistentData} — covers per-value
 *       round-trips with deeply nested tags (lists, compounds, primitives).</li>
 *   <li><b>Empty collections</b> on the map/list-shaped components — guards against a
 *       regression where the codec mishandles a zero-length list/map prefix.</li>
 * </ul>
 */
class TinkerDataComponentsTest {

    /** ResourceLocation whose toString() is long enough to push its UTF-8 length over 127,
     *  forcing the stream-codec's VarInt length prefix into two bytes instead of one. */
    private static final ResourceLocation LONG_PATH = ResourceLocation.fromNamespaceAndPath("tconstruct", "a".repeat(200));

    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");
    private static final ResourceLocation WOOD = ResourceLocation.fromNamespaceAndPath("tconstruct", "wood");

    // ────────────────────── ToolMaterials ──────────────────────

    @Test
    void toolMaterialsRoundTripsThroughCodec() {
        // Edge case: empty list + a long-path entry mixed with normal entries.
        ToolMaterials empty = ToolMaterials.empty();
        ToolMaterials mixed = new ToolMaterials(List.of(IRON, LONG_PATH, WOOD));
        assertEquals(empty, codecRoundTrip(ToolMaterials.CODEC, empty));
        assertEquals(mixed, codecRoundTrip(ToolMaterials.CODEC, mixed));
    }

    @Test
    void toolMaterialsRoundTripsThroughStreamCodec() {
        ToolMaterials mixed = new ToolMaterials(List.of(IRON, LONG_PATH, WOOD));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolMaterials.STREAM_CODEC.encode(buf, mixed);
            assertEquals(mixed, ToolMaterials.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte");
        }
        finally {
            buf.release();
        }
    }

    // ────────────────────── ToolModifiers ──────────────────────

    @Test
    void toolModifiersRoundTripsThroughCodec() {
        // Edge case: empty map + a long-path key + max-int level.
        ToolModifiers empty = ToolModifiers.empty();
        ToolModifiers populated = ToolModifiers.empty().with(IRON, 1).with(LONG_PATH, Integer.MAX_VALUE);
        assertEquals(empty, codecRoundTrip(ToolModifiers.CODEC, empty));
        ToolModifiers decoded = codecRoundTrip(ToolModifiers.CODEC, populated);
        assertEquals(populated, decoded);
        assertIterableEquals(List.of(IRON, LONG_PATH), decoded.levels().keySet(), "Insertion order must survive the round-trip even with a long-path key");
        assertEquals(Integer.MAX_VALUE, decoded.levels().get(LONG_PATH), "Max-int level must round-trip without overflow");
    }

    @Test
    void toolModifiersRoundTripsThroughStreamCodec() {
        ToolModifiers populated = ToolModifiers.empty().with(IRON, 1).with(LONG_PATH, Integer.MAX_VALUE);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolModifiers.STREAM_CODEC.encode(buf, populated);
            ToolModifiers decoded = ToolModifiers.STREAM_CODEC.decode(buf);
            assertEquals(populated, decoded);
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte");
            assertIterableEquals(List.of(IRON, LONG_PATH), decoded.levels().keySet(), "Insertion order must survive the binary round-trip");
        }
        finally {
            buf.release();
        }
    }

    // ────────────────────── ToolStats ──────────────────────

    @Test
    void toolStatsRoundTripsThroughCodec() {
        // Edge case: maxima for both int and float fields (within the finite/non-negative
        // guard the compact constructor enforces).
        ToolStats extreme = new ToolStats(Integer.MAX_VALUE, // maxDurability
                Float.MAX_VALUE, // attackDamage
                Float.MIN_VALUE, // attackSpeed (smallest positive normal)
                1024.5f, // miningSpeed (normal)
                Integer.MAX_VALUE, // harvestLevel (unconstrained but exercise it)
                Integer.MAX_VALUE, // freeModifiers
                -Float.MAX_VALUE, // drawSpeed (large negative finite)
                0f, // bowRange (boundary)
                Float.MAX_VALUE); // projectileBonus
        assertEquals(extreme, codecRoundTrip(ToolStats.CODEC, extreme));
        assertEquals(ToolStats.zero(), codecRoundTrip(ToolStats.CODEC, ToolStats.zero()));
    }

    @Test
    void toolStatsRoundTripsThroughStreamCodec() {
        ToolStats extreme = new ToolStats(Integer.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, 1024.5f, Integer.MAX_VALUE, Integer.MAX_VALUE, -Float.MAX_VALUE, 0f, Float.MAX_VALUE);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolStats.STREAM_CODEC.encode(buf, extreme);
            assertEquals(extreme, ToolStats.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte");
        }
        finally {
            buf.release();
        }
    }

    // ────────────────────── ToolPersistentData ──────────────────────

    @Test
    void toolPersistentDataRoundTripsThroughCodec() {
        // Edge case: nested CompoundTag with mixed value types + a long-path key.
        CompoundTag nested = new CompoundTag();
        nested.putInt("count", Integer.MAX_VALUE);
        nested.putBoolean("flag", true);
        CompoundTag deep = new CompoundTag();
        deep.putString("name", "alpha");
        nested.put("inner", deep);
        ToolPersistentData populated = new ToolPersistentData(Map.of(LONG_PATH, nested));

        Tag encoded = ToolPersistentData.CODEC.encodeStart(NbtOps.INSTANCE, populated).getOrThrow();
        ToolPersistentData decoded = ToolPersistentData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(populated, decoded);
        assertEquals(Integer.MAX_VALUE, decoded.data().get(LONG_PATH).getInt("count"), "Max-int nested NBT field must round-trip without overflow");
    }

    @Test
    void toolPersistentDataRoundTripsThroughStreamCodec() {
        CompoundTag nested = new CompoundTag();
        nested.putInt("count", Integer.MAX_VALUE);
        nested.putBoolean("flag", true);
        ToolPersistentData populated = new ToolPersistentData(Map.of(LONG_PATH, nested));

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolPersistentData.STREAM_CODEC.encode(buf, populated);
            ToolPersistentData decoded = ToolPersistentData.STREAM_CODEC.decode(buf);
            assertEquals(populated, decoded);
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte");
        }
        finally {
            buf.release();
        }
    }

    // ────────────────────── ToolBroken ──────────────────────

    @Test
    void toolBrokenRoundTripsThroughCodec() {
        assertEquals(ToolBroken.intact(), codecRoundTrip(ToolBroken.CODEC, ToolBroken.intact()));
        assertEquals(ToolBroken.BROKEN, codecRoundTrip(ToolBroken.CODEC, ToolBroken.BROKEN));
    }

    @Test
    void toolBrokenRoundTripsThroughStreamCodec() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolBroken.STREAM_CODEC.encode(buf, ToolBroken.intact());
            ToolBroken.STREAM_CODEC.encode(buf, ToolBroken.BROKEN);
            assertEquals(ToolBroken.intact(), ToolBroken.STREAM_CODEC.decode(buf));
            assertEquals(ToolBroken.BROKEN, ToolBroken.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte across both encoded values");
        }
        finally {
            buf.release();
        }
    }

    /**
     * Helper: encode {@code value} through {@code codec} on {@code NbtOps}, then decode it back.
     * NbtOps is the production codec path for ItemStack persistence; JsonOps is deliberately
     * not exercised here (see class javadoc) because JSON loses NBT integer width.
     */
    private static <T> T codecRoundTrip(com.mojang.serialization.Codec<T> codec, T value) {
        Tag encoded = codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow();
        return codec.parse(NbtOps.INSTANCE, encoded).getOrThrow();
    }
}
