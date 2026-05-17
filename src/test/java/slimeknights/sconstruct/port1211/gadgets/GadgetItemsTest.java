package slimeknights.sconstruct.port1211.gadgets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredItem;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.gadgets.item.SlimeSlingItem;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * Pinned-behaviour tests for {@link GadgetItems}. Covers the SMTCON-132 acceptance criteria
 * that are unit-testable without a live server: four slimesling items registered, each bound
 * to its {@link SlimeColor}, each carrying the shared durability budget and single-stack
 * convention, and the accept-all visitor reaching every sling — the unit proxy for "appears in
 * the creative inventory", since the {@code BuildCreativeModeTabContentsEvent} listener routes
 * through the same visitor. The launch / fire / heal behaviour needs a live {@code Player} and
 * is covered by manual in-game testing per the ticket.
 */
class GadgetItemsTest {

    @Test
    void registersFourSlimeslings() {
        assertEquals(4, GadgetItems.ALL.size(), "one slimesling per SlimeColor");
    }

    @Test
    void registrationPathsMatchTheColourKeys() {
        assertAll(() -> assertEquals("slimesling_blue", GadgetItems.SLING_BLUE.getId().getPath()), () -> assertEquals("slimesling_purple", GadgetItems.SLING_PURPLE.getId().getPath()),
                () -> assertEquals("slimesling_magma", GadgetItems.SLING_MAGMA.getId().getPath()), () -> assertEquals("slimesling_blood", GadgetItems.SLING_BLOOD.getId().getPath()));
    }

    @Test
    void everySlingBindsToItsColour() {
        // Pin the colour mapping per item — the colour drives the launch profile and the
        // on-release side effect, so a swapped binding would silently mis-behave.
        assertAll(() -> assertEquals(SlimeColor.BLUE, GadgetItems.SLING_BLUE.get().color()), () -> assertEquals(SlimeColor.PURPLE, GadgetItems.SLING_PURPLE.get().color()),
                () -> assertEquals(SlimeColor.MAGMA, GadgetItems.SLING_MAGMA.get().color()), () -> assertEquals(SlimeColor.BLOOD, GadgetItems.SLING_BLOOD.get().color()));
    }

    @Test
    void everySlingIsADurableSingleStackItem() {
        // Durability decrements per use (AC), so each sling must carry a durability budget and
        // the single-stack convention a charged tool uses.
        for (DeferredItem<SlimeSlingItem> sling : GadgetItems.ALL) {
            ItemStack stack = new ItemStack(sling.get());
            assertAll(() -> assertTrue(stack.getMaxDamage() > 0, sling.getId() + " must be damageable"), () -> assertEquals(1, stack.getMaxStackSize(), sling.getId() + " must not stack"));
        }
    }

    @Test
    void everySlingChargesWithTheBowUseAnimation() {
        // The charge pose is the visible "winding up" animation the AC calls for.
        for (DeferredItem<SlimeSlingItem> sling : GadgetItems.ALL) {
            ItemStack stack = new ItemStack(sling.get());
            assertEquals(UseAnim.BOW, sling.get().getUseAnimation(stack), sling.getId() + " charges with the BOW animation");
        }
    }

    @Test
    void acceptAllVisitsEverySlingExactlyOnce() {
        // The BuildCreativeModeTabContentsEvent listener delegates here, so verifying coverage
        // is the unit-level proxy for "every sling appears in the creative inventory".
        List<ItemLike> visited = new ArrayList<>();
        GadgetItems.acceptAll(visited::add);
        Set<ItemLike> canonical = GadgetItems.ALL.stream().map(DeferredItem::get).collect(Collectors.toSet());
        assertAll(() -> assertEquals(4, visited.size(), "visitor must reach every sling exactly once"), () -> assertEquals(4L, visited.stream().distinct().count(), "no duplicates"),
                () -> assertEquals(canonical, new HashSet<>(visited), "visited set must equal the canonical ALL roster"));
    }

    @Test
    void allRosterIsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> GadgetItems.ALL.add(null));
    }
}
