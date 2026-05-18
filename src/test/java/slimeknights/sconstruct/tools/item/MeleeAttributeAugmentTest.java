package slimeknights.sconstruct.tools.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.SConstruct;

/**
 * Pinned-constant tests for the per-class melee attribute augmentations. The {@code Item}
 * subclass constructors trip the {@code MappedRegistry} freeze in the bare-JVM test
 * environment, so we don't instantiate {@link LongswordItem} / {@link RapierItem} directly —
 * instead the {@code public static final} surface (modifier id, bonus magnitude) is pinned
 * here so a future refactor that drops or rescopes the per-class boost surfaces loudly.
 */
class MeleeAttributeAugmentTest {

    @Test
    void longswordReachConstantsPinned() {
        assertEquals(1.0D, LongswordItem.REACH_BONUS, "legacy 1.12 longsword baseline +1 reach");
        assertNotNull(LongswordItem.REACH_MODIFIER_ID, "reach modifier id must be initialised");
        assertEquals(SConstruct.MOD_ID, LongswordItem.REACH_MODIFIER_ID.getNamespace(), "reach modifier id must live in the mod namespace");
        assertEquals("longsword_reach", LongswordItem.REACH_MODIFIER_ID.getPath());
    }

    @Test
    void rapierAttackSpeedConstantsPinned() {
        assertEquals(1.0D, RapierItem.ATTACK_SPEED_BONUS, "legacy 1.12 rapier baseline +1 attack speed");
        assertNotNull(RapierItem.SPEED_MODIFIER_ID, "speed modifier id must be initialised");
        assertEquals(SConstruct.MOD_ID, RapierItem.SPEED_MODIFIER_ID.getNamespace());
        assertEquals("rapier_speed", RapierItem.SPEED_MODIFIER_ID.getPath());
    }

    @Test
    void cleaverBeheadChancePinned() {
        assertEquals(0.05F, CleaverItem.BASE_BEHEAD_CHANCE, "legacy 1.12 cleaver 1-in-20 behead baseline");
    }

    @Test
    void froeWoodArmorMultiplierPinned() {
        assertEquals(2.0F, FroeItem.WOOD_ARMOR_MULTIPLIER, "legacy 1.12 froe 2x damage vs wood/leather armour");
    }
}
