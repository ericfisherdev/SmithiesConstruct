package slimeknights.sconstruct.common;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.SConstruct;

/**
 * Pinned-behaviour tests for the central {@link TinkerTags} declarations. Verifies that every
 * exposed {@link TagKey} targets the correct vanilla registry and lives in the
 * {@link SConstruct#MOD_ID sconstruct} namespace — a typo in the registry argument or path
 * would not be caught by the compiler but would silently break tag-driven behaviour at runtime.
 */
class TinkerTagsTest {

    @Test
    void blockTagsTargetTheBlockRegistryWithExpectedPaths() {
        assertAll(() -> assertTagBound(TinkerTags.Blocks.SMELTERY_COMPONENT, Registries.BLOCK.location().getPath(), "smeltery/component"),
                () -> assertTagBound(TinkerTags.Blocks.SMELTERY_FLOOR, Registries.BLOCK.location().getPath(), "smeltery/floor"),
                () -> assertTagBound(TinkerTags.Blocks.SMELTERY_WALL, Registries.BLOCK.location().getPath(), "smeltery/wall"),
                () -> assertTagBound(TinkerTags.Blocks.SLIMEGRASS, Registries.BLOCK.location().getPath(), "slimegrass"),
                () -> assertTagBound(TinkerTags.Blocks.SLIMELOGS, Registries.BLOCK.location().getPath(), "slimelogs"));
    }

    @Test
    void itemTagsTargetTheItemRegistryWithExpectedPaths() {
        assertAll(() -> assertTagBound(TinkerTags.Items.SLIMEBALLS, Registries.ITEM.location().getPath(), "slimeballs"),
                () -> assertTagBound(TinkerTags.Items.TOOLPARTS, Registries.ITEM.location().getPath(), "toolparts"));
    }

    @Test
    void fluidTagsTargetTheFluidRegistryWithExpectedPaths() {
        assertTagBound(TinkerTags.Fluids.SMELTERY_FUEL, Registries.FLUID.location().getPath(), "smelteryfuel");
    }

    @Test
    void biomeTagsTargetTheBiomeRegistryWithExpectedPaths() {
        assertTagBound(TinkerTags.Biomes.SLIME_ISLANDS, Registries.BIOME.location().getPath(), "slime_islands");
    }

    @Test
    void everyTagKeyFieldIsNonNull() {
        // Defends against a future refactor accidentally nulling out a constant — registration
        // code references these fields directly, so a null here would NPE at mod construction.
        assertAll(() -> assertNotNull(TinkerTags.Blocks.SMELTERY_COMPONENT), () -> assertNotNull(TinkerTags.Blocks.SMELTERY_FLOOR), () -> assertNotNull(TinkerTags.Blocks.SMELTERY_WALL),
                () -> assertNotNull(TinkerTags.Blocks.SLIMEGRASS), () -> assertNotNull(TinkerTags.Blocks.SLIMELOGS), () -> assertNotNull(TinkerTags.Items.SLIMEBALLS),
                () -> assertNotNull(TinkerTags.Items.TOOLPARTS), () -> assertNotNull(TinkerTags.Fluids.SMELTERY_FUEL), () -> assertNotNull(TinkerTags.Biomes.SLIME_ISLANDS));
    }

    private static void assertTagBound(TagKey<?> tag, String expectedRegistryPath, String expectedTagPath) {
        assertEquals(expectedRegistryPath, tag.registry().location().getPath(), "TagKey targets the wrong vanilla registry");
        assertEquals(SConstruct.MOD_ID, tag.location().getNamespace(), "TagKey should be namespaced under sconstruct");
        assertEquals(expectedTagPath, tag.location().getPath(), "TagKey path drifted from the declared value");
    }
}
