package slimeknights.sconstruct.tools.material.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MaterialClientCacheTest {

    private static final ResourceLocation WOOD = ResourceLocation.fromNamespaceAndPath("tconstruct", "wood");
    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");

    @AfterEach
    void clear() {
        MaterialClientCache.clearForTest();
    }

    @Test
    void emptyCacheReturnsDefaultColor() {
        assertEquals(MaterialClientCache.DEFAULT_COLOR, MaterialClientCache.getColor(WOOD), "uninitialised cache must return the default colour, not crash");
    }

    @Test
    void populateReplacesSnapshotEntirely() {
        MaterialClientCache.populate(Map.of(WOOD, 0xFF8B5A2B));
        assertEquals(0xFF8B5A2B, MaterialClientCache.getColor(WOOD));
        assertEquals(1, MaterialClientCache.cacheSize());

        // Second populate overwrites the first — stale entries must not survive.
        MaterialClientCache.populate(Map.of(IRON, 0xFFD8D8D8));
        assertEquals(MaterialClientCache.DEFAULT_COLOR, MaterialClientCache.getColor(WOOD), "wood entry must be evicted by the iron-only snapshot");
        assertEquals(0xFFD8D8D8, MaterialClientCache.getColor(IRON));
    }

    @Test
    void populateCopiesSnapshotSoCallerCanMutateSource() {
        Map<ResourceLocation, Integer> mutable = new HashMap<>();
        mutable.put(WOOD, 0xFF8B5A2B);
        MaterialClientCache.populate(mutable);

        // Mutating the source map after populate() must not affect the cache contents.
        mutable.clear();
        assertEquals(0xFF8B5A2B, MaterialClientCache.getColor(WOOD), "cache must not alias the caller's mutable snapshot");
    }

    @Test
    void missingMaterialReturnsDefaultWithoutThrowing() {
        MaterialClientCache.populate(Map.of(WOOD, 0xFF8B5A2B));
        assertEquals(MaterialClientCache.DEFAULT_COLOR, MaterialClientCache.getColor(IRON), "missing entry must fall back, not throw — acceptance criterion #4");
    }
}
