package slimeknights.sconstruct.smeltery.recipe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.gadgets.recipe.DryingRecipe;

import io.netty.buffer.Unpooled;

/**
 * Round-trip tests for every custom {@code Recipe} codec (SMTCON-172) — one JSON pass and one
 * network pass for each of the four recipe types: melting, casting, alloy, and drying.
 *
 * <p>Equality is checked by re-encoding rather than by {@code equals}: {@link FluidStack} and
 * {@link ItemStack} carry no value {@code equals}, so the recipe records they sit inside cannot
 * be compared directly. Instead each test encodes a fixture, decodes it, re-encodes the decoded
 * value, and asserts the two encodings match — a stable encoding both ways proves the codec
 * loses nothing.
 *
 * <p>All fixtures use vanilla fluids and items so the test needs no mod registries; a single
 * {@link Bootstrap#bootStrap()} populates and freezes the built-in registries the
 * {@code FluidStack} / {@code ItemStack} / fluid-tag codecs resolve against.
 */
class RecipeCodecRoundTripTest {

    private static RegistryOps<JsonElement> jsonOps;
    private static RegistryAccess registryAccess;

    @BeforeAll
    static void bootstrap() {
        Bootstrap.bootStrap();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        jsonOps = registryAccess.createSerializationContext(JsonOps.INSTANCE);
    }

    // ----- Melting -----

    @Test
    void meltingRecipeRoundTripsThroughTheJsonCodec() {
        assertJsonRoundTrips(MeltingRecipe.CODEC, new MeltingRecipe(Ingredient.of(Items.IRON_ORE), new FluidStack(Fluids.LAVA, 1000), 800, 100));
    }

    @Test
    void meltingRecipeRoundTripsThroughTheStreamCodec() {
        assertStreamRoundTrips(MeltingRecipe.STREAM_CODEC, new MeltingRecipe(Ingredient.of(Items.IRON_ORE), new FluidStack(Fluids.LAVA, 1000), 800, 100));
    }

    // ----- Casting -----

    @Test
    void castingRecipeRoundTripsThroughTheJsonCodec() {
        assertJsonRoundTrips(CastingRecipe.CODEC, sampleCasting());
    }

    @Test
    void castingRecipeRoundTripsThroughTheStreamCodec() {
        assertStreamRoundTrips(CastingRecipe.STREAM_CODEC, sampleCasting());
    }

    // ----- Alloy -----

    @Test
    void alloyRecipeRoundTripsThroughTheJsonCodec() {
        assertJsonRoundTrips(AlloyRecipe.CODEC, sampleAlloy());
    }

    @Test
    void alloyRecipeRoundTripsThroughTheStreamCodec() {
        assertStreamRoundTrips(AlloyRecipe.STREAM_CODEC, sampleAlloy());
    }

    // ----- Drying -----

    @Test
    void dryingRecipeRoundTripsThroughTheJsonCodec() {
        assertJsonRoundTrips(DryingRecipe.CODEC, new DryingRecipe(Ingredient.of(Items.BEEF), new ItemStack(Items.COOKED_BEEF), 200));
    }

    @Test
    void dryingRecipeRoundTripsThroughTheStreamCodec() {
        assertStreamRoundTrips(DryingRecipe.STREAM_CODEC, new DryingRecipe(Ingredient.of(Items.BEEF), new ItemStack(Items.COOKED_BEEF), 200));
    }

    /**
     * A casting-table recipe: a cast item plus a single-fluid ingredient pouring into an iron
     * ingot. Exercises the optional {@code cast} field and the {@link FluidIngredient} codec.
     */
    private static CastingRecipe sampleCasting() {
        FluidIngredient fluid = new FluidIngredient(HolderSet.direct(Fluids.WATER.builtInRegistryHolder()), 288);
        return new CastingRecipe(fluid, Ingredient.of(Items.STICK), true, 120, new ItemStack(Items.IRON_INGOT, 1), false);
    }

    /**
     * A two-input alloy recipe. Exercises the {@link FluidIngredient} list and a large output
     * amount so the codec is checked against a non-trivial multi-element shape.
     */
    private static AlloyRecipe sampleAlloy() {
        List<FluidIngredient> inputs = List.of(new FluidIngredient(HolderSet.direct(Fluids.WATER.builtInRegistryHolder()), 720),
                new FluidIngredient(HolderSet.direct(Fluids.LAVA.builtInRegistryHolder()), 144));
        return new AlloyRecipe(inputs, new FluidStack(Fluids.WATER, 864), 900);
    }

    /**
     * Encodes {@code value} via the map codec, decodes the JSON back, re-encodes the decoded
     * value, and asserts the two encodings match — proving the codec is loss-free both ways.
     */
    private static <T> void assertJsonRoundTrips(MapCodec<T> mapCodec, T value) {
        Codec<T> codec = mapCodec.codec();
        JsonElement encoded = codec.encodeStart(jsonOps, value).getOrThrow();
        T decoded = codec.parse(jsonOps, encoded).getOrThrow();
        JsonElement reEncoded = codec.encodeStart(jsonOps, decoded).getOrThrow();
        assertEquals(encoded, reEncoded, "decoded recipe must re-encode to the same JSON");
    }

    /**
     * Writes {@code value} to a registry-aware buffer, reads it back, writes the decoded value
     * to a fresh buffer, and asserts the two byte streams match. Also checks the decode buffer
     * is fully drained — a codec that under-reads would leave trailing bytes unnoticed.
     *
     * <p>The {@link RegistryFriendlyByteBuf}s wrap reference-counted Netty buffers, so each is
     * released in the {@code finally} block rather than left for the collector.
     */
    private static <T> void assertStreamRoundTrips(StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, T value) {
        RegistryFriendlyByteBuf first = null;
        RegistryFriendlyByteBuf decodeBuffer = null;
        RegistryFriendlyByteBuf second = null;
        try {
            first = newBuffer();
            streamCodec.encode(first, value);
            byte[] firstBytes = readAll(first);

            decodeBuffer = newBuffer(firstBytes);
            T decoded = streamCodec.decode(decodeBuffer);
            assertEquals(0, decodeBuffer.readableBytes(), "stream codec must consume the full payload");

            second = newBuffer();
            streamCodec.encode(second, decoded);
            assertArrayEquals(firstBytes, readAll(second), "decoded recipe must re-encode to the same bytes");
        }
        finally {
            release(first);
            release(decodeBuffer);
            release(second);
        }
    }

    private static RegistryFriendlyByteBuf newBuffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
    }

    private static RegistryFriendlyByteBuf newBuffer(byte[] contents) {
        return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(contents), registryAccess);
    }

    private static byte[] readAll(RegistryFriendlyByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), bytes);
        return bytes;
    }

    /** Releases a reference-counted buffer when one was allocated; a no-op for {@code null}. */
    private static void release(RegistryFriendlyByteBuf buffer) {
        if (buffer != null) {
            buffer.release();
        }
    }
}
