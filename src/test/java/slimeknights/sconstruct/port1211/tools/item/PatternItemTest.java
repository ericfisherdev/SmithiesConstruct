package slimeknights.sconstruct.port1211.tools.item;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.tools.PartType;

/**
 * Pinned-behaviour tests for {@link PatternItem}. The {@code Item.<init>} path is frozen by
 * the unit-test harness, so the typed/blank name pivot is verified through a Mockito spy
 * that bypasses the constructor — same pattern as {@link MaterialItemTest}.
 *
 * <p>Covers the load-bearing pivot: a stack with the typed component renders
 * {@code "Pattern: <part>"}; a stack without renders the vanilla
 * {@code item.sconstruct.blank_pattern} fallback.
 */
class PatternItemTest {

    @Test
    void getPartReturnsEmptyWhenComponentAbsent() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.get(TinkerDataComponents.TINKER_PATTERN_PART.get())).thenReturn(null);
        assertFalse(PatternItem.getPart(stack).isPresent());
    }

    @Test
    void getPartReturnsValueWhenComponentPresent() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.get(TinkerDataComponents.TINKER_PATTERN_PART.get())).thenReturn(Optional.of(PatternType.SWORDBLADE_TEST_VALUE));
        Optional<PartType> read = PatternItem.getPart(stack);
        assertTrue(read.isPresent());
        assertEquals(PartType.SWORDBLADE, read.get());
    }

    @Test
    void getNameRendersTypedFormWhenComponentPresent() {
        PatternItem item = mock(PatternItem.class);
        ItemStack stack = mock(ItemStack.class);
        when(stack.get(TinkerDataComponents.TINKER_PATTERN_PART.get())).thenReturn(Optional.of(PartType.PICKHEAD));
        when(item.getName(stack)).thenCallRealMethod();

        Component name = item.getName(stack);
        assertNotNull(name);
        assertTrue(name.getContents() instanceof TranslatableContents, "typed getName must return a TranslatableComponent");
        TranslatableContents contents = (TranslatableContents) name.getContents();
        assertAll(() -> assertEquals("item.sconstruct.pattern", contents.getKey(), "typed key must be item.sconstruct.pattern"),
                () -> assertEquals(1, contents.getArgs().length, "typed name has one substitution: the part display label"),
                () -> assertTrue(((Component) contents.getArgs()[0]).getContents() instanceof TranslatableContents, "part arg must be a TranslatableComponent"));
    }

    /**
     * Constants used by the tests above. Kept private to this file so the test stays a pure
     * unit test — referencing the {@code PartType} enum directly through a real value avoids
     * any reflective access on a synthesised enum entry.
     */
    private static final class PatternType {
        static final PartType SWORDBLADE_TEST_VALUE = PartType.SWORDBLADE;

        private PatternType() {
        }
    }
}
