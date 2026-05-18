package slimeknights.sconstruct.world.block;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.world.WorldBlocks;

/**
 * Pinned-behaviour tests for {@link SlimePlantSet}. Covers the canonical-constructor NPE
 * guard on every component and the {@link SlimePlantSet#all()} visit order. Construction
 * with bespoke {@code DeferredBlock}s is not possible in the unit-test JVM (the underlying
 * Block ctors freeze-fail), so the canonical-constructor null tests reuse the registered
 * holders from {@link WorldBlocks} as known-good values.
 */
class SlimePlantSetTest {

    @Test
    void canonicalConstructorRejectsNullComponents() {
        SlimePlantSet template = WorldBlocks.PLANTS_BLUE;
        assertThrows(NullPointerException.class, () -> new SlimePlantSet(null, template.grass(), template.leaves(), template.sapling()));
        assertThrows(NullPointerException.class, () -> new SlimePlantSet(template.dirt(), null, template.leaves(), template.sapling()));
        assertThrows(NullPointerException.class, () -> new SlimePlantSet(template.dirt(), template.grass(), null, template.sapling()));
        assertThrows(NullPointerException.class, () -> new SlimePlantSet(template.dirt(), template.grass(), template.leaves(), null));
    }

    @Test
    void allReturnsTheFourComponentsInDeclarationOrder() {
        SlimePlantSet set = WorldBlocks.PLANTS_PURPLE;
        assertEquals(List.of(set.dirt(), set.grass(), set.leaves(), set.sapling()), set.all());
    }

    @Test
    void colorComponentsCarryMatchingSlimeColorPerSet() {
        // Cross-check: each set's components must agree on the colour they were registered
        // with. A drifted ctor wiring would silently mix colours (e.g. blue dirt paired with
        // purple grass) and the spread-target check in SlimeGrassBlock would never fire.
        assertAll(WorldBlocks.PLANT_SETS.entrySet().stream().map(entry -> () -> {
            SlimeColor expected = entry.getKey();
            SlimePlantSet set = entry.getValue();
            assertEquals(expected, set.dirt().get().color(), "dirt for " + expected);
            assertEquals(expected, set.grass().get().color(), "grass for " + expected);
            assertEquals(expected, set.leaves().get().color(), "leaves for " + expected);
            assertEquals(expected, set.sapling().get().color(), "sapling for " + expected);
        }));
    }
}
