package slimeknights.sconstruct.port1211.tools.modifier;

import java.util.Map;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.common.data.ToolModifiers;
import slimeknights.sconstruct.port1211.tools.ToolHelper;

/**
 * Routes {@link Modifier} lifecycle hooks for a built tool stack. Iterates the stack's
 * {@link ToolModifiers} map in insertion order — matching the legacy 1.12 contract where
 * tooltip render order and "first-match wins" tie-breakers depend on a stable iteration —
 * resolves each modifier id against the server-side {@link ModifierRegistry} cache, and invokes
 * the matching hook with the per-stack level.
 *
 * <p>Broken tools short-circuit every dispatch: a tool with {@code TOOL_BROKEN=true} has no
 * active modifiers. This mirrors the broken-state guards on {@link ToolHelper#getStats},
 * {@link slimeknights.sconstruct.port1211.tools.item.ToolBehavior#resolveDestroySpeed}, and
 * {@link slimeknights.sconstruct.port1211.tools.item.ToolBehavior#canHurtEnemy} so the
 * "broken tool acts as a stick" contract holds across every code path.
 *
 * <p>The dispatcher itself is server-side only — {@link Modifier} hooks may mutate inventories,
 * spawn projectiles, and read off the {@link ModifierRegistry} cache that's only populated on
 * the server. The {@link ToolEvents} context records each carry the {@code Level} the hook
 * fired in; the dispatcher skips the iteration when {@link net.minecraft.world.level.Level#isClientSide}
 * is true so a client-side hurtEnemy / mineBlock tick doesn't double-fire on the client.
 *
 * <p>Unknown modifier ids — JSON entries that referenced a {@link Modifier} subsequently
 * removed from the registry, or stacks built against an older datapack — are silently skipped
 * rather than throwing, so a registry-desync between save and reload doesn't crash the swing
 * tick. The corresponding entry stays in {@link ToolModifiers#levels} so the next reload that
 * brings the modifier back resumes its hooks where they were.
 */
public final class ModifierHookDispatcher {

    private ModifierHookDispatcher() {
    }

    /**
     * Route {@link Modifier#onAttack} to every applied modifier in insertion order. No-op when
     * the stack is broken, when the hook fired on the client side, or when the modifier roster
     * is empty.
     */
    public static void dispatchOnAttack(ItemStack stack, ToolEvents.OnHitContext context) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(context, "context");
        if (context.level().isClientSide() || ToolHelper.isBroken(stack)) {
            return;
        }
        forEachAppliedModifier(stack, (modifier, level) -> modifier.onAttack(stack, level, context));
    }

    /**
     * Route {@link Modifier#onMine} to every applied modifier in insertion order. Same
     * short-circuit contract as {@link #dispatchOnAttack}.
     */
    public static void dispatchOnMine(ItemStack stack, ToolEvents.OnMineContext context) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(context, "context");
        if (context.level().isClientSide() || ToolHelper.isBroken(stack)) {
            return;
        }
        forEachAppliedModifier(stack, (modifier, level) -> modifier.onMine(stack, level, context));
    }

    /**
     * Route {@link Modifier#onBuild} to every applied modifier in insertion order. Invoked from
     * {@link ToolHelper#rebuildStats} after the stat / attribute components are written so
     * onBuild listeners stamp their persistent state against the freshly-computed snapshot.
     *
     * <p>No level / no-context — onBuild is the per-application stamping hook; modifiers that
     * need to know whether they were just added vs. an existing level changed branch on the
     * persistent-data slot they own, not on a callback argument.
     */
    public static void dispatchOnBuild(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (ToolHelper.isBroken(stack)) {
            return;
        }
        forEachAppliedModifier(stack, (modifier, level) -> modifier.onBuild(stack, level));
    }

    /**
     * Walk the stack's {@link ToolModifiers} in insertion order, look up each id against
     * {@link ModifierRegistry}, and invoke {@code action} for every successfully-resolved
     * pair. Modifiers at {@code level == 0} are skipped per the SMTCON-82 contract ("level 0
     * is the not-applied sentinel"), and unknown ids are silently dropped so a registry
     * desync doesn't crash the dispatch.
     */
    private static void forEachAppliedModifier(ItemStack stack, ModifierHook action) {
        if (stack.isEmpty()) {
            return;
        }
        ToolModifiers modifiers = stack.getOrDefault(TinkerDataComponents.TOOL_MODIFIERS.get(), ToolModifiers.empty());
        for (Map.Entry<ResourceLocation, Integer> entry : modifiers.levels().entrySet()) {
            int level = entry.getValue();
            if (level <= 0) {
                continue;
            }
            ModifierRegistry.lookup(entry.getKey()).ifPresent(modifier -> action.invoke(modifier, level));
        }
    }

    /** Functional handle for the per-modifier callback inside {@link #forEachAppliedModifier}. */
    @FunctionalInterface
    private interface ModifierHook {
        void invoke(Modifier modifier, int level);
    }
}
