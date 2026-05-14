package slimeknights.sconstruct.port1211.world;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * Pinned-behaviour tests for {@link WorldFeatures}'s {@link ResourceKey} surface. The bootstrap
 * methods themselves can't be exercised here — they require a live
 * {@link net.minecraft.data.worldgen.BootstrapContext} which only exists during datagen — but
 * the key-table contract is fully testable: every {@link SlimeColor} maps to a non-null key in
 * the right registry, namespace, and unique path.
 */
class WorldFeaturesTest {

    @Test
    void configuredKeysExistInTheConfiguredFeatureRegistryForEveryColor() {
        assertAll(java.util.Arrays.stream(SlimeColor.values()).map(color -> () -> {
            ResourceKey<ConfiguredFeature<?, ?>> key = WorldFeatures.configuredTreeKey(color);
            assertNotNull(key, "no configured-feature key for " + color);
            // ResourceKey#registry() returns the parent registry's ResourceLocation — compare
            // against Registries.CONFIGURED_FEATURE.location(), not the meta-ResourceKey
            // constant itself (different types).
            assertEquals(Registries.CONFIGURED_FEATURE.location(), key.registry());
            assertEquals(SConstruct.MOD_ID, key.location().getNamespace());
            assertEquals("slime_" + color.id() + "_tree", key.location().getPath());
        }));
    }

    @Test
    void placedKeysExistInThePlacedFeatureRegistryForEveryColor() {
        assertAll(java.util.Arrays.stream(SlimeColor.values()).map(color -> () -> {
            ResourceKey<PlacedFeature> key = WorldFeatures.placedTreeKey(color);
            assertNotNull(key, "no placed-feature key for " + color);
            assertEquals(Registries.PLACED_FEATURE.location(), key.registry());
            assertEquals(SConstruct.MOD_ID, key.location().getNamespace());
            assertEquals("slime_" + color.id() + "_tree", key.location().getPath());
        }));
    }

    @Test
    void configuredAndPlacedKeysAreDistinctAcrossColors() {
        // Per-colour keys must not collide with any other colour's key in either registry —
        // otherwise two saplings could grow the same tree. Dedupe through a HashSet and
        // assert the size equals the colour count: a duplicate would silently shrink the set.
        Set<ResourceKey<ConfiguredFeature<?, ?>>> seenConfigured = new HashSet<>();
        Set<ResourceKey<PlacedFeature>> seenPlaced = new HashSet<>();
        for (SlimeColor color : SlimeColor.values()) {
            seenConfigured.add(WorldFeatures.configuredTreeKey(color));
            seenPlaced.add(WorldFeatures.placedTreeKey(color));
        }
        assertEquals(SlimeColor.values().length, seenConfigured.size(), "configured keys must be unique per colour");
        assertEquals(SlimeColor.values().length, seenPlaced.size(), "placed keys must be unique per colour");
    }

    @Test
    void configuredAndPlacedKeysShareTheSamePathPerColor() {
        // The placed feature references the configured feature of the same colour. Sharing the
        // path (in different registries) keeps the JSON pair trivially correlatable in the
        // generated data tree — a deviation here would silently emit mismatched files.
        for (SlimeColor color : SlimeColor.values()) {
            assertEquals(WorldFeatures.configuredTreeKey(color).location().getPath(), WorldFeatures.placedTreeKey(color).location().getPath(), "path for " + color);
        }
    }
}
