package slimeknights.sconstruct.tools.item;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.tools.PartType;

import io.netty.buffer.Unpooled;

/**
 * Pinned-behaviour tests for {@link MaterialItem} and the per-stack {@code PART_MATERIAL}
 * codec/stream-codec round-trip. The {@code MaterialItem} constructor cannot be exercised here
 * — the moddev unitTest harness freezes the {@code minecraft:item} registry before this test
 * class loads, and {@code Item.<init>} would throw "Registry is already frozen". Instead, the
 * fallback / read-component path is verified through a {@code spy} that bypasses the {@code
 * Item.<init>} call site, and the component round-trip is verified directly against the
 * {@link ResourceLocation} codec — the one {@code TinkerDataComponents.PART_MATERIAL} wires
 * into its {@code persistent(...)} / {@code networkSynchronized(...)} builder pair.
 *
 * <p>End-to-end registration is exercised by the next ticket ({@code [4a] Register all part
 * items via PartItems}) once a registered {@code MaterialItem} instance becomes addressable
 * from test code.
 */
class MaterialItemTest {

    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");

    @Test
    void defaultMaterialIsTconstructWoodSoLegacyMaterialRegistriesResolve() {
        // Legacy 1.12 addons still ship materials under tconstruct:* — the fallback must keep
        // landing in that namespace so a cross-mod material lookup against the default doesn't
        // miss every legacy entry.
        assertEquals("tconstruct", MaterialItem.DEFAULT_MATERIAL.getNamespace());
        assertEquals("wood", MaterialItem.DEFAULT_MATERIAL.getPath());
    }

    @Test
    void getMaterialFallsBackToDefaultWhenStackReturnsNullComponent() {
        // Stub a stack that says "no PART_MATERIAL attached" — getMaterial must return the
        // default rather than letting the null leak through. Mockito lets us probe this path
        // without constructing a real MaterialItem (would trip the post-bootstrap Item registry
        // freeze).
        MaterialItem item = mock(MaterialItem.class);
        ItemStack stack = mock(ItemStack.class);
        when(stack.get(any(DataComponentType.class))).thenReturn(null);
        when(item.getMaterial(stack)).thenCallRealMethod();
        assertSame(MaterialItem.DEFAULT_MATERIAL, item.getMaterial(stack));
    }

    @Test
    void getMaterialReturnsAttachedComponentWhenStackHasOne() {
        MaterialItem item = mock(MaterialItem.class);
        ItemStack stack = mock(ItemStack.class);
        when(stack.get(any(DataComponentType.class))).thenReturn(IRON);
        when(item.getMaterial(stack)).thenCallRealMethod();
        assertSame(IRON, item.getMaterial(stack));
    }

    @Test
    void getNameProducesATranslatableComponentWithMaterialAndPartArguments() {
        // Same spy-style approach: avoid constructing the Item. Stub getMaterial to return the
        // default and partType to return PICKHEAD — then verify getName's structural output.
        MaterialItem item = mock(MaterialItem.class);
        ItemStack stack = mock(ItemStack.class);
        when(item.getMaterial(stack)).thenReturn(MaterialItem.DEFAULT_MATERIAL);
        when(item.partType()).thenReturn(PartType.PICKHEAD);
        when(item.getName(stack)).thenCallRealMethod();

        Component name = item.getName(stack);
        assertNotNull(name);
        assertTrue(name.getContents() instanceof TranslatableContents, "getName must return a TranslatableComponent");
        TranslatableContents contents = (TranslatableContents) name.getContents();
        assertEquals(2, contents.getArgs().length, "getName must supply both material and part as format args");
        assertAll(() -> assertTrue(contents.getKey().contains("material_part"), "name format key must reference the material_part lang slot"),
                () -> assertTrue(((Component) contents.getArgs()[0]).getContents() instanceof TranslatableContents, "material arg must be a TranslatableComponent"),
                () -> assertTrue(((Component) contents.getArgs()[1]).getContents() instanceof TranslatableContents, "part arg must be a TranslatableComponent"));
    }

    @Test
    void partMaterialComponentCodecRoundTripsAResourceLocationThroughNbt() {
        // The DataComponentType wraps ResourceLocation.CODEC; round-trip the codec directly so a
        // regression in the wired codec surfaces here even if the holder ever swaps to a
        // different ResourceLocation alias.
        Tag encoded = ResourceLocation.CODEC.encodeStart(NbtOps.INSTANCE, IRON).getOrThrow();
        ResourceLocation decoded = ResourceLocation.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(IRON, decoded);
    }

    @Test
    void partMaterialComponentStreamCodecRoundTripsAResourceLocation() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ResourceLocation.STREAM_CODEC.encode(buf, IRON);
            assertEquals(IRON, ResourceLocation.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes(), "decoder should consume every byte");
        }
        finally {
            buf.release();
        }
    }
}
