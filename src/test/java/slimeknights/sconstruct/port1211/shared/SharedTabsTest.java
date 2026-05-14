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
        // + slimeballs (4) + 3 miscs (mudbrick, bacon, blood bucket).
        int expectedSize = SharedBlocks.METAL_BLOCKS.size() + 3 + SharedItems.INGOTS.size() + SharedItems.NUGGETS.size() + SharedItems.SLIMEBALLS.size() + 3;
        assertEquals(expectedSize, visited.size(), "wrong number of items accepted into GENERAL");

        // No duplicates per AC — collecting into a Set of identity-keyed items must match the
        // list size. Use Item handles (ItemLike.asItem()) so block/blockitem identity collapses.
        Set<net.minecraft.world.item.Item> uniqueItems = new HashSet<>();
        for (ItemLike like : visited) {
            assertTrue(uniqueItems.add(like.asItem()), "duplicate item accepted: " + like.asItem());
        }
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
                () -> assertTrue(items.contains(SharedBlocks.COBALT.get().asItem()), "metal storage block items missing"),
                () -> assertTrue(items.contains(SharedBlocks.GLOW.get().asItem()), "glow decorative missing"));
    }
}
