package slimeknights.sconstruct.port1211.shared;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;

/**
 * Pinned-behaviour tests for {@link SharedTabs}. Verifies the {@link CreativeModeTab} is
 * registered at the expected id with the cobalt-ingot icon and {@code itemGroup.sconstruct}
 * lang key, and that {@link SharedTabs#acceptAll(java.util.function.Consumer)} visits every
 * Phase-2 shared-pulse item exactly once.
 */
class SharedTabsTest {

    @Test
    void tabIsRegisteredAtTconstructGeneral() {
        assertAll(() -> assertEquals(SConstruct.MOD_ID, SharedTabs.GENERAL.getId().getNamespace()), () -> assertEquals("general", SharedTabs.GENERAL.getId().getPath()),
                () -> assertNotNull(SharedTabs.GENERAL.get(), "GENERAL tab must resolve to a CreativeModeTab"));
    }

    @Test
    void tabUsesTheItemGroupLangKey() {
        assertEquals(Component.translatable("itemGroup.sconstruct"), SharedTabs.GENERAL.get().getDisplayName());
    }

    @Test
    void tabIconIsACobaltIngot() {
        ItemStack icon = SharedTabs.GENERAL.get().getIconItem();
        assertNotNull(icon);
        // Picking cobalt over a generic stack mirrors the "first Tinkers metal" recognition.
        // Swapping it (e.g. to manyullyn) silently rebrands the tab strip — pin it.
        assertSame(SharedItems.INGOT_COBALT.get(), icon.getItem());
    }

    @Test
    void acceptAllVisitsEveryPhase2ItemExactlyOnce() {
        List<ItemLike> visited = new ArrayList<>();
        SharedTabs.acceptAll(visited::add);

        // Coverage size: metal blocks (13) + decoratives (3) + ingots (15) + nuggets (15)
        // + slimeballs (4) + 4 miscs (mudbrick, bacon, blood bucket, materials book).
        int expectedSize = SharedBlocks.METAL_BLOCKS.size() + 3 + SharedItems.INGOTS.size() + SharedItems.NUGGETS.size() + SharedItems.SLIMEBALLS.size() + 4;
        assertEquals(expectedSize, visited.size(), "wrong number of items accepted into GENERAL");

        // No duplicates per AC — collecting into a Set of identity-keyed items must match the
        // list size. Use Item handles (ItemLike.asItem()) so block/blockitem identity collapses.
        Set<net.minecraft.world.item.Item> uniqueItems = new HashSet<>();
        for (ItemLike like : visited) {
            assertTrue(uniqueItems.add(like.asItem()), "duplicate item accepted: " + like.asItem());
        }
    }

    @Test
    void allFourThemedTabsAreRegistered() {
        // SMTCON-168 splits the single tab into GENERAL + three themed tabs. Pin every id so a
        // future refactor that drops or renames a tab trips here.
        // Pin the full tab id (namespace + path) so a wrong namespace cannot slip past a
        // path-only check.
        assertAll(() -> assertEquals("sconstruct:general", SharedTabs.GENERAL.getId().toString()), () -> assertEquals("sconstruct:tools", SharedTabs.TOOLS.getId().toString()),
                () -> assertEquals("sconstruct:parts", SharedTabs.PARTS.getId().toString()), () -> assertEquals("sconstruct:materials", SharedTabs.MATERIALS.getId().toString()),
                () -> assertNotNull(SharedTabs.TOOLS.get(), "TOOLS tab must resolve"), () -> assertNotNull(SharedTabs.PARTS.get(), "PARTS tab must resolve"),
                () -> assertNotNull(SharedTabs.MATERIALS.get(), "MATERIALS tab must resolve"));
    }

    @Test
    void themedTabsUseTheirItemGroupLangKeys() {
        assertAll(() -> assertEquals(Component.translatable("itemGroup.sconstruct.tools"), SharedTabs.TOOLS.get().getDisplayName()),
                () -> assertEquals(Component.translatable("itemGroup.sconstruct.parts"), SharedTabs.PARTS.get().getDisplayName()),
                () -> assertEquals(Component.translatable("itemGroup.sconstruct.materials"), SharedTabs.MATERIALS.get().getDisplayName()));
    }

    @Test
    void acceptMaterialsVisitsOnlyTheMaterialsSubset() {
        List<ItemLike> visited = new ArrayList<>();
        SharedTabs.acceptMaterials(visited::add);

        // Materials subset: metal storage blocks (13) + ingots (15) + nuggets (15)
        // + slimeballs (4). Decoratives and misc items stay GENERAL-only.
        int expectedSize = SharedBlocks.METAL_BLOCKS.size() + SharedItems.INGOTS.size() + SharedItems.NUGGETS.size() + SharedItems.SLIMEBALLS.size();
        assertEquals(expectedSize, visited.size(), "wrong number of items accepted into MATERIALS");

        // No duplicates — a repeated family would inflate the count and could mask a missing
        // one, so collecting into an identity set must match the visited list size.
        Set<net.minecraft.world.item.Item> items = new HashSet<>();
        for (ItemLike like : visited) {
            assertTrue(items.add(like.asItem()), "duplicate item accepted into MATERIALS: " + like.asItem());
        }
        assertAll(() -> assertTrue(items.contains(SharedItems.INGOT_COBALT.get()), "ingots family missing"),
                () -> assertTrue(items.contains(SharedItems.NUGGET_COBALT.get()), "nuggets family missing"),
                () -> assertTrue(items.contains(SharedItems.SLIMEBALL_BLUE.get()), "slimeballs family missing"),
                () -> assertTrue(items.contains(SharedBlocks.COBALT.get().asItem()), "metal storage block items missing"),
                () -> assertTrue(!items.contains(SharedBlocks.GLOW.get().asItem()), "decoratives must not be in MATERIALS"),
                () -> assertTrue(!items.contains(SharedItems.BACON.get()), "misc items must not be in MATERIALS"));
    }

    @Test
    void acceptAllSurfacesTheKnownAnchorItems() {
        // Pin a handful of representative items so a future refactor that drops a family
        // (e.g. forgets to include slimeballs) trips the assertion.
        List<ItemLike> visited = new ArrayList<>();
        SharedTabs.acceptAll(visited::add);
        Set<net.minecraft.world.item.Item> items = new HashSet<>();
        visited.forEach(like -> items.add(like.asItem()));
        assertAll(() -> assertTrue(items.contains(SharedItems.INGOT_COBALT.get()), "ingots family missing"),
                () -> assertTrue(items.contains(SharedItems.NUGGET_COBALT.get()), "nuggets family missing"),
                () -> assertTrue(items.contains(SharedItems.SLIMEBALL_BLUE.get()), "slimeballs family missing"), () -> assertTrue(items.contains(SharedItems.BACON.get()), "bacon missing"),
                () -> assertTrue(items.contains(SharedItems.MUDBRICK.get()), "mudbrick missing"), () -> assertTrue(items.contains(SharedItems.BUCKET_BLOOD.get()), "blood bucket missing"),
                () -> assertTrue(items.contains(SharedItems.MATERIALS_BOOK.get()), "materials book missing"),
                () -> assertTrue(items.contains(SharedBlocks.COBALT.get().asItem()), "metal storage block items missing"),
                () -> assertTrue(items.contains(SharedBlocks.GLOW.get().asItem()), "glow decorative missing"));
    }
}
