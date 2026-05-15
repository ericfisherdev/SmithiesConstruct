package slimeknights.sconstruct.port1211.tools;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import org.junit.jupiter.api.Test;

/**
 * Pinned-behaviour tests for {@link ToolDefinition}. Locks in the legacy-accurate part roster
 * for every shipped tool constant, the {@link #DEFAULT_MODIFIER_SLOTS} baseline, and the
 * defensive-copy contract — if any of these silently change, every tool's craft path bends.
 */
class ToolDefinitionTest {

    private static final List<ToolDefinition> ALL = List.of(ToolDefinition.PICKAXE, ToolDefinition.SHOVEL, ToolDefinition.HATCHET, ToolDefinition.MATTOCK, ToolDefinition.HAMMER,
            ToolDefinition.EXCAVATOR, ToolDefinition.LUMBER_AXE, ToolDefinition.SCYTHE, ToolDefinition.BROADSWORD, ToolDefinition.SHORTBOW, ToolDefinition.CROSSBOW, ToolDefinition.ARROW);

    @Test
    void shipsAtLeastEightToolConstants() {
        // Acceptance criterion (SMTCON-73): "ToolDefinition + at least 8 tool constants
        // compile". We ship 10; the floor is pinned so a future cleanup that drops a constant
        // surfaces the AC regression rather than silently shrinking the roster.
        assertTrue(ALL.size() >= 8, "ticket spec: at least 8 tool constants — have " + ALL.size());
    }

    @Test
    void everyConstantHasAUniqueId() {
        long unique = ALL.stream().map(ToolDefinition::id).distinct().count();
        assertEquals(ALL.size(), unique, "tool ids must be unique — collisions would break recipe / registry paths");
    }

    @Test
    void everyConstantStartsAtThreeBaseModifierSlots() {
        // Legacy ToolCore.DEFAULT_MODIFIERS = 3. Pin so a future cap-tier ticket can't silently
        // raise the floor and inflate every existing world's modifier capacity.
        assertEquals(3, ToolDefinition.DEFAULT_MODIFIER_SLOTS);
        for (ToolDefinition def : ALL) {
            assertEquals(3, def.baseModifierSlots(), () -> def.id() + " should start at 3 free modifier slots");
        }
    }

    @Test
    void partRosterMatchesLegacyForEveryShippedTool() {
        // Audit anchors: legacy PartMaterialType ordering in TinkerHarvestTools /
        // TinkerMeleeWeapons / TinkerRangedWeapons. Positional order matters — the tool-
        // building UI uses index to drive slot rendering.
        assertAll(() -> assertEquals(List.of(PartType.HANDLE, PartType.PICKHEAD, PartType.BINDING), ToolDefinition.PICKAXE.parts()),
                () -> assertEquals(List.of(PartType.HANDLE, PartType.SHOVELHEAD, PartType.BINDING), ToolDefinition.SHOVEL.parts()),
                () -> assertEquals(List.of(PartType.HANDLE, PartType.AXEHEAD, PartType.BINDING), ToolDefinition.HATCHET.parts()),
                () -> assertEquals(List.of(PartType.HANDLE, PartType.AXEHEAD, PartType.SHOVELHEAD), ToolDefinition.MATTOCK.parts()),
                () -> assertEquals(List.of(PartType.TOUGHHANDLE, PartType.HAMMERHEAD, PartType.LARGEPLATE, PartType.LARGEPLATE), ToolDefinition.HAMMER.parts()),
                () -> assertEquals(List.of(PartType.TOUGHHANDLE, PartType.BROADAXEHEAD, PartType.LARGEPLATE, PartType.TOUGHBINDING), ToolDefinition.LUMBER_AXE.parts()),
                () -> assertEquals(List.of(PartType.TOUGHHANDLE, PartType.SHOVELHEAD, PartType.LARGEPLATE, PartType.TOUGHBINDING), ToolDefinition.EXCAVATOR.parts()),
                () -> assertEquals(List.of(PartType.TOUGHHANDLE, PartType.BROADBLADE, PartType.TOUGHBINDING, PartType.TOUGHBINDING), ToolDefinition.SCYTHE.parts()),
                () -> assertEquals(List.of(PartType.HANDLE, PartType.SWORDBLADE, PartType.WIDEGUARD), ToolDefinition.BROADSWORD.parts()),
                () -> assertEquals(List.of(PartType.BOWLIMB, PartType.BOWLIMB, PartType.BOWSTRING), ToolDefinition.SHORTBOW.parts()),
                () -> assertEquals(List.of(PartType.TOUGHHANDLE, PartType.BOWLIMB, PartType.TOUGHBINDING, PartType.BOWSTRING), ToolDefinition.CROSSBOW.parts()),
                () -> assertEquals(List.of(PartType.ARROWSHAFT, PartType.ARROW_HEAD, PartType.FLETCHING), ToolDefinition.ARROW.parts()));
    }

    @Test
    void miningTagsMatchVanillaTagsForDigTools() {
        assertAll(() -> assertSame(BlockTags.MINEABLE_WITH_PICKAXE, ToolDefinition.PICKAXE.miningTag()), () -> assertSame(BlockTags.MINEABLE_WITH_SHOVEL, ToolDefinition.SHOVEL.miningTag()),
                () -> assertSame(BlockTags.MINEABLE_WITH_AXE, ToolDefinition.HATCHET.miningTag()), () -> assertSame(BlockTags.MINEABLE_WITH_PICKAXE, ToolDefinition.HAMMER.miningTag()),
                () -> assertSame(BlockTags.MINEABLE_WITH_AXE, ToolDefinition.LUMBER_AXE.miningTag()), () -> assertSame(BlockTags.MINEABLE_WITH_SHOVEL, ToolDefinition.EXCAVATOR.miningTag()));
    }

    @Test
    void nonDigToolsRecordNullMiningTag() {
        // Mattock has a custom legacy harvest-class; swords / bows / arrows aren't bound to a
        // mining tag at all. Null is the documented sentinel — pinned so a refactor doesn't
        // accidentally substitute a vanilla tag and silently change which blocks the tool
        // mines.
        assertAll(() -> assertNull(ToolDefinition.MATTOCK.miningTag()), () -> assertNull(ToolDefinition.BROADSWORD.miningTag()), () -> assertNull(ToolDefinition.SCYTHE.miningTag()),
                () -> assertNull(ToolDefinition.SHORTBOW.miningTag()), () -> assertNull(ToolDefinition.CROSSBOW.miningTag()), () -> assertNull(ToolDefinition.ARROW.miningTag()));
    }

    @Test
    void digToolsExposeTheirVanillaAbilitySet() {
        assertAll(() -> assertEquals(ItemAbilities.DEFAULT_PICKAXE_ACTIONS, ToolDefinition.PICKAXE.abilities()),
                () -> assertEquals(ItemAbilities.DEFAULT_SHOVEL_ACTIONS, ToolDefinition.SHOVEL.abilities()), () -> assertEquals(ItemAbilities.DEFAULT_AXE_ACTIONS, ToolDefinition.HATCHET.abilities()),
                () -> assertEquals(ItemAbilities.DEFAULT_PICKAXE_ACTIONS, ToolDefinition.HAMMER.abilities()),
                () -> assertEquals(ItemAbilities.DEFAULT_AXE_ACTIONS, ToolDefinition.LUMBER_AXE.abilities()),
                () -> assertEquals(ItemAbilities.DEFAULT_SHOVEL_ACTIONS, ToolDefinition.EXCAVATOR.abilities()),
                () -> assertEquals(ItemAbilities.DEFAULT_SWORD_ACTIONS, ToolDefinition.SCYTHE.abilities()),
                () -> assertEquals(ItemAbilities.DEFAULT_SWORD_ACTIONS, ToolDefinition.BROADSWORD.abilities()));
    }

    @Test
    void mattockUnionsAxeShovelAndHoeAbilities() {
        // Legacy "mattock" harvest-class behaved as axe+shovel+hoe — pin so the union doesn't
        // silently lose HOE_TILL or AXE_STRIP and quietly break recipe-less in-world actions.
        Set<?> a = ToolDefinition.MATTOCK.abilities();
        assertTrue(a.contains(ItemAbilities.AXE_DIG));
        assertTrue(a.contains(ItemAbilities.SHOVEL_DIG));
        assertTrue(a.contains(ItemAbilities.HOE_DIG));
        assertTrue(a.contains(ItemAbilities.HOE_TILL));
        assertTrue(a.contains(ItemAbilities.SHOVEL_FLATTEN));
        assertTrue(a.contains(ItemAbilities.AXE_STRIP));
    }

    @Test
    void rangedConstantsShipEmptyAbilitySets() {
        // Bows and arrows have no NeoForge ItemAbility — pinned empty so a future contributor
        // doesn't bolt SWORD_DIG onto bows to "just make right-click work".
        assertAll(() -> assertTrue(ToolDefinition.SHORTBOW.abilities().isEmpty()), () -> assertTrue(ToolDefinition.CROSSBOW.abilities().isEmpty()),
                () -> assertTrue(ToolDefinition.ARROW.abilities().isEmpty()));
    }

    @Test
    void getPartCountAndGetPartSlotAgreeWithPartsList() {
        for (ToolDefinition def : ALL) {
            assertEquals(def.parts().size(), def.getPartCount(), () -> def.id() + " getPartCount() must equal parts.size()");
            for (int i = 0; i < def.getPartCount(); i++) {
                final int idx = i;
                assertSame(def.parts().get(idx), def.getPartSlot(idx), () -> def.id() + " slot " + idx);
            }
        }
    }

    @Test
    void getPartSlotRejectsOutOfRangeIndex() {
        // Index errors must be loud — a quiet null would propagate into attribute resolution
        // and crash at a less actionable site.
        assertThrows(IndexOutOfBoundsException.class, () -> ToolDefinition.PICKAXE.getPartSlot(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> ToolDefinition.PICKAXE.getPartSlot(ToolDefinition.PICKAXE.getPartCount()));
    }

    @Test
    void constructorCopiesPartsAndAbilitiesDefensively() {
        // The caller's collection identity must not be retained — otherwise an alias could
        // mutate the tool definition after construction (e.g. swap a part slot or strip an
        // ability) and silently corrupt every consumer.
        List<PartType> mutableParts = new ArrayList<>(List.of(PartType.HANDLE, PartType.PICKHEAD, PartType.BINDING));
        Set<ItemAbility> mutableAbilities = new HashSet<>();
        mutableAbilities.add(ItemAbilities.PICKAXE_DIG);
        ToolDefinition def = new ToolDefinition("test_copy", mutableParts, BlockTags.MINEABLE_WITH_PICKAXE, mutableAbilities, 3);
        mutableParts.clear();
        mutableAbilities.clear();
        assertEquals(3, def.getPartCount(), "post-construction caller mutation must not shrink parts");
        assertFalse(def.abilities().isEmpty(), "post-construction caller mutation must not strip abilities");
        // The returned list/set must also be unmodifiable so a downstream reader can't mutate
        // shared state either.
        assertThrows(UnsupportedOperationException.class, () -> def.parts().add(PartType.HANDLE));
        assertThrows(UnsupportedOperationException.class, () -> def.abilities().clear());
    }

    @Test
    void constructorRejectsEmptyPartsListAndNegativeModifierSlots() {
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> new ToolDefinition("empty", Collections.emptyList(), null, Collections.emptySet(), 3)),
                () -> assertThrows(IllegalArgumentException.class, () -> new ToolDefinition("negative", List.of(PartType.HANDLE), null, Collections.emptySet(), -1)),
                () -> assertThrows(NullPointerException.class, () -> new ToolDefinition(null, List.of(PartType.HANDLE), null, Collections.emptySet(), 3)));
    }

    @Test
    void everyConstantIsAccessibleAsAStaticReference() {
        // Lightweight smoke check that the static initialisers don't NPE on classload (e.g.
        // because BlockTags isn't bootstrapped in unit tests). If this test passes the whole
        // ALL list is sound.
        for (ToolDefinition def : ALL) {
            assertNotNull(def, "constant must initialise");
            assertNotNull(def.id());
            assertNotNull(def.parts());
            assertNotNull(def.abilities());
        }
    }
}
