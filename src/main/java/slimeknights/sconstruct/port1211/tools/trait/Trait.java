package slimeknights.sconstruct.port1211.tools.trait;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.data.ToolStats;

/**
 * The contract every Smithies' Construct trait implements. Traits are auto-applying modifiers
 * — they're attached to a {@code Material} (via {@code MaterialTrait}) rather than applied by
 * the player at the tool station, and they cost zero modifier slots. A trait's stat
 * contribution is applied by {@link slimeknights.sconstruct.port1211.tools.StatsBuilder#compute}
 * at rebuild time via {@link #applyStats}; trait classes that need a per-tick or per-event
 * side effect (autosmelt, stonebound's mining-speed ramp) layer that on top in a future
 * dispatcher ticket — this minimal contract covers the stat-only majority of legacy 1.12
 * traits.
 *
 * <p>The datapack registry key is reserved here so the SMTCON-104 follow-up that introduces
 * datapack-driven traits doesn't have to relocate the constant. The current implementation
 * holds traits in {@link TraitRegistry} as code-side singletons; the datapack registry is wired
 * by {@link TraitRegistry#register}.
 */
public interface Trait {

    /** Datapack registry key for {@code sconstruct:trait}. */
    ResourceKey<Registry<Trait>> REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "trait"));

    /** Stable id used by {@link slimeknights.sconstruct.port1211.tools.material.MaterialTrait}
     *  to refer to this trait grant. */
    ResourceLocation id();

    /**
     * Apply the trait's stat contribution. The default returns {@code stats} unchanged for
     * traits that have no stat impact (autosmelt, crude, cheap, duritos — those layer
     * behaviour at event hooks rather than the stat snapshot). Stat-bearing traits override
     * to return a new {@link ToolStats} record with the contribution folded in.
     */
    default ToolStats applyStats(ToolStats stats) {
        return stats;
    }
}
