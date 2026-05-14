package slimeknights.sconstruct.port1211.shared;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.level.material.MapColor;

import org.junit.jupiter.api.Test;

/**
 * Pinned-behaviour tests for the canonical {@link SharedMetals#ALL} driver list. Every Phase-2
 * provider iterates this list, so a silent drift here (wrong count, duplicate id, mis-flagged
 * tier) cascades into every downstream provider. The tests pin the contract before any
 * provider code lands.
 */
class SharedMetalsTest {

    private static final List<String> EXPECTED_IDS = List.of("cobalt", "ardite", "manyullyn", "knightslime", "pigiron", "silver", "copper", "tin", "zinc", "brass", "alubrass", "electrum", "steel",
            "lead", "nickel");

    private static final Set<String> DIAMOND_TIER_IDS = Set.of("cobalt", "ardite", "manyullyn");

    private static final Set<String> FICTIONAL_IDS = Set.of("cobalt", "ardite", "manyullyn", "knightslime", "pigiron", "alubrass");

    @Test
    void containsTheExpectedFifteenMetalsInDeclaredOrder() {
        // Order matters: providers stream ALL into deterministic outputs (lang files, recipe
        // ids). A reorder would shuffle generated artifacts and create review noise without
        // changing semantics — pin order so reorders are deliberate.
        List<String> actual = SharedMetals.ALL.stream().map(Metal::id).collect(Collectors.toList());
        assertEquals(EXPECTED_IDS, actual);
    }

    @Test
    void idsAreUnique() {
        // Duplicate ids would collide on every registry (block, item, fluid) and silently
        // overwrite the earlier registration — catch it at the driver layer instead.
        Set<String> seen = new HashSet<>();
        for (Metal metal : SharedMetals.ALL) {
            assertTrue(seen.add(metal.id()), "duplicate metal id: " + metal.id());
        }
    }

    @Test
    void onlyTheThreeNetherTierMetalsRequireDiamond() {
        // Per plan/03-shared-pulse: cobalt, ardite, manyullyn are NEEDS_DIAMOND_TOOL; every
        // other storage block is NEEDS_IRON_TOOL. Tier flags drive the block tag provider, so
        // a flip here would let players mine cobalt with iron (or worse, force diamond for
        // copper).
        for (Metal metal : SharedMetals.ALL) {
            boolean expected = DIAMOND_TIER_IDS.contains(metal.id());
            assertEquals(expected, metal.needsDiamond(), metal.id() + " needsDiamond flag");
        }
    }

    @Test
    void realWorldFlagMatchesTheLegacyClassification() {
        // The realWorld boolean drives the c:/sconstruct: namespace split for common item tags
        // (SMTCON-42). Pin the per-metal classification so a future refactor that flips a
        // fictional metal to realWorld (or vice versa) lights up this test before it ships a
        // misleading c:ingots/manyullyn or a missed c:ingots/copper.
        for (Metal metal : SharedMetals.ALL) {
            boolean expected = !FICTIONAL_IDS.contains(metal.id());
            assertEquals(expected, metal.realWorld(), metal.id() + " realWorld flag");
        }
    }

    @Test
    void everyFieldIsPopulated() {
        // Non-null and well-formed — the record constructor enforces this for new entries, but
        // a refactor that introduces a null literal would slip past the compiler.
        assertAll(SharedMetals.ALL.stream().map(metal -> () -> assertAll(() -> assertNotNull(metal.id(), "id"), () -> assertNotNull(metal.mapColor(), "mapColor for " + metal.id()),
                () -> assertTrue(metal.tintHex() >= 0 && metal.tintHex() <= 0xFFFFFF, "tintHex out of 24-bit range for " + metal.id()), () -> assertFalse(metal.id().isBlank(), "id blank"))));
    }

    @Test
    void recordRejectsInvalidConstruction() {
        // The record validates input so providers can trust the rows they iterate. Pin the
        // validations explicitly so a future refactor of the canonical constructor does not
        // silently weaken them.
        assertAll(() -> assertThrows(NullPointerException.class, () -> new Metal(null, MapColor.METAL, false, 0x000000, true)),
                () -> assertThrows(NullPointerException.class, () -> new Metal("ok", null, false, 0x000000, true)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Metal("", MapColor.METAL, false, 0x000000, true)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Metal("ok", MapColor.METAL, false, 0x1000000, true)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Metal("ok", MapColor.METAL, false, -1, true)));
    }
}
