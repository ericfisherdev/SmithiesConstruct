package slimeknights.sconstruct.port1211.tools.item;

import java.util.Set;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;

import slimeknights.sconstruct.port1211.tools.ToolHelper;

/**
 * Pure-function helpers behind {@link ToolCore}'s vanilla overrides. Lives in its own class so
 * the broken-state guard, harvest-level ladder, and durability-tick math can be unit-tested
 * without standing up a NeoForge {@code Item} registry — constructing a real {@link ToolCore}
 * trips the {@code MappedRegistry} freeze in the bare-JVM test environment.
 *
 * <p>{@link ToolCore} delegates every override to one of the methods here, so the override
 * surface stays a one-liner and the testable logic stays addressable without reflection.
 */
public final class ToolBehavior {

    private ToolBehavior() {
    }

    /**
     * Resolve the destroy speed for a tool stack against a block. A broken tool reads {@code 0}
     * regardless of stats. Vanilla returns {@code 1.0F} for blocks outside the tool's mining
     * tag — keep that fall-through so a pickaxe mining dirt still gets the {@code 1.0F} pass
     * rather than the tool's full mining speed.
     *
     * @param stack the tool stack — broken state and stats are read off its component map.
     * @param vanillaSpeed the speed vanilla {@link net.minecraft.world.item.DiggerItem#getDestroySpeed}
     *     returns; the caller is responsible for invoking {@code super}.
     * @return the destroy speed to surface to the caller's vanilla override.
     */
    public static float resolveDestroySpeed(ItemStack stack, float vanillaSpeed) {
        if (ToolHelper.isBroken(stack)) {
            return 0.0F;
        }
        return vanillaSpeed > 1.0F ? ToolHelper.getStats(stack).miningSpeed() : vanillaSpeed;
    }

    /**
     * Resolve whether a tool stack is correct for a block. A broken tool is never correct.
     * Otherwise the vanilla "tool is in the right mining tag" answer is gated behind the
     * harvest-level ladder: stone-needs ⇒ {@code harvestLevel ≥ 1}, iron-needs ⇒
     * {@code harvestLevel ≥ 2}, diamond-needs ⇒ {@code harvestLevel ≥ 3}. Vanilla ships no
     * {@code NEEDS_NETHERITE_TOOL} tag — netherite blocks fall through to the diamond check.
     *
     * @param stack the tool stack.
     * @param state the block being mined.
     * @param vanillaCorrect the answer from {@code super.isCorrectToolForDrops}; the caller is
     *     responsible for invoking {@code super}.
     */
    public static boolean isCorrectForBlock(ItemStack stack, BlockState state, boolean vanillaCorrect) {
        if (ToolHelper.isBroken(stack)) {
            return false;
        }
        int harvestLevel = ToolHelper.getStats(stack).harvestLevel();
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL) && harvestLevel < 3) {
            return false;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL) && harvestLevel < 2) {
            return false;
        }
        if (state.is(BlockTags.NEEDS_STONE_TOOL) && harvestLevel < 1) {
            return false;
        }
        return vanillaCorrect;
    }

    /**
     * Whether a swing through {@link net.minecraft.world.item.DiggerItem#hurtEnemy} should
     * register. Broken tools decline the swing so neither the durability tick nor the
     * attack-cooldown sound fires.
     */
    public static boolean canHurtEnemy(ItemStack stack) {
        return !ToolHelper.isBroken(stack);
    }

    /**
     * Resolve {@link net.neoforged.neoforge.common.extensions.IItemExtension#canPerformAction
     * canPerformAction} against the tool's static {@link ItemAbility} set. Broken tools decline
     * every ability so vanilla's right-click handlers (axe-strip, shovel-flatten, sword-sweep,
     * hoe-till) short-circuit before the side effect runs.
     *
     * <p>The ability set is part of the {@code ToolDefinition} so it stays fixed per tool
     * subclass — abilities don't shift with materials or modifiers in legacy 1.12 either.
     */
    public static boolean canPerformAction(ItemStack stack, ItemAbility ability, Set<ItemAbility> definitionAbilities) {
        if (ToolHelper.isBroken(stack)) {
            return false;
        }
        return definitionAbilities.contains(ability);
    }

    /**
     * Run the durability-tick interceptor. A tick that would push {@code damageValue} to
     * {@code maxDamage} flips the {@link slimeknights.sconstruct.port1211.common.data.TinkerDataComponents#TOOL_BROKEN}
     * flag via {@link ToolHelper#setBroken}, pins {@code damageValue} at {@code maxDamage - 1}
     * (one tick below the shrink threshold so {@link ItemStack#hurtAndBreak} never reaches its
     * {@link ItemStack#shrink} branch), and returns {@code 0} so vanilla applies no further
     * damage on this tick. The persisting empty-durability tool stays in the inventory so it
     * can be repaired at the smeltery — vanilla's shrink-on-break behaviour would otherwise
     * erase the per-stack material / modifier state.
     *
     * <p>A zero {@code maxDamage} (un-built tool with no stats yet) short-circuits to the
     * pass-through branch so the broken bit doesn't latch on before the tool has any
     * durability budget.
     *
     * @return the amount of damage to surface to vanilla's {@code damageItem} caller.
     */
    public static int processDurabilityTick(ItemStack stack, int amount) {
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return amount;
        }
        int newDamage = stack.getDamageValue() + amount;
        if (newDamage >= max) {
            ToolHelper.setBroken(stack, true);
            stack.setDamageValue(max - 1);
            return 0;
        }
        return amount;
    }
}
