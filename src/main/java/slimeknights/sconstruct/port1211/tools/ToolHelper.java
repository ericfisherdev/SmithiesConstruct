package slimeknights.sconstruct.port1211.tools;

import java.util.List;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.common.data.ToolBroken;
import slimeknights.sconstruct.port1211.common.data.ToolMaterials;
import slimeknights.sconstruct.port1211.common.data.ToolModifiers;
import slimeknights.sconstruct.port1211.common.data.ToolStats;

/**
 * Read / write façade over the five tool {@link net.minecraft.core.component.DataComponentType
 * DataComponentTypes} registered in {@link TinkerDataComponents}. {@code ToolCore}, modifier
 * code, and event handlers go through this class rather than poking
 * {@link ItemStack#get}/{@link ItemStack#set} directly — keeps the component keys hidden behind
 * a single migration surface (DIP) and gives every mutator a uniform place to drive the stat
 * rebuild and broken-state side effects.
 *
 * <p>Accessor contract for {@link ItemStack#isEmpty() empty} stacks and stacks missing the
 * relevant component: return the same sentinel a freshly-zeroed tool would (empty parts list,
 * zero stats, no modifier levels, not broken). Callers can iterate over an ItemStack stream
 * blindly without an extra {@code isEmpty()} guard — the helpers absorb that check.
 *
 * <p>Mutators are no-ops on empty stacks. Component records are immutable, so every mutator
 * builds a fresh record and {@code stack.set}s it — the caller's reference to the previous
 * record (returned from an earlier accessor call) remains valid. {@link #setMaterials} and
 * {@link #addModifier} additionally drive {@link #rebuildStats}, which will be wired to the
 * StatsBuilder pipeline in <a href="https://ericfisherdev.atlassian.net/browse/SMTCON-77">SMTCON-77</a>
 * once <a href="https://ericfisherdev.atlassian.net/browse/SMTCON-75">SMTCON-75</a> ships.
 */
public final class ToolHelper {

    private ToolHelper() {
    }

    // ----- Accessors (read-only; empty / missing-component stacks return defaults) -----

    /**
     * Ordered material list for the stack, one entry per tool part slot. Empty list for empty
     * stacks or stacks without {@link TinkerDataComponents#TOOL_MATERIALS} attached — the
     * caller iterates blindly without a null-check.
     */
    public static List<ResourceLocation> getMaterials(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        ToolMaterials materials = stack.get(TinkerDataComponents.TOOL_MATERIALS.get());
        return materials == null ? List.of() : materials.parts();
    }

    /**
     * Modifier level for the given id, or {@code 0} if the modifier isn't present (or the
     * stack is empty / has no {@link TinkerDataComponents#TOOL_MODIFIERS} component). Zero is
     * the legacy "not applied" sentinel — modifier code branches on {@code level > 0}.
     */
    public static int getModifierLevel(ItemStack stack, ResourceLocation modId) {
        Objects.requireNonNull(modId, "modId");
        if (stack.isEmpty()) {
            return 0;
        }
        ToolModifiers modifiers = stack.get(TinkerDataComponents.TOOL_MODIFIERS.get());
        if (modifiers == null) {
            return 0;
        }
        return modifiers.levels().getOrDefault(modId, 0);
    }

    /**
     * Cached fully-resolved stat snapshot. {@link ToolStats#zero()} for empty / un-built
     * stacks — every numeric field is {@code 0} so consumers can dereference fields without
     * branching on the missing-component case.
     */
    public static ToolStats getStats(ItemStack stack) {
        if (stack.isEmpty()) {
            return ToolStats.zero();
        }
        ToolStats stats = stack.get(TinkerDataComponents.TOOL_STATS.get());
        return stats == null ? ToolStats.zero() : stats;
    }

    /**
     * Whether the tool is currently broken. {@code false} for empty stacks or stacks missing
     * the broken-flag component — the legacy "intact unless proven broken" default.
     */
    public static boolean isBroken(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ToolBroken broken = stack.get(TinkerDataComponents.TOOL_BROKEN.get());
        return broken != null && broken.broken();
    }

    // ----- Mutators (no-op on empty stacks; always set a fresh immutable record) -----

    /**
     * Replace the stack's material list and trigger a stat rebuild. The parts list is
     * defensively copied via {@link ToolMaterials}' compact constructor, so a downstream alias
     * mutation on the caller's list can't leak into the stack's component state.
     */
    public static void setMaterials(ItemStack stack, List<ResourceLocation> parts) {
        if (stack.isEmpty()) {
            return;
        }
        Objects.requireNonNull(parts, "parts");
        stack.set(TinkerDataComponents.TOOL_MATERIALS.get(), new ToolMaterials(parts));
        rebuildStats(stack);
    }

    /**
     * Set / overwrite the level for a modifier id and trigger a stat rebuild. Builds a new
     * {@link ToolModifiers} via {@link ToolModifiers#with(ResourceLocation, int)} so the
     * existing record is not mutated and the insertion-order contract is preserved.
     *
     * <p>Level is bounded at {@code 0} — a negative level is a calling-code bug (modifier code
     * branches on {@code level > 0} as the "applied" sentinel) and throws so the call site is
     * the failure point rather than a silent downstream miscount. Level {@code 0} stores
     * explicitly: {@link #getModifierLevel} reads it back as {@code 0}, which downstream code
     * treats as "not applied" — keeps the insertion-order slot reserved without re-arming the
     * modifier.
     */
    public static void addModifier(ItemStack stack, ResourceLocation modId, int level) {
        if (stack.isEmpty()) {
            return;
        }
        Objects.requireNonNull(modId, "modId");
        if (level < 0) {
            throw new IllegalArgumentException("modifier level must be non-negative (got " + level + " for " + modId + ")");
        }
        ToolModifiers current = stack.getOrDefault(TinkerDataComponents.TOOL_MODIFIERS.get(), ToolModifiers.empty());
        stack.set(TinkerDataComponents.TOOL_MODIFIERS.get(), current.with(modId, level));
        rebuildStats(stack);
    }

    /**
     * Flip the broken bit. Routes through the {@link ToolBroken#BROKEN}/{@link ToolBroken#intact()}
     * singletons so the cheap-to-allocate path doesn't garbage every durability tick.
     */
    public static void setBroken(ItemStack stack, boolean broken) {
        if (stack.isEmpty()) {
            return;
        }
        stack.set(TinkerDataComponents.TOOL_BROKEN.get(), broken ? ToolBroken.BROKEN : ToolBroken.intact());
    }

    /**
     * Stat-rebuild hook called whenever materials or modifiers change. SMTCON-75 introduces
     * {@code StatsBuilder} and SMTCON-77 wires this method to invoke it — currently a no-op so
     * SMTCON-74 can ship its accessor / mutator surface without a forward-reference to code
     * that doesn't yet exist.
     */
    private static void rebuildStats(ItemStack stack) {
        // TODO(SMTCON-77): replace with StatsBuilder.rebuild(stack) once SMTCON-75 ships.
    }
}
