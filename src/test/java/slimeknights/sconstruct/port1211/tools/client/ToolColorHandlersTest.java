package slimeknights.sconstruct.port1211.tools.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.tools.item.MaterialItem;
import slimeknights.sconstruct.port1211.tools.material.client.MaterialClientCache;

/**
 * Pinned-behaviour tests for {@link ToolColorHandlers#resolveColor(ItemStack, int)}. The
 * tinting logic is exercised directly — the {@code RegisterColorHandlersEvent} side of the
 * class needs a live client + registered items so it's deferred to a GameTest under
 * SMTCON-96.
 */
class ToolColorHandlersTest {

    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");
    private static final int IRON_TINT = 0xFFD8D8D8;
    private static final int WOOD_TINT = 0xFF8B5A2B;

    @BeforeEach
    void resetCacheBeforeEach() {
        // Cache is static global state — reset before every test as well as after, so the first
        // test in the class never sees leftover entries from a previously-running test class.
        MaterialClientCache.populate(Map.of());
    }

    @AfterEach
    void clearCache() {
        // Use populate(empty) rather than the package-private clearForTest seam — this test
        // lives in a different package and the public populate API resets the cache equally
        // well by overwriting the snapshot with an empty map.
        MaterialClientCache.populate(Map.of());
    }

    @Test
    void layerOtherThanZeroAlwaysReturnsNoTint() {
        // Layer != 0 must short-circuit to -1 (vanilla's "no tint" sentinel) regardless of the
        // material or the cache state — overlay layers ship un-tinted so the head shape reads on
        // top of the tinted base.
        ItemStack stack = mock(ItemStack.class);
        assertEquals(-1, ToolColorHandlers.resolveColor(stack, 1));
        assertEquals(-1, ToolColorHandlers.resolveColor(stack, 2));
    }

    @Test
    void layerZeroWithNoComponentFallsBackToDefaultMaterial() {
        // No PART_MATERIAL attached → fall back to DEFAULT_MATERIAL (tconstruct:wood) and look
        // its tint up in the cache.
        MaterialClientCache.populate(Map.of(MaterialItem.DEFAULT_MATERIAL, WOOD_TINT));
        ItemStack stack = mock(ItemStack.class);
        when(stack.get(eq(TinkerDataComponents.PART_MATERIAL.get()))).thenReturn(null);
        assertEquals(WOOD_TINT, ToolColorHandlers.resolveColor(stack, 0));
    }

    @Test
    void layerZeroReturnsCachedTintForAttachedMaterial() {
        MaterialClientCache.populate(Map.of(IRON, IRON_TINT));
        ItemStack stack = mock(ItemStack.class);
        when(stack.get(eq(TinkerDataComponents.PART_MATERIAL.get()))).thenReturn(IRON);
        assertEquals(IRON_TINT, ToolColorHandlers.resolveColor(stack, 0));
    }

    @Test
    void missingMaterialIdReturnsDefaultColorWithoutCrashing() {
        // Cache empty for the attached id (e.g. addon material that didn't sync) — the renderer
        // must receive the white sentinel rather than crash. Acceptance criterion #4.
        ItemStack stack = mock(ItemStack.class);
        when(stack.get(eq(TinkerDataComponents.PART_MATERIAL.get()))).thenReturn(IRON);
        assertEquals(MaterialClientCache.DEFAULT_COLOR, ToolColorHandlers.resolveColor(stack, 0));
    }
}
