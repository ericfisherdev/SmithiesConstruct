package slimeknights.sconstruct.port1211.tools.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.common.data.ToolBroken;
import slimeknights.sconstruct.port1211.common.data.ToolStats;

/**
 * Pinned-behaviour tests for the {@link ToolBehavior} helpers behind {@link ToolCore}'s vanilla
 * overrides. Covers the broken-state guard on every code path (zero destroy speed, no drops, no
 * swing), the harvest-level ladder gating {@code isCorrectForBlock} against
 * {@link BlockTags#NEEDS_STONE_TOOL}/{@link BlockTags#NEEDS_IRON_TOOL}/
 * {@link BlockTags#NEEDS_DIAMOND_TOOL}, and the durability-tick interceptor's "mark broken one
 * tick below the shrink threshold" contract.
 */
class ToolBehaviorTest {

    // ----------------------------------------------------------- resolveDestroySpeed

    @Test
    void resolveDestroySpeedReturnsZeroOnBrokenTool() {
        ItemStack stack = brokenStack();
        assertEquals(0.0F, ToolBehavior.resolveDestroySpeed(stack, 6.0F));
    }

    @Test
    void resolveDestroySpeedReturnsVanillaForBlocksOutsideMiningTag() {
        // Vanilla returns 1.0F when the block isn't in the tool's mining tag — keep that
        // fall-through so a pickaxe mining dirt doesn't full-speed-mine non-pickaxe blocks.
        ItemStack stack = intactStackWithStats(new ToolStats(50, 2.0F, 0.0F, 6.0F, 0, 3, 0.0F, 0.0F, 0.0F));
        assertEquals(1.0F, ToolBehavior.resolveDestroySpeed(stack, 1.0F));
    }

    @Test
    void resolveDestroySpeedReplacesTierBaselineWithStatsMiningSpeedWhenBlockMatches() {
        // Super speed > 1.0F means the block is in the mining tag; replace the vanilla tier
        // speed (2.0F for wood-tier baseline) with the stats-driven mining speed.
        ItemStack stack = intactStackWithStats(new ToolStats(50, 2.0F, 0.0F, 6.0F, 0, 3, 0.0F, 0.0F, 0.0F));
        assertEquals(6.0F, ToolBehavior.resolveDestroySpeed(stack, 2.0F));
    }

    // ----------------------------------------------------------- isCorrectForBlock

    @Test
    void isCorrectForBlockReturnsFalseOnBrokenTool() {
        ItemStack stack = brokenStack();
        BlockState state = mock(BlockState.class);
        assertFalse(ToolBehavior.isCorrectForBlock(stack, state, true));
    }

    @Test
    void isCorrectForBlockRejectsDiamondBlockBelowTier3() {
        ItemStack stack = intactStackWithStats(new ToolStats(50, 2.0F, 0.0F, 6.0F, 2, 3, 0.0F, 0.0F, 0.0F));
        BlockState state = mock(BlockState.class);
        when(state.is(BlockTags.NEEDS_DIAMOND_TOOL)).thenReturn(true);
        assertFalse(ToolBehavior.isCorrectForBlock(stack, state, true));
    }

    @Test
    void isCorrectForBlockAllowsDiamondBlockAtTier3() {
        ItemStack stack = intactStackWithStats(new ToolStats(50, 2.0F, 0.0F, 6.0F, 3, 3, 0.0F, 0.0F, 0.0F));
        BlockState state = mock(BlockState.class);
        when(state.is(BlockTags.NEEDS_DIAMOND_TOOL)).thenReturn(true);
        assertTrue(ToolBehavior.isCorrectForBlock(stack, state, true));
    }

    @Test
    void isCorrectForBlockRejectsIronBlockBelowTier2() {
        ItemStack stack = intactStackWithStats(new ToolStats(50, 2.0F, 0.0F, 6.0F, 1, 3, 0.0F, 0.0F, 0.0F));
        BlockState state = mock(BlockState.class);
        when(state.is(BlockTags.NEEDS_IRON_TOOL)).thenReturn(true);
        assertFalse(ToolBehavior.isCorrectForBlock(stack, state, true));
    }

    @Test
    void isCorrectForBlockRejectsStoneBlockBelowTier1() {
        ItemStack stack = intactStackWithStats(new ToolStats(50, 2.0F, 0.0F, 6.0F, 0, 3, 0.0F, 0.0F, 0.0F));
        BlockState state = mock(BlockState.class);
        when(state.is(BlockTags.NEEDS_STONE_TOOL)).thenReturn(true);
        assertFalse(ToolBehavior.isCorrectForBlock(stack, state, true));
    }

    @Test
    void isCorrectForBlockDefersToVanillaWhenLadderPasses() {
        // Above every tag floor — pass-through to the vanilla "tool is in the mining tag"
        // answer. If vanilla says false, we say false too.
        ItemStack stack = intactStackWithStats(new ToolStats(50, 2.0F, 0.0F, 6.0F, 3, 3, 0.0F, 0.0F, 0.0F));
        BlockState state = mock(BlockState.class);
        assertFalse(ToolBehavior.isCorrectForBlock(stack, state, false));
    }

    // ----------------------------------------------------------- canHurtEnemy

    @Test
    void canHurtEnemyReturnsFalseOnBrokenTool() {
        assertFalse(ToolBehavior.canHurtEnemy(brokenStack()));
    }

    @Test
    void canHurtEnemyReturnsTrueOnIntactTool() {
        assertTrue(ToolBehavior.canHurtEnemy(intactStackWithStats(ToolStats.zero())));
    }

    // ----------------------------------------------------------- canPerformAction

    @Test
    void canPerformActionReturnsFalseOnBrokenTool() {
        // Every right-click handler must short-circuit when the tool is broken — axe-strip,
        // shovel-flatten, sword-sweep, hoe-till. The definition set is irrelevant in this case.
        ItemStack stack = brokenStack();
        assertFalse(ToolBehavior.canPerformAction(stack, ItemAbilities.AXE_STRIP, Set.of(ItemAbilities.AXE_STRIP)));
    }

    @Test
    void canPerformActionReturnsTrueWhenDefinitionDeclaresIt() {
        ItemStack stack = intactStackWithStats(ToolStats.zero());
        assertTrue(ToolBehavior.canPerformAction(stack, ItemAbilities.PICKAXE_DIG, Set.of(ItemAbilities.PICKAXE_DIG)));
    }

    @Test
    void canPerformActionReturnsFalseWhenDefinitionOmitsIt() {
        // A pickaxe doesn't strip logs — the pickaxe definition's ability set won't contain
        // AXE_STRIP, so the answer is false even on an intact tool.
        ItemStack stack = intactStackWithStats(ToolStats.zero());
        assertFalse(ToolBehavior.canPerformAction(stack, ItemAbilities.AXE_STRIP, Set.of(ItemAbilities.PICKAXE_DIG)));
    }

    // ----------------------------------------------------------- processDurabilityTick

    @Test
    void processDurabilityTickMarksToolBrokenAtMaxAndReturnsZero() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.getMaxDamage()).thenReturn(50);
        when(stack.getDamageValue()).thenReturn(49);

        int leftover = ToolBehavior.processDurabilityTick(stack, 1);

        assertEquals(0, leftover, "vanilla must see zero remaining damage so it doesn't shrink the stack");
        verify(stack).set(eq(TinkerDataComponents.TOOL_BROKEN.get()), eq(ToolBroken.BROKEN));
        verify(stack).setDamageValue(49);
    }

    @Test
    void processDurabilityTickAbsorbsOverflowDamageOnLargeHit() {
        // A burst hit (lava-bucket, fall-damage, etc.) that would push damage past max in one
        // tick must still clamp at max - 1 rather than letting vanilla shrink.
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.getMaxDamage()).thenReturn(50);
        when(stack.getDamageValue()).thenReturn(10);

        int leftover = ToolBehavior.processDurabilityTick(stack, 100);

        assertEquals(0, leftover);
        verify(stack).set(eq(TinkerDataComponents.TOOL_BROKEN.get()), eq(ToolBroken.BROKEN));
        verify(stack).setDamageValue(49);
    }

    @Test
    void processDurabilityTickPassesThroughBelowMax() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.getMaxDamage()).thenReturn(50);
        when(stack.getDamageValue()).thenReturn(10);

        int leftover = ToolBehavior.processDurabilityTick(stack, 1);

        assertEquals(1, leftover);
        verify(stack, never()).set(any(DataComponentType.class), any());
        verify(stack, never()).setDamageValue(anyInt());
    }

    @Test
    void processDurabilityTickPassesThroughWhenMaxDamageIsZero() {
        // Un-built tool with no stats yet — broken bit must not latch on before the tool has a
        // durability budget. Without this guard the freshly-spawned creative tab item would
        // read as broken.
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.getMaxDamage()).thenReturn(0);

        int leftover = ToolBehavior.processDurabilityTick(stack, 5);

        assertEquals(5, leftover);
        verify(stack, never()).set(any(DataComponentType.class), any());
    }

    // -------------------------------------------------------------------------- helpers

    private static ItemStack brokenStack() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.get(TinkerDataComponents.TOOL_BROKEN.get())).thenReturn(ToolBroken.BROKEN);
        return stack;
    }

    private static ItemStack intactStackWithStats(ToolStats stats) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.get(TinkerDataComponents.TOOL_BROKEN.get())).thenReturn(ToolBroken.intact());
        when(stack.get(TinkerDataComponents.TOOL_STATS.get())).thenReturn(stats);
        return stack;
    }
}
