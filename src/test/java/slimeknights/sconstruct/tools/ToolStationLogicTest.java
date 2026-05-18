package slimeknights.sconstruct.tools;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.common.data.TinkerDataComponents;
import slimeknights.sconstruct.tools.item.MaterialItem;

/**
 * Pinned-behaviour tests for {@link ToolStationLogic#findMatch}. The matching rule (positional
 * part-type check + present-{@code PART_MATERIAL} check + part-count parity) is the
 * load-bearing piece of the Tool Station / Tool Forge build path and is exposed as a pure
 * static helper for exactly this reason — {@link ToolStationLogic#tryBuild} layers a {@code
 * new ItemStack(tool)} on top of {@code findMatch}, which the JUnit harness can't reach (the
 * item registry freezes before tests run).
 *
 * <p>Inputs are built as Mockito-mocked {@link ItemStack} + {@link MaterialItem} pairs: the
 * stack's {@link ItemStack#getItem} returns the part item, the part's
 * {@link MaterialItem#partType} returns the slot identity, and the stack's
 * {@link ItemStack#get(DataComponentType)} returns the {@code PART_MATERIAL} resource location
 * (or null for the un-stamped case). The test covers five matching scenarios per AC: full
 * 3-part pickaxe match, wrong slot order, missing {@code PART_MATERIAL}, advanced (4-part)
 * definition not in the basic roster, advanced definition in the advanced roster.
 */
class ToolStationLogicTest {

    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("sconstruct", "iron");
    private static final ResourceLocation WOOD = ResourceLocation.fromNamespaceAndPath("sconstruct", "wood");
    private static final DataComponentType<ResourceLocation> PART_MATERIAL = TinkerDataComponents.PART_MATERIAL.get();

    @Test
    void findMatchReturnsThePickaxeWhenInputsMatchPickaxePartRoster() {
        // ToolDefinition.PICKAXE = [HANDLE, PICKHEAD, BINDING].
        List<ItemStack> inputs = List.of(part(PartType.HANDLE, WOOD), part(PartType.PICKHEAD, IRON), part(PartType.BINDING, IRON), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        Optional<ToolStationLogic.Match> match = ToolStationLogic.findMatch(inputs, Stream.of(ToolDefinition.PICKAXE));
        assertTrue(match.isPresent(), "pickaxe inputs must match the pickaxe definition");
        assertAll(() -> assertSame(ToolDefinition.PICKAXE, match.get().definition()), () -> assertEquals(List.of(WOOD, IRON, IRON), match.get().materials()));
    }

    @Test
    void findMatchRejectsInputsInWrongPartOrder() {
        // Pickaxe order is [HANDLE, PICKHEAD, BINDING] — swap slots 0 and 1 (PICKHEAD first,
        // then HANDLE) and the matcher must reject. The materials are valid; only the order is
        // wrong. A silent match here would let a player assemble a tool with a head in the
        // handle slot, which downstream stat code would misread.
        List<ItemStack> inputs = List.of(part(PartType.PICKHEAD, IRON), part(PartType.HANDLE, WOOD), part(PartType.BINDING, IRON), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        Optional<ToolStationLogic.Match> match = ToolStationLogic.findMatch(inputs, Stream.of(ToolDefinition.PICKAXE));
        assertTrue(match.isEmpty(), "wrong-order inputs must not match the pickaxe definition");
    }

    @Test
    void findMatchRejectsInputsWithoutPartMaterialComponent() {
        // Three correctly-ordered pickaxe parts but the head is missing its PART_MATERIAL —
        // the build path must reject rather than falling back to MaterialItem.DEFAULT_MATERIAL.
        // A silent fallback would build a wood-headed pickaxe out of an un-bound part stack.
        ItemStack head = mockStack(PartType.PICKHEAD, null);
        List<ItemStack> inputs = List.of(part(PartType.HANDLE, WOOD), head, part(PartType.BINDING, IRON), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        Optional<ToolStationLogic.Match> match = ToolStationLogic.findMatch(inputs, Stream.of(ToolDefinition.PICKAXE));
        assertTrue(match.isEmpty(), "missing PART_MATERIAL on any input must reject the build");
    }

    @Test
    void findMatchRejectsAdvancedDefinitionWhenOnlyBasicRosterIsPassed() {
        // Hammer is a 4-part advanced tool (toughhandle + hammerhead + largeplate + largeplate).
        // When the candidate stream only contains the basic ALL_BASIC roster, the hammer's
        // inputs find no match. Simulates the base Tool Station refusing a hammer build.
        List<ItemStack> inputs = List.of(part(PartType.TOUGHHANDLE, IRON), part(PartType.HAMMERHEAD, IRON), part(PartType.LARGEPLATE, IRON), part(PartType.LARGEPLATE, IRON), ItemStack.EMPTY,
                ItemStack.EMPTY);
        Optional<ToolStationLogic.Match> match = ToolStationLogic.findMatch(inputs, ToolDefinition.ALL_BASIC.stream());
        assertTrue(match.isEmpty(), "hammer must not match the basic roster (3-part definitions only)");
    }

    @Test
    void findMatchAcceptsAdvancedDefinitionWhenAdvancedRosterIsPassed() {
        // Same hammer inputs match when the advanced roster is passed in. Simulates the Tool
        // Forge accepting a hammer build.
        List<ItemStack> inputs = List.of(part(PartType.TOUGHHANDLE, IRON), part(PartType.HAMMERHEAD, IRON), part(PartType.LARGEPLATE, IRON), part(PartType.LARGEPLATE, IRON), ItemStack.EMPTY,
                ItemStack.EMPTY);
        Optional<ToolStationLogic.Match> match = ToolStationLogic.findMatch(inputs, ToolDefinition.ALL_ADVANCED.stream());
        assertTrue(match.isPresent(), "hammer must match the advanced roster");
        assertSame(ToolDefinition.HAMMER, match.get().definition());
    }

    @Test
    void findMatchReturnsEmptyWhenAllInputsAreEmpty() {
        // Defensive: an entirely empty input grid (no parts placed yet) must produce empty
        // rather than picking the first 0-part definition. ToolDefinition rejects 0-part
        // construction in its canonical constructor, so this is also a sanity check that the
        // matcher doesn't iterate a fabricated empty definition.
        List<ItemStack> inputs = List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        Optional<ToolStationLogic.Match> match = ToolStationLogic.findMatch(inputs, ToolDefinition.ALL_ADVANCED.stream());
        assertTrue(match.isEmpty(), "empty input grid must not match any definition");
    }

    /** Build a populated mock part stack for the given part type + material. */
    private static ItemStack part(PartType type, ResourceLocation material) {
        return mockStack(type, material);
    }

    /**
     * Mock an {@link ItemStack} whose item is a {@link MaterialItem} of the supplied
     * {@link PartType} and whose {@code PART_MATERIAL} component reads as the supplied id (or
     * {@code null} for the un-stamped variant). The stack reports {@code isEmpty() == false}
     * so {@link ToolStationLogic#trimmedLength} treats it as populated.
     */
    private static ItemStack mockStack(PartType type, ResourceLocation material) {
        MaterialItem partItem = mock(MaterialItem.class);
        when(partItem.partType()).thenReturn(type);
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.getItem()).thenReturn(partItem);
        when(stack.get(PART_MATERIAL)).thenReturn(material);
        return stack;
    }
}
