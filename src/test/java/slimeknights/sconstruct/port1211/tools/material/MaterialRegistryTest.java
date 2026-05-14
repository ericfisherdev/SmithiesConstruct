package slimeknights.sconstruct.port1211.tools.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Pinned-behaviour tests for {@link MaterialRegistry}'s server-side cache surface. The live
 * {@link net.neoforged.neoforge.event.OnDatapackSyncEvent} path needs a running server which
 * isn't available under unit-tests, so the cache contract is exercised directly via the test
 * seams ({@code overwriteCacheForTest}, {@code clearCacheForTest}).
 */
class MaterialRegistryTest {

    private static final ResourceLocation WOOD = ResourceLocation.fromNamespaceAndPath("tconstruct", "wood");
    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");

    @AfterEach
    void clear() {
        MaterialRegistry.clearCacheForTest();
    }

    @Test
    void emptyCacheReturnsEmptyOptional() {
        assertTrue(MaterialRegistry.get(WOOD).isEmpty(), "uninitialised cache must return empty");
        assertTrue(MaterialRegistry.lookup(WOOD).isEmpty());
    }

    @Test
    void cacheReplacementSwapsTheEntireSnapshotAtomically() {
        // Simulate the OnDatapackSyncEvent semantic: a /reload completely replaces the cache.
        // The new snapshot must not retain entries from the previous one.
        Holder<Material> woodHolder = mockHolder(WOOD);
        MaterialRegistry.overwriteCacheForTest(Map.of(WOOD, woodHolder));
        assertEquals(1, MaterialRegistry.cacheSize());
        assertSame(woodHolder, MaterialRegistry.get(WOOD).orElseThrow());

        Holder<Material> ironHolder = mockHolder(IRON);
        MaterialRegistry.overwriteCacheForTest(Map.of(IRON, ironHolder));
        assertTrue(MaterialRegistry.get(WOOD).isEmpty(), "stale entry must not survive cache replacement");
        assertSame(ironHolder, MaterialRegistry.get(IRON).orElseThrow());
        assertEquals(1, MaterialRegistry.cacheSize());
    }

    @Test
    void lookupUnwrapsTheHolderValue() {
        Material woodMaterial = new Material(WOOD, 1, Optional.empty(), Map.of(), List.of(), 0);
        @SuppressWarnings("unchecked")
        Holder<Material> holder = mock(Holder.class);
        org.mockito.Mockito.when(holder.value()).thenReturn(woodMaterial);
        MaterialRegistry.overwriteCacheForTest(Map.of(WOOD, holder));
        assertEquals(woodMaterial, MaterialRegistry.lookup(WOOD).orElseThrow());
    }

    @SuppressWarnings("unchecked")
    private static Holder<Material> mockHolder(ResourceLocation id) {
        Holder<Material> h = mock(Holder.class);
        org.mockito.Mockito.when(h.value()).thenReturn(new Material(id, 1, Optional.empty(), Map.of(), List.of(), 0));
        return h;
    }
}
