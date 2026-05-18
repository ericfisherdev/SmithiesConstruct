package slimeknights.sconstruct.gadgets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredItem;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.gadgets.item.GlowBallItem;
import slimeknights.sconstruct.gadgets.item.SlimeArmorItem;
import slimeknights.sconstruct.gadgets.item.SlimeSlingItem;
import slimeknights.sconstruct.gadgets.item.ThrowballItem;
import slimeknights.sconstruct.world.block.SlimeColor;

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
    void registersTheGlowBall() {
        assertAll(() -> assertEquals("glow_ball", GadgetItems.GLOW_BALL.getId().getPath()),
                () -> assertEquals(GlowBallItem.STACK_SIZE, new ItemStack(GadgetItems.GLOW_BALL.get()).getMaxStackSize(), "the glow ball stacks like a snowball"),
                () -> assertTrue(GadgetItems.GLOW_BALL.get() instanceof ProjectileItem, "the glow ball must be a ProjectileItem for dispenser support"));
    }

    @Test
    void registersTheFourSlimeArmorPieces() {
        assertEquals(4, GadgetItems.ARMOR.size(), "one slime armor item per armor slot");
        assertAll(() -> assertEquals("slime_helmet", GadgetItems.SLIME_HELMET.getId().getPath()), () -> assertEquals("slime_chestplate", GadgetItems.SLIME_CHESTPLATE.getId().getPath()),
                () -> assertEquals("slime_leggings", GadgetItems.SLIME_LEGGINGS.getId().getPath()), () -> assertEquals("slime_boots", GadgetItems.SLIME_BOOTS.getId().getPath()));
    }

    @Test
    void everySlimeArmorPieceIsADurableArmorItem() {
        // Durable so anvil repair with a slimeball is meaningful; each must be an ArmorItem so
        // it equips into an armor slot.
        for (DeferredItem<SlimeArmorItem> piece : GadgetItems.ARMOR) {
            ItemStack stack = new ItemStack(piece.get());
            assertAll(() -> assertTrue(stack.getMaxDamage() > 0, piece.getId() + " must be damageable"), () -> assertTrue(piece.get() instanceof ArmorItem, piece.getId() + " must be an ArmorItem"));
        }
    }

    @Test
    void slimeArmorPiecesCoverEveryArmorSlot() {
        // Pin the slot mapping so a swapped registration cannot put two pieces in one slot.
        assertAll(() -> assertEquals(ArmorItem.Type.HELMET, GadgetItems.SLIME_HELMET.get().getType()), () -> assertEquals(ArmorItem.Type.CHESTPLATE, GadgetItems.SLIME_CHESTPLATE.get().getType()),
                () -> assertEquals(ArmorItem.Type.LEGGINGS, GadgetItems.SLIME_LEGGINGS.get().getType()), () -> assertEquals(ArmorItem.Type.BOOTS, GadgetItems.SLIME_BOOTS.get().getType()));
    }

    @Test
    void registersTheWitherHead() {
        assertEquals("wither_head", GadgetItems.WITHER_HEAD.getId().getPath(), "the wither head registers under its expected key");
    }

    @Test
    void acceptAllVisitsEveryGadgetItemExactlyOnce() {
        // The BuildCreativeModeTabContentsEvent listener delegates here, so verifying coverage
        // is the unit-level proxy for "every gadget item appears in the creative inventory".
        List<ItemLike> visited = new ArrayList<>();
        GadgetItems.acceptAll(visited::add);
        Set<ItemLike> canonical = GadgetItems.ALL.stream().map(DeferredItem::get).collect(Collectors.toSet());
        assertAll(() -> assertEquals(15, visited.size(), "visitor must reach every gadget item exactly once"), () -> assertEquals(15L, visited.stream().distinct().count(), "no duplicates"),
                () -> assertEquals(canonical, new HashSet<>(visited), "visited set must equal the canonical roster"));
    }

    @Test
    void rostersAreImmutable() {
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> GadgetItems.SLINGS.add(null)),
                () -> assertThrows(UnsupportedOperationException.class, () -> GadgetItems.THROWBALLS.add(null)),
                () -> assertThrows(UnsupportedOperationException.class, () -> GadgetItems.ARMOR.add(null)), () -> assertThrows(UnsupportedOperationException.class, () -> GadgetItems.ALL.add(null)));
    }

    @Test
    void allRosterCombinesEveryGadgetItem() {
        List<DeferredItem<? extends Item>> all = GadgetItems.ALL;
        assertEquals(15, all.size(), "the combined roster is every sling, throwball, piggyback, glow ball, wither head, and armor piece");
        assertAll(() -> assertTrue(all.containsAll(GadgetItems.SLINGS), "every sling is in the combined roster"),
                () -> assertTrue(all.containsAll(GadgetItems.THROWBALLS), "every throwball is in the combined roster"),
                () -> assertTrue(all.contains(GadgetItems.PIGGYBACK), "the piggyback item is in the combined roster"),
                () -> assertTrue(all.contains(GadgetItems.GLOW_BALL), "the glow ball is in the combined roster"),
                () -> assertTrue(all.contains(GadgetItems.WITHER_HEAD), "the wither head is in the combined roster"),
                () -> assertTrue(all.containsAll(GadgetItems.ARMOR), "every armor piece is in the combined roster"));
    }
}
