package slimeknights.sconstruct.port1211.tools.trait;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

import slimeknights.sconstruct.port1211.common.data.ToolStats;
import slimeknights.sconstruct.port1211.tools.material.MaterialTrait;

/**
 * Code-side trait registry. Mirrors {@code ModifierRegistry} in shape but holds traits as
 * static singletons rather than datapack JSON entries — every trait the mod ships is wired
 * here at class-load time, so a registry rebuild on every {@code OnDatapackSyncEvent} is
 * unnecessary. The SMTCON-104 follow-up that adds datapack-defined traits can replace this
 * hardcoded map with the same atomic-snapshot pattern {@code ModifierRegistry} uses without
 * changing the caller contract: {@link #lookup} stays the read surface.
 *
 * <p>{@link #applyAll} is the SMTCON-103 acceptance criterion: every material-granted trait's
 * stat contribution is folded into the supplied {@link ToolStats} snapshot at rebuild time.
 * Unknown trait ids are silently skipped — a datapack desync between the world's material
 * registry and the trait registry would otherwise turn every rebuild into a crash.
 */
public final class TraitRegistry {

    private static final Map<ResourceLocation, Trait> ENTRIES;

    static {
        Map<ResourceLocation, Trait> entries = new LinkedHashMap<>();
        registerEach(entries, Traits.AUTOSMELT, Traits.ECOLOGICAL, Traits.STONEBOUND, Traits.JAGGED, Traits.CRUDE, Traits.CHEAP, Traits.DENSE, Traits.DURITOS);
        ENTRIES = Map.copyOf(entries);
    }

    private TraitRegistry() {
    }

    private static void registerEach(Map<ResourceLocation, Trait> sink, Trait... traits) {
        for (Trait trait : traits) {
            if (sink.put(trait.id(), trait) != null) {
                throw new IllegalStateException("duplicate trait id: " + trait.id());
            }
        }
    }

    /** Resolve a trait by its id. Returns {@link Optional#empty} for unknown ids. */
    public static Optional<Trait> lookup(ResourceLocation id) {
        return Optional.ofNullable(ENTRIES.get(id));
    }

    /** Read-only view of every trait the mod ships. Used by datagen and unit tests. */
    public static List<Trait> all() {
        return List.copyOf(ENTRIES.values());
    }

    /**
     * Fold every material-granted trait's stat contribution into {@code stats}. Iteration order
     * matches the trait grant order on the material — a stable order keeps stat math
     * deterministic across reloads. Unknown trait ids (datapack drift) are skipped silently.
     */
    public static ToolStats applyAll(ToolStats stats, List<MaterialTrait> grants) {
        ToolStats current = stats;
        for (MaterialTrait grant : grants) {
            Optional<Trait> trait = lookup(grant.traitId());
            if (trait.isPresent()) {
                current = trait.get().applyStats(current);
            }
        }
        return current;
    }
}
