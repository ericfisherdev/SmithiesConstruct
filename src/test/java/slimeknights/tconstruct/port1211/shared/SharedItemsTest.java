package slimeknights.tconstruct.port1211.shared;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import org.junit.jupiter.api.Test;

import slimeknights.tconstruct.port1211.TConstruct;

/**
 * Pinned-behaviour tests for {@link SharedItems}. Coverage matches {@link SharedMetals#ALL}
 * one-to-one (unlike {@link SharedBlocks}, no metal is skipped), every entry is namespaced
 * under {@link TConstruct#MOD_ID tconstruct} with an {@code ingot_<metal>} path, and the
 * static fields agree with the exposed {@link SharedItems#INGOTS} list.
 */
class SharedItemsTest {

    private static final String INGOT_PREFIX = "ingot_";

    /** Asserts the path starts with {@link #INGOT_PREFIX} and returns the metal id suffix. */
    private static String stripIngotPrefix(DeferredItem<Item> ingot) {
        String path = ingot.getId().getPath();
        assertTrue(path.startsWith(INGOT_PREFIX), "expected path to start with '" + INGOT_PREFIX + "', got " + ingot.getId());
        return path.substring(INGOT_PREFIX.length());
    }

    @Test
    void registersExactlyOneIngotPerMetal() {
        assertEquals(SharedMetals.ALL.size(), SharedItems.INGOTS.size());
    }

    @Test
    void everyIngotPathFollowsTheIngotPrefix() {
        // The "ingot_<metal>" path is referenced by downstream tag and recipe providers; drift
        // would silently break casting and crafting JSON wiring.
        assertAll(SharedItems.INGOTS.stream().map(ingot -> () -> {
            assertEquals(TConstruct.MOD_ID, ingot.getId().getNamespace());
            assertTrue(ingot.getId().getPath().startsWith(INGOT_PREFIX), "expected path to start with '" + INGOT_PREFIX + "', got " + ingot.getId());
        }));
    }

    @Test
    void ingotPathsCoverEveryMetalIdExactlyOnce() {
        Set<String> ingotMetalIds = SharedItems.INGOTS.stream().map(SharedItemsTest::stripIngotPrefix).collect(Collectors.toSet());
        Set<String> expected = SharedMetals.ALL.stream().map(Metal::id).collect(Collectors.toCollection(HashSet::new));
        assertEquals(expected, ingotMetalIds);
    }

    @Test
    void ingotsListPreservesDeclaredOrder() {
        // INGOTS is built top-down by the field initialisers; downstream providers rely on
        // insertion order for deterministic generated artifacts.
        List<String> expected = SharedMetals.ALL.stream().map(Metal::id).collect(Collectors.toList());
        List<String> actual = SharedItems.INGOTS.stream().map(SharedItemsTest::stripIngotPrefix).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Test
    void everyStaticFieldIsNonNullAndPresentInTheList() {
        // Map of the public per-metal fields. A null literal here from a future refactor would
        // technically still leave INGOTS intact, but callers that reach for SharedItems.INGOT_X
        // would NPE — pin both shapes.
        Map<String, DeferredItem<Item>> fields = Map.ofEntries(Map.entry("cobalt", SharedItems.INGOT_COBALT), Map.entry("ardite", SharedItems.INGOT_ARDITE),
                Map.entry("manyullyn", SharedItems.INGOT_MANYULLYN), Map.entry("knightslime", SharedItems.INGOT_KNIGHTSLIME), Map.entry("pigiron", SharedItems.INGOT_PIGIRON),
                Map.entry("silver", SharedItems.INGOT_SILVER), Map.entry("copper", SharedItems.INGOT_COPPER), Map.entry("tin", SharedItems.INGOT_TIN), Map.entry("zinc", SharedItems.INGOT_ZINC),
                Map.entry("brass", SharedItems.INGOT_BRASS), Map.entry("alubrass", SharedItems.INGOT_ALUBRASS), Map.entry("electrum", SharedItems.INGOT_ELECTRUM),
                Map.entry("steel", SharedItems.INGOT_STEEL), Map.entry("lead", SharedItems.INGOT_LEAD), Map.entry("nickel", SharedItems.INGOT_NICKEL));
        assertAll(fields.entrySet().stream().map(entry -> () -> {
            assertNotNull(entry.getValue(), entry.getKey() + " field");
            assertEquals("ingot_" + entry.getKey(), entry.getValue().getId().getPath());
            org.junit.jupiter.api.Assertions.assertTrue(SharedItems.INGOTS.contains(entry.getValue()), entry.getKey() + " missing from INGOTS list");
        }));
    }
}
