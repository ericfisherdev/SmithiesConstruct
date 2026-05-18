package slimeknights.sconstruct.shared;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import slimeknights.sconstruct.SConstruct;

/**
 * Pinned-behaviour tests for {@link SharedItems}. Coverage of both item families ({@code
 * ingot_<metal>} and {@code nugget_<metal>}) matches {@link SharedMetals#ALL} one-to-one; every
 * entry is namespaced under {@link SConstruct#MOD_ID sconstruct} with the family prefix, and
 * the public per-metal static fields agree with the exposed {@link SharedItems#INGOTS} and
 * {@link SharedItems#NUGGETS} lists.
 */
class SharedItemsTest {

    private static final String INGOT_PREFIX = "ingot_";
    private static final String NUGGET_PREFIX = "nugget_";

    /** One row per registered item family — drives every cross-family parameterised test. */
    private record Family(String name, String prefix, List<DeferredItem<Item>> items, Map<String, DeferredItem<Item>> staticFields) {
    }

    private static Stream<Family> families() {
        return Stream.of(new Family("ingot", INGOT_PREFIX, SharedItems.INGOTS, ingotFields()), new Family("nugget", NUGGET_PREFIX, SharedItems.NUGGETS, nuggetFields()));
    }

    /** Asserts the path starts with the family prefix and returns the metal id suffix. */
    private static String stripPrefix(DeferredItem<Item> item, String prefix) {
        String path = item.getId().getPath();
        assertTrue(path.startsWith(prefix), "expected path to start with '" + prefix + "', got " + item.getId());
        return path.substring(prefix.length());
    }

    @ParameterizedTest
    @MethodSource("families")
    void registersExactlyOneItemPerMetal(Family family) {
        assertEquals(SharedMetals.ALL.size(), family.items().size(), family.name());
    }

    @ParameterizedTest
    @MethodSource("families")
    void everyPathFollowsTheFamilyPrefix(Family family) {
        // The "<prefix><metal>" path is referenced by downstream tag and recipe providers;
        // drift would silently break casting and crafting JSON wiring.
        assertAll(family.items().stream().map(item -> () -> {
            assertEquals(SConstruct.MOD_ID, item.getId().getNamespace());
            assertTrue(item.getId().getPath().startsWith(family.prefix()), "expected path to start with '" + family.prefix() + "', got " + item.getId());
        }));
    }

    @ParameterizedTest
    @MethodSource("families")
    void pathsCoverEveryMetalIdExactlyOnce(Family family) {
        Set<String> metalIds = family.items().stream().map(item -> stripPrefix(item, family.prefix())).collect(Collectors.toSet());
        Set<String> expected = SharedMetals.ALL.stream().map(Metal::id).collect(Collectors.toCollection(HashSet::new));
        assertEquals(expected, metalIds, family.name());
    }

    @ParameterizedTest
    @MethodSource("families")
    void listPreservesDeclaredOrder(Family family) {
        // Each family list is built top-down by its field initialisers; downstream providers
        // rely on insertion order for deterministic generated artifacts.
        List<String> expected = SharedMetals.ALL.stream().map(Metal::id).collect(Collectors.toList());
        List<String> actual = family.items().stream().map(item -> stripPrefix(item, family.prefix())).collect(Collectors.toList());
        assertEquals(expected, actual, family.name());
    }

    @ParameterizedTest
    @MethodSource("families")
    void everyStaticFieldIsNonNullAndPresentInTheList(Family family) {
        // A null literal from a future refactor would technically leave the list intact, but
        // callers that reach for SharedItems.{INGOT,NUGGET}_X would NPE — pin both shapes.
        assertAll(family.staticFields().entrySet().stream().map(entry -> () -> {
            assertNotNull(entry.getValue(), entry.getKey() + " " + family.name() + " field");
            assertEquals(family.prefix() + entry.getKey(), entry.getValue().getId().getPath());
            assertTrue(family.items().contains(entry.getValue()), entry.getKey() + " missing from " + family.name() + " list");
        }));
    }

    @Test
    void slimeballsRegisterTheFourExpectedVariantsInOrder() {
        // Order is the AC's "purple, blood, blue, magma". Phase-3 loot tables and Phase-6
        // recipes iterate SLIMEBALLS — drifting the order shuffles generated artifacts.
        List<String> expected = List.of("slimeball_purple", "slimeball_blood", "slimeball_blue", "slimeball_magma");
        List<String> actual = SharedItems.SLIMEBALLS.stream().map(slimeball -> slimeball.getId().getPath()).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Test
    void slimeballStaticFieldsAreNonNullAndPresentInTheList() {
        Map<String, DeferredItem<Item>> fields = Map.of("purple", SharedItems.SLIMEBALL_PURPLE, "blood", SharedItems.SLIMEBALL_BLOOD, "blue", SharedItems.SLIMEBALL_BLUE, "magma",
                SharedItems.SLIMEBALL_MAGMA);
        assertAll(fields.entrySet().stream().map(entry -> () -> {
            assertNotNull(entry.getValue(), entry.getKey() + " slimeball field");
            assertEquals("slimeball_" + entry.getKey(), entry.getValue().getId().getPath());
            assertEquals(SConstruct.MOD_ID, entry.getValue().getId().getNamespace());
            assertTrue(SharedItems.SLIMEBALLS.contains(entry.getValue()), entry.getKey() + " missing from SLIMEBALLS list");
        }));
    }

    @Test
    void baconIsRegisteredWithLegacyFoodValues() {
        // Nutrition 3 + saturation modifier 0.6 are the 1.12 values — pinning them here lets
        // save-game porters open a bacon and see the same hunger refill they'd get in legacy.
        assertNotNull(SharedItems.BACON);
        assertEquals("bacon", SharedItems.BACON.getId().getPath());
        assertEquals(SConstruct.MOD_ID, SharedItems.BACON.getId().getNamespace());
        net.minecraft.world.food.FoodProperties food = SharedItems.BACON.get().components().get(net.minecraft.core.component.DataComponents.FOOD);
        assertNotNull(food, "bacon should carry a FoodProperties data component");
        assertEquals(3, food.nutrition());
        // FoodProperties.saturation() returns the absolute value (nutrition * modifier * 2),
        // not the raw modifier passed to the builder. 3 * 0.6 * 2 = 3.6.
        assertEquals(3.6f, food.saturation(), 0.0001f);
    }

    @Test
    void mudbrickIsRegisteredWithNoFoodComponent() {
        // Mud brick is a plain crafting item — guard against a future copy/paste accidentally
        // turning it into food.
        assertNotNull(SharedItems.MUDBRICK);
        assertEquals("mudbrick", SharedItems.MUDBRICK.getId().getPath());
        assertEquals(SConstruct.MOD_ID, SharedItems.MUDBRICK.getId().getNamespace());
        org.junit.jupiter.api.Assertions.assertNull(SharedItems.MUDBRICK.get().components().get(net.minecraft.core.component.DataComponents.FOOD),
                "mudbrick is not food and should not carry a FoodProperties component");
    }

    @Test
    void ingotsAndNuggetsAreParallelByIndex() {
        // 1:1 correspondence at the same index means index 0 of INGOTS and NUGGETS reference
        // the same metal. Phase-5 casting pairs them by index — keep them aligned here.
        assertEquals(SharedItems.INGOTS.size(), SharedItems.NUGGETS.size());
        for (int i = 0; i < SharedItems.INGOTS.size(); i++) {
            String ingotId = stripPrefix(SharedItems.INGOTS.get(i), INGOT_PREFIX);
            String nuggetId = stripPrefix(SharedItems.NUGGETS.get(i), NUGGET_PREFIX);
            assertEquals(ingotId, nuggetId, "index " + i);
        }
    }

    private static Map<String, DeferredItem<Item>> ingotFields() {
        return Map.ofEntries(Map.entry("cobalt", SharedItems.INGOT_COBALT), Map.entry("ardite", SharedItems.INGOT_ARDITE), Map.entry("manyullyn", SharedItems.INGOT_MANYULLYN),
                Map.entry("knightslime", SharedItems.INGOT_KNIGHTSLIME), Map.entry("pigiron", SharedItems.INGOT_PIGIRON), Map.entry("silver", SharedItems.INGOT_SILVER),
                Map.entry("copper", SharedItems.INGOT_COPPER), Map.entry("tin", SharedItems.INGOT_TIN), Map.entry("zinc", SharedItems.INGOT_ZINC), Map.entry("brass", SharedItems.INGOT_BRASS),
                Map.entry("alubrass", SharedItems.INGOT_ALUBRASS), Map.entry("electrum", SharedItems.INGOT_ELECTRUM), Map.entry("steel", SharedItems.INGOT_STEEL),
                Map.entry("lead", SharedItems.INGOT_LEAD), Map.entry("nickel", SharedItems.INGOT_NICKEL));
    }

    private static Map<String, DeferredItem<Item>> nuggetFields() {
        return Map.ofEntries(Map.entry("cobalt", SharedItems.NUGGET_COBALT), Map.entry("ardite", SharedItems.NUGGET_ARDITE), Map.entry("manyullyn", SharedItems.NUGGET_MANYULLYN),
                Map.entry("knightslime", SharedItems.NUGGET_KNIGHTSLIME), Map.entry("pigiron", SharedItems.NUGGET_PIGIRON), Map.entry("silver", SharedItems.NUGGET_SILVER),
                Map.entry("copper", SharedItems.NUGGET_COPPER), Map.entry("tin", SharedItems.NUGGET_TIN), Map.entry("zinc", SharedItems.NUGGET_ZINC), Map.entry("brass", SharedItems.NUGGET_BRASS),
                Map.entry("alubrass", SharedItems.NUGGET_ALUBRASS), Map.entry("electrum", SharedItems.NUGGET_ELECTRUM), Map.entry("steel", SharedItems.NUGGET_STEEL),
                Map.entry("lead", SharedItems.NUGGET_LEAD), Map.entry("nickel", SharedItems.NUGGET_NICKEL));
    }
}
