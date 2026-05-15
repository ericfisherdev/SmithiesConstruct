package slimeknights.sconstruct.port1211.tools;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.common.data.ToolBroken;
import slimeknights.sconstruct.port1211.common.data.ToolMaterials;
import slimeknights.sconstruct.port1211.common.data.ToolModifiers;
import slimeknights.sconstruct.port1211.common.data.ToolStats;

/**
 * Pinned-behaviour tests for {@link ToolHelper}. The class is a façade over five
 * {@code DataComponentType}s — covers every accessor's empty-stack / missing-component default
 * (AC2), every mutator's "always set a fresh immutable record" contract (AC3), and the
 * rebuild-stats side effect on materials / modifier writes.
 */
class ToolHelperTest {

    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("sconstruct", "iron");
    private static final ResourceLocation WOOD = ResourceLocation.fromNamespaceAndPath("sconstruct", "wood");
    private static final ResourceLocation SHARPNESS = ResourceLocation.fromNamespaceAndPath("sconstruct", "sharpness");
    private static final ResourceLocation HASTE = ResourceLocation.fromNamespaceAndPath("sconstruct", "haste");

    private static final DataComponentType<ToolMaterials> MATERIALS = TinkerDataComponents.TOOL_MATERIALS.get();
    private static final DataComponentType<ToolModifiers> MODIFIERS = TinkerDataComponents.TOOL_MODIFIERS.get();
    private static final DataComponentType<ToolStats> STATS = TinkerDataComponents.TOOL_STATS.get();
    private static final DataComponentType<ToolBroken> BROKEN = TinkerDataComponents.TOOL_BROKEN.get();

    // ----------------------------------------------------------------------------- accessors

    @Test
    void accessorsReturnDefaultsForEmptyStacks() {
        // AC2: every accessor must absorb the empty-stack guard so callers can pipeline through
        // ItemStacks without an extra isEmpty() check.
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(true);
        assertAll(() -> assertEquals(List.of(), ToolHelper.getMaterials(stack)), () -> assertEquals(0, ToolHelper.getModifierLevel(stack, SHARPNESS)),
                () -> assertSame(ToolStats.zero(), ToolHelper.getStats(stack)), () -> assertFalse(ToolHelper.isBroken(stack)));
        verify(stack, never()).get(any(DataComponentType.class));
    }

    @Test
    void accessorsReturnDefaultsWhenComponentIsMissing() {
        // Non-empty stack with no component attached — return same defaults as an empty stack.
        ItemStack stack = nonEmptyStack();
        assertAll(() -> assertEquals(List.of(), ToolHelper.getMaterials(stack)), () -> assertEquals(0, ToolHelper.getModifierLevel(stack, SHARPNESS)),
                () -> assertSame(ToolStats.zero(), ToolHelper.getStats(stack)), () -> assertFalse(ToolHelper.isBroken(stack)));
    }

    @Test
    void getMaterialsReturnsAttachedPartsList() {
        ItemStack stack = nonEmptyStack();
        when(stack.get(eq(MATERIALS))).thenReturn(new ToolMaterials(List.of(IRON, WOOD)));
        assertEquals(List.of(IRON, WOOD), ToolHelper.getMaterials(stack));
    }

    @Test
    void getModifierLevelReadsFromAttachedMap() {
        ItemStack stack = nonEmptyStack();
        when(stack.get(eq(MODIFIERS))).thenReturn(ToolModifiers.empty().with(SHARPNESS, 3));
        assertAll(() -> assertEquals(3, ToolHelper.getModifierLevel(stack, SHARPNESS)),
                // Missing id reads as 0, not -1 or null — legacy "not applied" sentinel.
                () -> assertEquals(0, ToolHelper.getModifierLevel(stack, HASTE)));
    }

    @Test
    void getModifierLevelRejectsNullId() {
        ItemStack stack = nonEmptyStack();
        assertThrows(NullPointerException.class, () -> ToolHelper.getModifierLevel(stack, null));
    }

    @Test
    void getStatsReturnsAttachedSnapshot() {
        ItemStack stack = nonEmptyStack();
        ToolStats stats = new ToolStats(250, 4.0F, 1.6F, 6.0F, 2, 3, 0.0F, 0.0F, 0.0F);
        when(stack.get(eq(STATS))).thenReturn(stats);
        assertSame(stats, ToolHelper.getStats(stack));
    }

    @Test
    void isBrokenReadsTheBrokenBit() {
        ItemStack stack = nonEmptyStack();
        when(stack.get(eq(BROKEN))).thenReturn(ToolBroken.BROKEN);
        assertTrue(ToolHelper.isBroken(stack));
    }

    @Test
    void isBrokenReadsFalseWhenComponentIsIntact() {
        ItemStack stack = nonEmptyStack();
        when(stack.get(eq(BROKEN))).thenReturn(ToolBroken.intact());
        assertFalse(ToolHelper.isBroken(stack));
    }

    // ----------------------------------------------------------------------------- mutators

    @Test
    void setMaterialsWritesAFreshRecordAndPreservesCallerListIdentity() {
        // AC3: mutators must always set a fresh immutable record. The caller's list must not be
        // captured by reference — a downstream alias mutation can't leak back into the stack.
        ItemStack stack = nonEmptyStack();
        List<ResourceLocation> callerList = new ArrayList<>(List.of(IRON, WOOD));
        ToolHelper.setMaterials(stack, callerList);
        ArgumentCaptor<ToolMaterials> captor = ArgumentCaptor.forClass(ToolMaterials.class);
        verify(stack).set(eq(MATERIALS), captor.capture());
        ToolMaterials written = captor.getValue();
        callerList.clear();
        assertEquals(List.of(IRON, WOOD), written.parts(), "ToolMaterials must copy the caller's list defensively");
        assertThrows(UnsupportedOperationException.class, () -> written.parts().add(IRON));
    }

    @Test
    void addModifierAppendsToExistingMapAndPreservesPreviousLevels() {
        ItemStack stack = nonEmptyStack();
        ToolModifiers existing = ToolModifiers.empty().with(SHARPNESS, 2);
        when(stack.getOrDefault(eq(MODIFIERS), any())).thenReturn(existing);
        ToolHelper.addModifier(stack, HASTE, 1);
        ArgumentCaptor<ToolModifiers> captor = ArgumentCaptor.forClass(ToolModifiers.class);
        verify(stack).set(eq(MODIFIERS), captor.capture());
        ToolModifiers written = captor.getValue();
        assertAll(() -> assertEquals(2, written.levels().getOrDefault(SHARPNESS, 0), "existing levels preserved"), () -> assertEquals(1, written.levels().getOrDefault(HASTE, 0), "new level applied"),
                () -> assertNotSame(existing, written, "mutator must build a fresh record"));
    }

    @Test
    void addModifierStartsFromEmptyWhenComponentNotYetAttached() {
        // The helper falls back to ToolModifiers.empty() so the first modifier on a fresh tool
        // doesn't NPE on a missing component.
        ItemStack stack = nonEmptyStack();
        when(stack.getOrDefault(eq(MODIFIERS), any())).thenReturn(ToolModifiers.empty());
        ToolHelper.addModifier(stack, SHARPNESS, 5);
        ArgumentCaptor<ToolModifiers> captor = ArgumentCaptor.forClass(ToolModifiers.class);
        verify(stack).set(eq(MODIFIERS), captor.capture());
        assertEquals(5, captor.getValue().levels().getOrDefault(SHARPNESS, 0));
    }

    @Test
    void addModifierRejectsNullId() {
        ItemStack stack = nonEmptyStack();
        assertThrows(NullPointerException.class, () -> ToolHelper.addModifier(stack, null, 1));
    }

    @Test
    void addModifierRejectsNegativeLevel() {
        // Negative levels are a calling-code bug — modifier dispatch branches on level > 0.
        // Failing at the call site is more actionable than a silent miscount downstream.
        ItemStack stack = nonEmptyStack();
        assertThrows(IllegalArgumentException.class, () -> ToolHelper.addModifier(stack, SHARPNESS, -1));
        verify(stack, never()).set(eq(MODIFIERS), any(ToolModifiers.class));
    }

    @Test
    void addModifierWithZeroStoresExplicitZero() {
        // Level 0 is the legacy "applied but inactive" sentinel — kept distinct from "absent"
        // so insertion-order placement survives a temporary cap-tier disable.
        ItemStack stack = nonEmptyStack();
        when(stack.getOrDefault(eq(MODIFIERS), any())).thenReturn(ToolModifiers.empty());
        ToolHelper.addModifier(stack, SHARPNESS, 0);
        ArgumentCaptor<ToolModifiers> captor = ArgumentCaptor.forClass(ToolModifiers.class);
        verify(stack).set(eq(MODIFIERS), captor.capture());
        assertTrue(captor.getValue().levels().containsKey(SHARPNESS), "level 0 stores an explicit entry");
        assertEquals(0, captor.getValue().levels().get(SHARPNESS));
    }

    @Test
    void addModifierOverwritesExistingLevel() {
        // Same-id rewrite is the cap-tier upgrade path — pin that addModifier overwrites the
        // current level instead of stacking or being a no-op.
        ItemStack stack = nonEmptyStack();
        when(stack.getOrDefault(eq(MODIFIERS), any())).thenReturn(ToolModifiers.empty().with(SHARPNESS, 2));
        ToolHelper.addModifier(stack, SHARPNESS, 5);
        ArgumentCaptor<ToolModifiers> captor = ArgumentCaptor.forClass(ToolModifiers.class);
        verify(stack).set(eq(MODIFIERS), captor.capture());
        assertEquals(5, captor.getValue().levels().get(SHARPNESS));
    }

    @Test
    void addModifierAcceptsIntegerMaxValue() {
        // Upper bound is the codec's VAR_INT ceiling — pinned so a future bound-check doesn't
        // silently clip cap-tier modifiers that legitimately stack to large levels.
        ItemStack stack = nonEmptyStack();
        when(stack.getOrDefault(eq(MODIFIERS), any())).thenReturn(ToolModifiers.empty());
        ToolHelper.addModifier(stack, SHARPNESS, Integer.MAX_VALUE);
        ArgumentCaptor<ToolModifiers> captor = ArgumentCaptor.forClass(ToolModifiers.class);
        verify(stack).set(eq(MODIFIERS), captor.capture());
        assertEquals(Integer.MAX_VALUE, captor.getValue().levels().get(SHARPNESS));
    }

    @Test
    void setBrokenRoutesThroughSingletons() {
        // Cheap-allocation contract: every durability tick must reuse the BROKEN / intact()
        // singletons rather than churning a fresh record.
        ItemStack stack = nonEmptyStack();
        ToolHelper.setBroken(stack, true);
        ToolHelper.setBroken(stack, false);
        // same(...) asserts reference identity — eq(...) would pass a defensive copy too. The
        // contract is that the durability-tick path reuses the singletons, so identity matters.
        verify(stack).set(eq(BROKEN), same(ToolBroken.BROKEN));
        verify(stack).set(eq(BROKEN), same(ToolBroken.intact()));
    }

    @Test
    void mutatorsAreNoOpsOnEmptyStacks() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(true);
        ToolHelper.setMaterials(stack, List.of(IRON));
        ToolHelper.addModifier(stack, SHARPNESS, 1);
        ToolHelper.setBroken(stack, true);
        verify(stack, never()).set(any(DataComponentType.class), any());
    }

    @Test
    void setMaterialsRejectsNullList() {
        ItemStack stack = nonEmptyStack();
        assertThrows(NullPointerException.class, () -> ToolHelper.setMaterials(stack, null));
    }

    @Test
    void setMaterialsTriggersStatRebuildAndSetMaterialsComponent() {
        // Mutator must touch the MATERIALS component (the rebuild hook is currently a no-op
        // pending SMTCON-77, so we assert the visible side effect — the component write).
        ItemStack stack = nonEmptyStack();
        ToolHelper.setMaterials(stack, List.of(IRON));
        verify(stack, times(1)).set(eq(MATERIALS), any(ToolMaterials.class));
    }

    private static ItemStack nonEmptyStack() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        return stack;
    }
}
