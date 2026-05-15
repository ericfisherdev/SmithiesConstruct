package slimeknights.sconstruct.port1211.tools.modifier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.common.data.ToolBroken;
import slimeknights.sconstruct.port1211.common.data.ToolModifiers;

/**
 * Pinned-behaviour tests for {@link ModifierHookDispatcher}. Covers SMTCON-83's acceptance
 * criteria: the dispatcher iterates the {@link ToolModifiers} map in deterministic insertion
 * order, each modifier hook fires once per event, broken tools skip every hook, client-side
 * ticks are filtered, and unknown modifier ids are silently dropped (so a registry desync
 * doesn't crash the swing).
 */
// Level is AutoCloseable, but the mocks here are pure stubs with no resources to release —
// PMD's CloseResource heuristic doesn't model Mockito's lifecycle.
@SuppressWarnings("PMD.CloseResource")
class ModifierHookDispatcherTest {

    private static final ResourceLocation SHARPNESS = ResourceLocation.fromNamespaceAndPath("sconstruct", "sharpness");
    private static final ResourceLocation HASTE = ResourceLocation.fromNamespaceAndPath("sconstruct", "haste");
    private static final ResourceLocation UNKNOWN = ResourceLocation.fromNamespaceAndPath("sconstruct", "unknown");

    @BeforeEach
    void clearRegistryCache() {
        ModifierRegistry.clearCacheForTest();
        registryCache.clear();
    }

    @AfterEach
    void tearDownRegistryCache() {
        ModifierRegistry.clearCacheForTest();
    }

    // ----------------------------------------------------------- null-argument guards

    @Test
    void dispatchOnAttackRejectsNullArguments() {
        ItemStack stack = nonEmptyStack();
        ToolEvents.OnHitContext context = new ToolEvents.OnHitContext(mock(Player.class), mock(LivingEntity.class), mockServerLevel(), 0F);
        assertThrows(NullPointerException.class, () -> ModifierHookDispatcher.dispatchOnAttack(null, context));
        assertThrows(NullPointerException.class, () -> ModifierHookDispatcher.dispatchOnAttack(stack, null));
    }

    @Test
    void dispatchOnMineRejectsNullArguments() {
        ItemStack stack = nonEmptyStack();
        ToolEvents.OnMineContext context = new ToolEvents.OnMineContext(mock(Player.class), mock(BlockState.class), BlockPos.ZERO, mockServerLevel());
        assertThrows(NullPointerException.class, () -> ModifierHookDispatcher.dispatchOnMine(null, context));
        assertThrows(NullPointerException.class, () -> ModifierHookDispatcher.dispatchOnMine(stack, null));
    }

    @Test
    void dispatchOnBuildRejectsNullStack() {
        assertThrows(NullPointerException.class, () -> ModifierHookDispatcher.dispatchOnBuild(null));
    }

    // ----------------------------------------------------------- broken-state guard

    @Test
    void dispatchOnAttackSkipsBrokenTool() {
        Modifier modifier = registerMock(SHARPNESS);
        ItemStack stack = nonEmptyStack();
        when(stack.get(TinkerDataComponents.TOOL_BROKEN.get())).thenReturn(ToolBroken.BROKEN);

        ModifierHookDispatcher.dispatchOnAttack(stack, new ToolEvents.OnHitContext(mock(Player.class), mock(LivingEntity.class), mockServerLevel(), 0F));

        verify(modifier, never()).onAttack(any(), anyInt(), any());
    }

    @Test
    void dispatchOnMineSkipsBrokenTool() {
        Modifier modifier = registerMock(SHARPNESS);
        ItemStack stack = nonEmptyStack();
        when(stack.get(TinkerDataComponents.TOOL_BROKEN.get())).thenReturn(ToolBroken.BROKEN);

        ModifierHookDispatcher.dispatchOnMine(stack, new ToolEvents.OnMineContext(mock(Player.class), mock(BlockState.class), BlockPos.ZERO, mockServerLevel()));

        verify(modifier, never()).onMine(any(), anyInt(), any());
    }

    @Test
    void dispatchOnBuildSkipsBrokenTool() {
        Modifier modifier = registerMock(SHARPNESS);
        ItemStack stack = stackWithModifiers(SHARPNESS, 1);
        when(stack.get(TinkerDataComponents.TOOL_BROKEN.get())).thenReturn(ToolBroken.BROKEN);

        ModifierHookDispatcher.dispatchOnBuild(stack);

        verify(modifier, never()).onBuild(any(), anyInt());
    }

    // ----------------------------------------------------------- client-side guard

    @Test
    void dispatchOnAttackSkipsClientSideHook() {
        Modifier modifier = registerMock(SHARPNESS);
        ItemStack stack = stackWithModifiers(SHARPNESS, 1);
        Level clientLevel = mock(Level.class);
        when(clientLevel.isClientSide()).thenReturn(true);

        ModifierHookDispatcher.dispatchOnAttack(stack, new ToolEvents.OnHitContext(mock(Player.class), mock(LivingEntity.class), clientLevel, 0F));

        verify(modifier, never()).onAttack(any(), anyInt(), any());
    }

    @Test
    void dispatchOnMineSkipsClientSideHook() {
        Modifier modifier = registerMock(SHARPNESS);
        ItemStack stack = stackWithModifiers(SHARPNESS, 1);
        Level clientLevel = mock(Level.class);
        when(clientLevel.isClientSide()).thenReturn(true);

        ModifierHookDispatcher.dispatchOnMine(stack, new ToolEvents.OnMineContext(mock(Player.class), mock(BlockState.class), BlockPos.ZERO, clientLevel));

        verify(modifier, never()).onMine(any(), anyInt(), any());
    }

    // ----------------------------------------------------------- deterministic dispatch

    @Test
    void dispatchOnAttackInvokesEveryAppliedModifierInInsertionOrder() {
        Modifier sharpness = registerMock(SHARPNESS);
        Modifier haste = registerMock(HASTE);

        ItemStack stack = stackWithModifiers(SHARPNESS, 2, HASTE, 1);
        ToolEvents.OnHitContext context = new ToolEvents.OnHitContext(mock(Player.class), mock(LivingEntity.class), mockServerLevel(), 0F);

        ModifierHookDispatcher.dispatchOnAttack(stack, context);

        // Each modifier's hook fires once with its declared level.
        verify(sharpness).onAttack(eq(stack), eq(2), eq(context));
        verify(haste).onAttack(eq(stack), eq(1), eq(context));
    }

    @Test
    void dispatchOnAttackPreservesInsertionOrder() {
        // Two modifiers registered: capture the call order via ArgumentCaptor on a shared mock
        // observer. Insertion order is sharpness → haste; iteration must walk in that order.
        List<ResourceLocation> seenOrder = new ArrayList<>();
        Modifier sharpness = mock(Modifier.class);
        when(sharpness.id()).thenReturn(SHARPNESS);
        doRecord(sharpness, SHARPNESS, seenOrder);
        Modifier haste = mock(Modifier.class);
        when(haste.id()).thenReturn(HASTE);
        doRecord(haste, HASTE, seenOrder);
        ModifierRegistry.overwriteCacheForTest(java.util.Map.of(SHARPNESS, Holder.direct(sharpness), HASTE, Holder.direct(haste)));

        ItemStack stack = stackWithModifiers(SHARPNESS, 1, HASTE, 1);
        ModifierHookDispatcher.dispatchOnAttack(stack, new ToolEvents.OnHitContext(mock(Player.class), mock(LivingEntity.class), mockServerLevel(), 0F));

        assertEquals(List.of(SHARPNESS, HASTE), seenOrder, "dispatch must iterate ToolModifiers.levels in insertion order");
    }

    @Test
    void dispatchOnAttackSkipsLevelZeroEntries() {
        // Level 0 is the "applied but inactive" sentinel per SMTCON-74 — the dispatcher must
        // not fire the hook so a sleeping modifier doesn't accidentally swing.
        Modifier modifier = registerMock(SHARPNESS);
        ItemStack stack = stackWithModifiers(SHARPNESS, 0);
        ModifierHookDispatcher.dispatchOnAttack(stack, new ToolEvents.OnHitContext(mock(Player.class), mock(LivingEntity.class), mockServerLevel(), 0F));
        verify(modifier, never()).onAttack(any(), anyInt(), any());
    }

    @Test
    void dispatchOnAttackSkipsUnknownIds() {
        // Stack references an id the registry doesn't know — registry desync between save and
        // reload. The dispatcher must silently drop rather than throwing so the swing tick
        // doesn't crash.
        ItemStack stack = stackWithModifiers(UNKNOWN, 1);
        // No assertion needed — just confirm the call doesn't throw.
        // The dispatch must not throw for unknown ids — registry desync between save and
        // reload is recoverable, not fatal.
        assertDoesNotThrow(() -> ModifierHookDispatcher.dispatchOnAttack(stack, new ToolEvents.OnHitContext(mock(Player.class), mock(LivingEntity.class), mockServerLevel(), 0F)));
    }

    @Test
    void dispatchOnMineFiresOnceWithTheSuppliedContext() {
        Modifier modifier = registerMock(SHARPNESS);
        ItemStack stack = stackWithModifiers(SHARPNESS, 3);
        ToolEvents.OnMineContext context = new ToolEvents.OnMineContext(mock(Player.class), mock(BlockState.class), BlockPos.ZERO, mockServerLevel());

        ModifierHookDispatcher.dispatchOnMine(stack, context);

        ArgumentCaptor<Integer> levelCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(modifier).onMine(eq(stack), levelCaptor.capture(), eq(context));
        assertEquals(3, levelCaptor.getValue());
    }

    @Test
    void dispatchOnBuildFiresOncePerAppliedModifier() {
        Modifier modifier = registerMock(SHARPNESS);
        ItemStack stack = stackWithModifiers(SHARPNESS, 5);

        ModifierHookDispatcher.dispatchOnBuild(stack);

        verify(modifier).onBuild(eq(stack), eq(5));
    }

    @Test
    void dispatchOnAttackIsNoOpForEmptyStack() {
        Modifier modifier = registerMock(SHARPNESS);
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(true);

        ModifierHookDispatcher.dispatchOnAttack(stack, new ToolEvents.OnHitContext(mock(Player.class), mock(LivingEntity.class), mockServerLevel(), 0F));

        verify(modifier, never()).onAttack(any(), anyInt(), any());
    }

    // ----------------------------------------------------------- helpers

    private static ItemStack nonEmptyStack() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.get(TinkerDataComponents.TOOL_BROKEN.get())).thenReturn(ToolBroken.intact());
        return stack;
    }

    private static ItemStack stackWithModifiers(ResourceLocation id1, int level1) {
        ItemStack stack = nonEmptyStack();
        ToolModifiers modifiers = ToolModifiers.empty().with(id1, level1);
        when(stack.getOrDefault(eq(TinkerDataComponents.TOOL_MODIFIERS.get()), any())).thenReturn(modifiers);
        return stack;
    }

    private static ItemStack stackWithModifiers(ResourceLocation id1, int level1, ResourceLocation id2, int level2) {
        ItemStack stack = nonEmptyStack();
        ToolModifiers modifiers = ToolModifiers.empty().with(id1, level1).with(id2, level2);
        when(stack.getOrDefault(eq(TinkerDataComponents.TOOL_MODIFIERS.get()), any())).thenReturn(modifiers);
        return stack;
    }

    private static Level mockServerLevel() {
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        return level;
    }

    private final java.util.Map<ResourceLocation, Holder<Modifier>> registryCache = new java.util.HashMap<>();

    /** Register a Modifier mock under {@code id} in the {@link ModifierRegistry} test cache,
     *  accumulating across calls so multi-modifier tests don't lose previous entries. */
    private Modifier registerMock(ResourceLocation id) {
        Modifier modifier = mock(Modifier.class);
        when(modifier.id()).thenReturn(id);
        registryCache.put(id, Holder.direct(modifier));
        ModifierRegistry.overwriteCacheForTest(registryCache);
        return modifier;
    }

    /** Stub a Modifier mock so onAttack records its id into the supplied list — order-sensitive
     *  iteration check. */
    private static void doRecord(Modifier mock, ResourceLocation id, List<ResourceLocation> order) {
        org.mockito.Mockito.doAnswer(invocation -> {
            order.add(id);
            return null;
        }).when(mock).onAttack(any(), anyInt(), any());
    }
}
