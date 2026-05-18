package slimeknights.sconstruct.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import slimeknights.sconstruct.common.data.TinkerDataComponents;
import slimeknights.sconstruct.common.data.ToolBroken;
import slimeknights.sconstruct.common.data.ToolMaterials;
import slimeknights.sconstruct.common.data.ToolModifiers;
import slimeknights.sconstruct.common.data.ToolStats;
import slimeknights.sconstruct.tools.material.Material;
import slimeknights.sconstruct.tools.modifier.ModifierHookDispatcher;

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
     * Single-source-of-truth stat rebuild for a tool stack. Resolves the stack's
     * {@link ToolMaterials} via the server-side {@link Material#REGISTRY_KEY} datapack
     * registry, runs {@link StatsBuilder#compute} for the supplied {@link ToolDefinition},
     * and writes the resulting snapshot back as three components: {@link
     * TinkerDataComponents#TOOL_STATS}, vanilla {@link DataComponents#MAX_DAMAGE}, and
     * vanilla {@link DataComponents#ATTRIBUTE_MODIFIERS} (built via {@link
     * AttributeBuilder#build}). Existing damage is clamped to the new {@code maxDurability}
     * so a recipe / cap-tier downgrade can't leave a tool with a damage value past its new
     * ceiling — vanilla would render the durability bar at a negative fill and the next
     * hit would underflow.
     *
     * <p>Server-only: the materials registry is server-authoritative and the cached stat
     * snapshot is network-synchronised down to clients via {@link
     * TinkerDataComponents#TOOL_STATS} — running the rebuild on the client thread would
     * either read a stale registry view (integrated server) or NPE on a missing one
     * (dedicated client). The {@link MinecraftServer#isSameThread} guard catches the
     * misuse at the call site rather than after a downstream registry NPE.
     *
     * <p>The private no-op {@code rebuildStats} hook called from {@link #setMaterials} and
     * {@link #addModifier} stays in place: those mutators are part of the SMTCON-74
     * façade and don't have a {@link MinecraftServer} / {@link ToolDefinition} in scope.
     * SMTCON-78's {@code ToolCore} will wire callers to invoke this public method
     * directly with the definition pinned on the item.
     *
     * @param stack the tool stack to rebuild — no-op when {@link ItemStack#isEmpty}.
     * @param server the running server (provides the materials {@link HolderLookup} and
     *     the thread-affinity check); must not be null.
     * @param definition the tool's part-slot / modifier-slot metadata used to drive the
     *     {@link StatsBuilder} aggregation; must not be null.
     * @throws IllegalStateException if invoked off the server thread.
     */
    public static void rebuildStats(ItemStack stack, MinecraftServer server, ToolDefinition definition) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(definition, "definition");
        if (!server.isSameThread()) {
            throw new IllegalStateException("ToolHelper.rebuildStats must be invoked on the server thread");
        }
        if (stack.isEmpty()) {
            return;
        }

        HolderLookup.RegistryLookup<Material> lookup = server.registryAccess().lookupOrThrow(Material.REGISTRY_KEY);
        List<ResourceLocation> ids = getMaterials(stack);
        List<Holder<Material>> materials = new ArrayList<>(ids.size());
        for (ResourceLocation id : ids) {
            // null entries fold into the StatsBuilder's wood fallback per the documented
            // contract — a missing material on a built tool produces meaningful baseline
            // stats rather than crashing the rebuild.
            materials.add(lookup.get(ResourceKey.create(Material.REGISTRY_KEY, id)).orElse(null));
        }

        ToolModifiers modifiers = stack.getOrDefault(TinkerDataComponents.TOOL_MODIFIERS.get(), ToolModifiers.empty());
        ToolStats computed = StatsBuilder.compute(materials, modifiers, definition);

        stack.set(TinkerDataComponents.TOOL_STATS.get(), computed);
        stack.set(DataComponents.MAX_DAMAGE, computed.maxDurability());
        // Subclass hook: melee variants (longsword, rapier) layer reach / attack-speed
        // boosts on top of the stats-driven baseline so per-class behaviour participates in
        // the same rebuild path as the generic stats.
        net.minecraft.world.item.component.ItemAttributeModifiers baseline = AttributeBuilder.build(computed);
        net.minecraft.world.item.component.ItemAttributeModifiers augmented = stack.getItem() instanceof slimeknights.sconstruct.tools.item.ToolCore tool ? tool.augmentAttributes(baseline, computed)
                : baseline;
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, augmented);

        // Damage value is held in vanilla DataComponents.DAMAGE (not our component map). A
        // stat recompute that lowers maxDurability under the current damage would otherwise
        // leave the tool past full damage — clamp so the broken-flag transition (driven by
        // damage == maxDurability) still triggers at the right moment.
        int currentDamage = stack.getDamageValue();
        int clampedDamage = Math.min(currentDamage, computed.maxDurability());
        if (clampedDamage != currentDamage) {
            stack.setDamageValue(clampedDamage);
        }

        // Re-evaluate the TOOL_BROKEN flag against the post-clamp damage / new maxDurability
        // so the cached broken state matches the new ceiling: a downgrade that drops the
        // ceiling to current damage marks the tool broken in the same tick; an upgrade that
        // lifts the ceiling above current damage un-breaks a previously-broken tool. Without
        // this resync the broken bit could lag the stat snapshot through a material swap and
        // leave a UI / damage-handler that branches on isBroken reading stale state.
        boolean shouldBeBroken = computed.maxDurability() > 0 && clampedDamage >= computed.maxDurability();
        stack.set(TinkerDataComponents.TOOL_BROKEN.get(), shouldBeBroken ? ToolBroken.BROKEN : ToolBroken.intact());

        // Stamp per-modifier persistent state via the onBuild hook now that the stat / broken
        // snapshot is fully resolved — modifiers that depend on the post-rebuild maxDurability
        // (mossy auto-repair seed, soulbound-owner stamp) read it back through the cached
        // TOOL_STATS component. The dispatcher's broken-state guard ensures a tool that just
        // flipped to broken doesn't run onBuild side effects against a zero-budget snapshot.
        ModifierHookDispatcher.dispatchOnBuild(stack);
    }

    /**
     * Stat-rebuild hook called whenever materials or modifiers change via the SMTCON-74
     * façade. Currently a no-op: the public {@link #rebuildStats(ItemStack, MinecraftServer,
     * ToolDefinition)} variant added in SMTCON-77 needs a {@link MinecraftServer} and a
     * {@link ToolDefinition} that neither {@link #setMaterials} nor {@link #addModifier}
     * have in scope. SMTCON-78's {@code ToolCore} will swap these callsites over to the
     * public method once the per-item definition is reachable.
     */
    private static void rebuildStats(ItemStack stack) {
        // TODO(SMTCON-78): replace with rebuildStats(stack, server, def) once ToolCore lands.
    }

    /** Repair restored per consumed item, as a fraction of {@code maxDurability}. Legacy 1.12
     *  parity: {@code TinkerToolEvent.OnToolRepair} also applied a quarter of max durability
     *  per item, so a stack of 4 fully repairs a tool from zero. */
    public static final float REPAIR_FRACTION_PER_ITEM = 0.25F;

    /**
     * Repair a tool stack by consuming items from a repair-material stack. Identifies the
     * tool's head material (the first part slot whose {@link PartType#isHead} returns true),
     * resolves its {@code Material} via the server-side {@link Material#REGISTRY_KEY} datapack
     * registry, and verifies the supplied {@code repairItem} sits in that material's
     * {@link Material#repairTag}. On a match, each consumed item restores
     * {@link #REPAIR_FRACTION_PER_ITEM} (25%) of {@code maxDurability} — the legacy 1.12
     * parity ratio — capped at the {@code repairItem} stack size and at fully repaired
     * (damage clamped to zero). The {@link TinkerDataComponents#TOOL_BROKEN} flag is cleared
     * whenever damage drops below {@code maxDurability}.
     *
     * <p>Returns the number of items consumed — callers (smeltery / tool-station UI) should
     * subsequently shrink the supplied {@code repairItem} stack by that amount.
     *
     * <p>Server-only: the materials registry is server-authoritative and the cached broken
     * snapshot is network-synchronised to clients. The {@link MinecraftServer#isSameThread}
     * guard surfaces off-thread misuse at the call site.
     *
     * @return items consumed; {@code 0} when no repair occurred (empty stack, no head slot,
     *     missing material, mismatched repair item, or tool already at full durability).
     * @throws IllegalStateException if invoked off the server thread.
     */
    public static int repair(ItemStack stack, ItemStack repairItem, MinecraftServer server, ToolDefinition definition) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(repairItem, "repairItem");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(definition, "definition");
        if (!server.isSameThread()) {
            throw new IllegalStateException("ToolHelper.repair must be invoked on the server thread");
        }
        if (stack.isEmpty() || repairItem.isEmpty()) {
            return 0;
        }
        int maxDurability = stack.getMaxDamage();
        int currentDamage = stack.getDamageValue();
        if (maxDurability <= 0 || currentDamage <= 0) {
            return 0;
        }

        ResourceLocation headMaterialId = findHeadMaterialId(stack, definition);
        if (headMaterialId == null) {
            return 0;
        }
        HolderLookup.RegistryLookup<Material> lookup = server.registryAccess().lookupOrThrow(Material.REGISTRY_KEY);
        Material material = lookup.get(ResourceKey.create(Material.REGISTRY_KEY, headMaterialId)).map(Holder::value).orElse(null);
        if (material == null) {
            return 0;
        }
        TagKey<Item> repairTag = material.repairTag().orElse(null);
        if (repairTag == null || !repairItem.is(repairTag)) {
            return 0;
        }

        int repairPerItem = Math.max(1, Math.round(maxDurability * REPAIR_FRACTION_PER_ITEM));
        int itemsNeededToFullyRepair = (currentDamage + repairPerItem - 1) / repairPerItem;
        int itemsConsumed = Math.min(itemsNeededToFullyRepair, repairItem.getCount());
        int repaired = Math.min(currentDamage, itemsConsumed * repairPerItem);
        int newDamage = currentDamage - repaired;

        stack.setDamageValue(newDamage);
        if (newDamage < maxDurability) {
            // Repair always clears the broken bit when the damage budget lifts above zero —
            // legacy 1.12 parity: even a single repair item lets the player swing the tool
            // again rather than requiring full restoration.
            stack.set(TinkerDataComponents.TOOL_BROKEN.get(), ToolBroken.intact());
        }
        return itemsConsumed;
    }

    /**
     * Walk the tool's {@link ToolMaterials} positionally and return the material id of the
     * first slot whose {@link PartType} is a head type. Tools with no head slot (arrows,
     * mattock — though mattock has an axe-head) return {@code null}; in that case repair has
     * no canonical material to bind to and the caller skips the repair tick.
     */
    @Nullable
    private static ResourceLocation findHeadMaterialId(ItemStack stack, ToolDefinition definition) {
        List<ResourceLocation> ids = getMaterials(stack);
        for (int i = 0; i < definition.getPartCount() && i < ids.size(); i++) {
            if (definition.getPartSlot(i).isHead()) {
                return ids.get(i);
            }
        }
        return null;
    }
}
