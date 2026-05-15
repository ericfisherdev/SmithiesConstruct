package slimeknights.sconstruct.port1211.tools;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.common.data.ToolBroken;
import slimeknights.sconstruct.port1211.common.data.ToolMaterials;
import slimeknights.sconstruct.port1211.common.data.ToolModifiers;
import slimeknights.sconstruct.port1211.common.data.ToolStats;
import slimeknights.sconstruct.port1211.tools.material.Material;

/**
 * Pinned-behaviour tests for {@link ToolHelper}. The class is a façade over five
 * {@code DataComponentType}s — covers every accessor's empty-stack / missing-component default
 * (AC2), every mutator's "always set a fresh immutable record" contract (AC3), and the
 * rebuild-stats side effect on materials / modifier writes.
 */
// MinecraftServer is AutoCloseable, but the mocks here are pure stubs with no resources to
// release — PMD's CloseResource heuristic doesn't model Mockito's lifecycle.
@SuppressWarnings("PMD.CloseResource")
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
    void mutatorsSwallowInvalidInputsOnEmptyStacks() {
        // Empty-stack short-circuit must fire ahead of argument validation — a future guard
        // reorder would otherwise crack the "no-op on empty stacks" contract for the worst-case
        // calling pattern (uninitialised drops in inventory pipelines, where the caller hasn't
        // verified its arguments yet).
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(true);
        ToolHelper.setMaterials(stack, null);
        ToolHelper.addModifier(stack, null, -1);
        ToolHelper.addModifier(stack, SHARPNESS, -1);
        verify(stack, never()).set(any(DataComponentType.class), any());
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

    // -------------------------------------------------------------------- rebuildStats (SMTCON-77)

    @Test
    void rebuildStatsThrowsWhenCalledOffServerThread() {
        // Client-thread guard — running the rebuild without server-thread affinity would read a
        // stale / unsynced material registry; surface the misuse at the call site.
        ItemStack stack = nonEmptyStack();
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.isSameThread()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> ToolHelper.rebuildStats(stack, server, ToolDefinition.PICKAXE));
        verify(stack, never()).set(any(DataComponentType.class), any());
    }

    @Test
    void rebuildStatsRejectsNullArguments() {
        ItemStack stack = nonEmptyStack();
        MinecraftServer server = mock(MinecraftServer.class);
        assertAll(() -> assertThrows(NullPointerException.class, () -> ToolHelper.rebuildStats(null, server, ToolDefinition.PICKAXE)),
                () -> assertThrows(NullPointerException.class, () -> ToolHelper.rebuildStats(stack, null, ToolDefinition.PICKAXE)),
                () -> assertThrows(NullPointerException.class, () -> ToolHelper.rebuildStats(stack, server, null)));
    }

    @Test
    void rebuildStatsIsNoOpForEmptyStack() {
        // Empty-stack short-circuit mirrors every other mutator on the SMTCON-74 façade.
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(true);
        MinecraftServer server = serverOnServerThreadWithEmptyMaterials();
        ToolHelper.rebuildStats(stack, server, ToolDefinition.PICKAXE);
        verify(stack, never()).set(any(DataComponentType.class), any());
        verify(stack, never()).setDamageValue(anyInt());
    }

    @Test
    void rebuildStatsWritesStatsMaxDamageAndAttributeComponents() {
        // AC: ItemStack components reflect computed values. Materials list is empty here so
        // StatsBuilder folds in the wood baseline for every pickaxe slot; the test pins the
        // wood-pickaxe stat block against the cached StatsBuilder constants. A future shift
        // in the wood baseline will surface here as a clear delta rather than a silent drift.
        ItemStack stack = nonEmptyStack();
        when(stack.getDamageValue()).thenReturn(0);
        when(stack.getOrDefault(eq(TinkerDataComponents.TOOL_MODIFIERS.get()), any())).thenReturn(ToolModifiers.empty());
        MinecraftServer server = serverOnServerThreadWithEmptyMaterials();

        ToolHelper.rebuildStats(stack, server, ToolDefinition.PICKAXE);

        ArgumentCaptor<ToolStats> statsCaptor = ArgumentCaptor.forClass(ToolStats.class);
        verify(stack).set(eq(STATS), statsCaptor.capture());
        ToolStats written = statsCaptor.getValue();
        // Pure wood pickaxe (3 slots, all wood fallback): head=35 dur × handle 1.0 + extra 15 = 50.
        assertAll(() -> assertEquals(50, written.maxDurability(), "wood pickaxe baseline durability"), () -> assertEquals(2.0F, written.attackDamage(), 0.0001F),
                () -> assertEquals(2.0F, written.miningSpeed(), 0.0001F), () -> assertEquals(0, written.harvestLevel()), () -> assertEquals(3, written.freeModifiers()));

        verify(stack).set(eq(DataComponents.MAX_DAMAGE), eq(50));

        ArgumentCaptor<ItemAttributeModifiers> attrCaptor = ArgumentCaptor.forClass(ItemAttributeModifiers.class);
        verify(stack).set(eq(DataComponents.ATTRIBUTE_MODIFIERS), attrCaptor.capture());
        ItemAttributeModifiers attrs = attrCaptor.getValue();
        // Both modifiers must land in the mainhand slot (off-hand / armor see no swing bonus).
        attrs.modifiers().forEach(entry -> assertEquals(EquipmentSlotGroup.MAINHAND, entry.slot()));
        assertEquals(2, attrs.modifiers().size(), "attack damage + attack speed = two modifiers");
    }

    @Test
    void rebuildStatsClampsDamageAndMarksBrokenWhenMaxDurabilityDropsBelowCurrent() {
        // AC: damage value never exceeds maxDurability after rebuild. Recompute path mid-life
        // (e.g. cap-tier downgrade) must clamp the damage AND resync the TOOL_BROKEN flag —
        // damage == maxDurability after clamp means the tool is broken in the same tick.
        ItemStack stack = nonEmptyStack();
        when(stack.getDamageValue()).thenReturn(500); // far past the wood-pickaxe ceiling of 50
        when(stack.getOrDefault(eq(TinkerDataComponents.TOOL_MODIFIERS.get()), any())).thenReturn(ToolModifiers.empty());
        MinecraftServer server = serverOnServerThreadWithEmptyMaterials();

        ToolHelper.rebuildStats(stack, server, ToolDefinition.PICKAXE);

        verify(stack).setDamageValue(50);
        verify(stack).set(eq(BROKEN), same(ToolBroken.BROKEN));
    }

    @Test
    void rebuildStatsDoesNotResetDamageWhenWithinNewMax() {
        // Inverse of the clamp test — a tool whose existing damage is below the new ceiling
        // keeps its damage untouched, so the player's wear-and-tear progress doesn't reset on
        // every material swap. The broken flag must also resolve to intact.
        ItemStack stack = nonEmptyStack();
        when(stack.getDamageValue()).thenReturn(20);
        when(stack.getOrDefault(eq(TinkerDataComponents.TOOL_MODIFIERS.get()), any())).thenReturn(ToolModifiers.empty());
        MinecraftServer server = serverOnServerThreadWithEmptyMaterials();

        ToolHelper.rebuildStats(stack, server, ToolDefinition.PICKAXE);

        verify(stack, never()).setDamageValue(anyInt());
        verify(stack).set(eq(BROKEN), same(ToolBroken.intact()));
    }

    @Test
    void rebuildStatsUnbreaksToolWhenCeilingRisesAboveDamage() {
        // A material upgrade that lifts maxDurability past the current damage must clear the
        // broken bit so the UI / damage-handler sees the tool as usable again on the same
        // tick the stats were recomputed.
        ItemStack stack = nonEmptyStack();
        when(stack.getDamageValue()).thenReturn(30); // below the new wood-pickaxe ceiling of 50
        when(stack.getOrDefault(eq(TinkerDataComponents.TOOL_MODIFIERS.get()), any())).thenReturn(ToolModifiers.empty());
        MinecraftServer server = serverOnServerThreadWithEmptyMaterials();

        ToolHelper.rebuildStats(stack, server, ToolDefinition.PICKAXE);

        verify(stack).set(eq(BROKEN), same(ToolBroken.intact()));
    }

    // ----------------------------------------------------------------- repair (SMTCON-80)

    @Test
    void repairThrowsOffServerThread() {
        ItemStack stack = nonEmptyStack();
        ItemStack repairItem = nonEmptyStack();
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.isSameThread()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> ToolHelper.repair(stack, repairItem, server, ToolDefinition.PICKAXE));
    }

    @Test
    void repairRejectsNullArguments() {
        ItemStack stack = nonEmptyStack();
        ItemStack repairItem = nonEmptyStack();
        MinecraftServer server = mock(MinecraftServer.class);
        assertAll(() -> assertThrows(NullPointerException.class, () -> ToolHelper.repair(null, repairItem, server, ToolDefinition.PICKAXE)),
                () -> assertThrows(NullPointerException.class, () -> ToolHelper.repair(stack, null, server, ToolDefinition.PICKAXE)),
                () -> assertThrows(NullPointerException.class, () -> ToolHelper.repair(stack, repairItem, null, ToolDefinition.PICKAXE)),
                () -> assertThrows(NullPointerException.class, () -> ToolHelper.repair(stack, repairItem, server, null)));
    }

    @Test
    void repairIsNoOpForEmptyStacks() {
        ItemStack emptyStack = mock(ItemStack.class);
        when(emptyStack.isEmpty()).thenReturn(true);
        MinecraftServer server = serverOnServerThreadWithEmptyMaterials();
        assertEquals(0, ToolHelper.repair(emptyStack, nonEmptyStack(), server, ToolDefinition.PICKAXE));
    }

    @Test
    void repairIsNoOpWhenToolIsAlreadyAtFullDurability() {
        // Damage 0 means nothing to repair — return 0 items consumed so the caller doesn't shrink
        // the repair stack for a no-op interaction.
        ItemStack stack = nonEmptyStack();
        when(stack.getMaxDamage()).thenReturn(250);
        when(stack.getDamageValue()).thenReturn(0);
        ItemStack repairItem = nonEmptyStack();
        MinecraftServer server = serverWithIronMaterial();
        assertEquals(0, ToolHelper.repair(stack, repairItem, server, ToolDefinition.PICKAXE));
    }

    @Test
    void repairIsNoOpWhenMaterialNotInRegistry() {
        // Head material id resolves to no Material entry — registry desync between server save
        // and reload; bail out rather than NPE.
        ItemStack stack = builtPickaxeStack(250, 100, IRON);
        ItemStack repairItem = nonEmptyStack();
        MinecraftServer server = serverOnServerThreadWithEmptyMaterials();
        assertEquals(0, ToolHelper.repair(stack, repairItem, server, ToolDefinition.PICKAXE));
    }

    @Test
    void repairIsNoOpWhenRepairItemDoesNotMatchTag() {
        // The supplied repair item is in a different tag than the head material's repair tag —
        // wrong fuel for this material's repair binding.
        ItemStack stack = builtPickaxeStack(250, 100, IRON);
        ItemStack repairItem = nonEmptyStack();
        when(repairItem.is(any(TagKey.class))).thenReturn(false);
        MinecraftServer server = serverWithIronMaterial();
        assertEquals(0, ToolHelper.repair(stack, repairItem, server, ToolDefinition.PICKAXE));
    }

    @Test
    void repairConsumesOneItemAndRestoresOneQuarterOfMaxDurability() {
        // Legacy 1.12 parity: each item restores 25% of maxDurability. 250 × 0.25 = 62.5
        // rounded to 63 — one consumed item against ~63 damage fully restores the tool, and
        // the helper consumes only the single item needed (not the rest of the stack).
        ItemStack stack = builtPickaxeStack(250, 63, IRON);
        ItemStack repairItem = repairItemMatchingTag(4);
        MinecraftServer server = serverWithIronMaterial();

        int consumed = ToolHelper.repair(stack, repairItem, server, ToolDefinition.PICKAXE);

        assertEquals(1, consumed);
        verify(stack).setDamageValue(0);
        verify(stack).set(eq(BROKEN), same(ToolBroken.intact()));
    }

    @Test
    void repairConsumesOnlyAsManyItemsAsNeededToFullyRestore() {
        // Damage 70, repairPerItem = 63 — two items would over-repair (consume 126 > damage 70).
        // The helper must cap consumption at the items-needed ceiling.
        ItemStack stack = builtPickaxeStack(250, 70, IRON);
        ItemStack repairItem = repairItemMatchingTag(4);
        MinecraftServer server = serverWithIronMaterial();

        int consumed = ToolHelper.repair(stack, repairItem, server, ToolDefinition.PICKAXE);

        assertEquals(2, consumed); // ceil(70 / 63) = 2
        verify(stack).setDamageValue(0); // capped at full restoration
        verify(stack).set(eq(BROKEN), same(ToolBroken.intact()));
    }

    @Test
    void repairUnbreaksToolWhenAnyDamageBudgetIsRestored() {
        // Single repair item against a fully-broken tool restores the broken bit even if the
        // tool isn't fully repaired — legacy parity, lets the player swing again.
        ItemStack stack = builtPickaxeStack(250, 250, IRON);
        ItemStack repairItem = repairItemMatchingTag(1);
        MinecraftServer server = serverWithIronMaterial();

        int consumed = ToolHelper.repair(stack, repairItem, server, ToolDefinition.PICKAXE);

        assertEquals(1, consumed);
        verify(stack).setDamageValue(187); // 250 - 63
        verify(stack).set(eq(BROKEN), same(ToolBroken.intact()));
    }

    @Test
    void repairCapsConsumptionAtAvailableStackSize() {
        // 200 damage, repairPerItem = 63 → 4 items would fully repair, but the stack only has 1.
        // The helper must consume 1 and restore proportionally rather than mutating beyond the
        // stack's available count.
        ItemStack stack = builtPickaxeStack(250, 200, IRON);
        ItemStack repairItem = repairItemMatchingTag(1);
        MinecraftServer server = serverWithIronMaterial();

        int consumed = ToolHelper.repair(stack, repairItem, server, ToolDefinition.PICKAXE);

        assertEquals(1, consumed);
        verify(stack).setDamageValue(137); // 200 - 63
    }

    /**
     * Server mock pre-wired so {@link MinecraftServer#isSameThread} returns {@code true} and the
     * material registry lookup returns an empty {@link Optional} for every id — drives the
     * StatsBuilder wood-fallback path without needing a real Material registry under test.
     */
    @SuppressWarnings("unchecked")
    private static MinecraftServer serverOnServerThreadWithEmptyMaterials() {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.isSameThread()).thenReturn(true);
        // MinecraftServer#registryAccess returns the Frozen subtype, so mock that specific
        // subtype rather than the raw RegistryAccess interface to match the signature.
        RegistryAccess.Frozen registryAccess = mock(RegistryAccess.Frozen.class);
        HolderLookup.RegistryLookup<Material> lookup = mock(HolderLookup.RegistryLookup.class);
        when(server.registryAccess()).thenReturn(registryAccess);
        when(registryAccess.lookupOrThrow(eq(Material.REGISTRY_KEY))).thenReturn(lookup);
        // HolderGetter#get is overloaded for ResourceKey and TagKey — disambiguate to the
        // ResourceKey overload that rebuildStats actually calls.
        when(lookup.get(any(ResourceKey.class))).thenReturn(Optional.empty());
        return server;
    }

    private static ItemStack nonEmptyStack() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        return stack;
    }

    /**
     * A non-empty stack stubbed to look like a built {@link slimeknights.sconstruct.port1211.tools.ToolDefinition#PICKAXE}
     * with the supplied head material at slot 1 ({@link slimeknights.sconstruct.port1211.tools.PartType#PICKHEAD}).
     * The materials list mirrors the pickaxe's positional part order: handle / pickhead /
     * binding — wood for the non-head slots, the supplied id for the head.
     */
    private static ItemStack builtPickaxeStack(int maxDurability, int currentDamage, ResourceLocation headMaterial) {
        ItemStack stack = nonEmptyStack();
        when(stack.getMaxDamage()).thenReturn(maxDurability);
        when(stack.getDamageValue()).thenReturn(currentDamage);
        when(stack.get(eq(MATERIALS))).thenReturn(new ToolMaterials(List.of(WOOD, headMaterial, WOOD)));
        return stack;
    }

    /**
     * A repair-item stack that matches every {@link TagKey} membership probe. Lets repair tests
     * pin the "tag matched" branch without standing up a real ItemTags registry.
     */
    private static ItemStack repairItemMatchingTag(int count) {
        ItemStack repairItem = mock(ItemStack.class);
        when(repairItem.isEmpty()).thenReturn(false);
        when(repairItem.getCount()).thenReturn(count);
        when(repairItem.is(any(TagKey.class))).thenReturn(true);
        return repairItem;
    }

    /**
     * Server mock pre-wired with a {@link slimeknights.sconstruct.port1211.tools.material.Material}
     * keyed at {@link #IRON}. The material's repair tag is non-empty; the test seam
     * {@link #repairItemMatchingTag} returns {@code true} for every tag probe so the supplied
     * repair item resolves as a match.
     */
    @SuppressWarnings("unchecked")
    private static MinecraftServer serverWithIronMaterial() {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.isSameThread()).thenReturn(true);
        RegistryAccess.Frozen registryAccess = mock(RegistryAccess.Frozen.class);
        HolderLookup.RegistryLookup<Material> lookup = mock(HolderLookup.RegistryLookup.class);
        when(server.registryAccess()).thenReturn(registryAccess);
        when(registryAccess.lookupOrThrow(eq(Material.REGISTRY_KEY))).thenReturn(lookup);

        // Materials registry holds one entry — iron — whose repair tag is the iron-ingot tag.
        // The actual TagKey value is irrelevant because repairItemMatchingTag's mock answers
        // true for any TagKey probe; we just need the Optional to be non-empty so the helper
        // doesn't short-circuit on a missing tag.
        TagKey<Item> ironRepairTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/iron"));
        Material iron = new Material(IRON, 2, Optional.of(ironRepairTag), Map.of(), List.of(), 0xFFFFFFFF);
        @SuppressWarnings("rawtypes")
        Holder.Reference holder = mock(Holder.Reference.class);
        when(holder.value()).thenReturn(iron);
        when(lookup.get(any(ResourceKey.class))).thenReturn(Optional.of(holder));
        return server;
    }
}
