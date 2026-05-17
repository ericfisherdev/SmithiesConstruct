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

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredItem;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.gadgets.item.SlimeSlingItem;
import slimeknights.sconstruct.port1211.gadgets.item.ThrowballItem;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * Pinned-behaviour tests for {@link GadgetItems}. Covers the unit-testable SMTCON-132 and
 * SMTCON-133 acceptance criteria: four slimesling items and four throwball items registered,
 * each bound to its {@link SlimeColor}, each carrying the right stacking convention, and the
 * accept-all visitor reaching every gadget item — the unit proxy for "appears in the creative
 * inventory". The launch / impact-effect / dispenser behaviour needs a live server and is
 * covered by manual in-game testing per the tickets.
 */
class GadgetItemsTest {

    @Test
    void registersFourSlimeslings() {
        assertEquals(4, GadgetItems.SLINGS.size(), "one slimesling per SlimeColor");
    }

    @Test
    void registersFourThrowballs() {
        assertEquals(4, GadgetItems.THROWBALLS.size(), "one throwball per SlimeColor");
    }

    @Test
    void slingRegistrationPathsMatchTheColourKeys() {
        assertAll(() -> assertEquals("slimesling_blue", GadgetItems.SLING_BLUE.getId().getPath()), () -> assertEquals("slimesling_purple", GadgetItems.SLING_PURPLE.getId().getPath()),
                () -> assertEquals("slimesling_magma", GadgetItems.SLING_MAGMA.getId().getPath()), () -> assertEquals("slimesling_blood", GadgetItems.SLING_BLOOD.getId().getPath()));
    }

    @Test
    void throwballRegistrationPathsMatchTheColourKeys() {
        assertAll(() -> assertEquals("throwball_blue", GadgetItems.THROWBALL_BLUE.getId().getPath()), () -> assertEquals("throwball_purple", GadgetItems.THROWBALL_PURPLE.getId().getPath()),
                () -> assertEquals("throwball_magma", GadgetItems.THROWBALL_MAGMA.getId().getPath()), () -> assertEquals("throwball_blood", GadgetItems.THROWBALL_BLOOD.getId().getPath()));
    }

    @Test
    void everySlingBindsToItsColour() {
        // Pin the colour mapping per item — the colour drives the launch profile and the
        // on-release side effect, so a swapped binding would silently mis-behave.
        assertAll(() -> assertEquals(SlimeColor.BLUE, GadgetItems.SLING_BLUE.get().color()), () -> assertEquals(SlimeColor.PURPLE, GadgetItems.SLING_PURPLE.get().color()),
                () -> assertEquals(SlimeColor.MAGMA, GadgetItems.SLING_MAGMA.get().color()), () -> assertEquals(SlimeColor.BLOOD, GadgetItems.SLING_BLOOD.get().color()));
    }

    @Test
    void everyThrowballBindsToItsColour() {
        // The colour drives the on-impact effect the ThrowballEntity applies.
        assertAll(() -> assertEquals(SlimeColor.BLUE, GadgetItems.THROWBALL_BLUE.get().color()), () -> assertEquals(SlimeColor.PURPLE, GadgetItems.THROWBALL_PURPLE.get().color()),
                () -> assertEquals(SlimeColor.MAGMA, GadgetItems.THROWBALL_MAGMA.get().color()), () -> assertEquals(SlimeColor.BLOOD, GadgetItems.THROWBALL_BLOOD.get().color()));
    }

    @Test
    void everySlingIsADurableSingleStackItem() {
        // Durability decrements per use (AC), so each sling must carry a durability budget and
        // the single-stack convention a charged tool uses.
        for (DeferredItem<SlimeSlingItem> sling : GadgetItems.SLINGS) {
            ItemStack stack = new ItemStack(sling.get());
            assertAll(() -> assertTrue(stack.getMaxDamage() > 0, sling.getId() + " must be damageable"), () -> assertEquals(1, stack.getMaxStackSize(), sling.getId() + " must not stack"));
        }
    }

    @Test
    void everySlingChargesWithTheBowUseAnimation() {
        // The charge pose is the visible "winding up" animation the AC calls for.
        for (DeferredItem<SlimeSlingItem> sling : GadgetItems.SLINGS) {
            ItemStack stack = new ItemStack(sling.get());
            assertEquals(UseAnim.BOW, sling.get().getUseAnimation(stack), sling.getId() + " charges with the BOW animation");
        }
    }

    @Test
    void everyThrowballStacksAndIsDispenserCapable() {
        // Throwballs stack like snowballs, and implement ProjectileItem so a dispenser can fire
        // them (the dispenser behaviour is wired by GadgetDispenserBehaviors).
        for (DeferredItem<ThrowballItem> throwball : GadgetItems.THROWBALLS) {
            ItemStack stack = new ItemStack(throwball.get());
            assertAll(() -> assertEquals(ThrowballItem.STACK_SIZE, stack.getMaxStackSize(), throwball.getId() + " stacks like a snowball"),
                    () -> assertTrue(throwball.get() instanceof ProjectileItem, throwball.getId() + " must be a ProjectileItem for dispenser support"));
        }
    }

    @Test
    void registersThePiggybackItem() {
        assertAll(() -> assertEquals("piggyback", GadgetItems.PIGGYBACK.getId().getPath()),
                () -> assertEquals(1, new ItemStack(GadgetItems.PIGGYBACK.get()).getMaxStackSize(), "the piggyback item does not stack"));
    }

    @Test
    void acceptAllVisitsEveryGadgetItemExactlyOnce() {
        // The BuildCreativeModeTabContentsEvent listener delegates here, so verifying coverage
        // is the unit-level proxy for "every gadget item appears in the creative inventory".
        List<ItemLike> visited = new ArrayList<>();
        GadgetItems.acceptAll(visited::add);
        Set<ItemLike> canonical = GadgetItems.ALL.stream().map(DeferredItem::get).collect(Collectors.toSet());
        assertAll(() -> assertEquals(9, visited.size(), "visitor must reach every gadget item exactly once"), () -> assertEquals(9L, visited.stream().distinct().count(), "no duplicates"),
                () -> assertEquals(canonical, new HashSet<>(visited), "visited set must equal the canonical roster"));
    }

    @Test
    void rostersAreImmutable() {
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> GadgetItems.SLINGS.add(null)),
                () -> assertThrows(UnsupportedOperationException.class, () -> GadgetItems.THROWBALLS.add(null)),
                () -> assertThrows(UnsupportedOperationException.class, () -> GadgetItems.ALL.add(null)));
    }

    @Test
    void allRosterCombinesEveryGadgetItem() {
        List<DeferredItem<? extends Item>> all = GadgetItems.ALL;
        assertEquals(9, all.size(), "the combined roster is every sling, throwball, and the piggyback item");
        assertAll(() -> assertTrue(all.containsAll(GadgetItems.SLINGS), "every sling is in the combined roster"),
                () -> assertTrue(all.containsAll(GadgetItems.THROWBALLS), "every throwball is in the combined roster"),
                () -> assertTrue(all.contains(GadgetItems.PIGGYBACK), "the piggyback item is in the combined roster"));
    }
}
