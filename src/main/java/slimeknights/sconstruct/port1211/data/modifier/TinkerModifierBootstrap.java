package slimeknights.sconstruct.port1211.data.modifier;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import slimeknights.sconstruct.port1211.tools.modifier.Modifier;
import slimeknights.sconstruct.port1211.tools.modifier.SimpleStatBoostType;

/**
 * Bootstrap entries for the {@link Modifier} datapack registry. SMTCON-84 ships the canonical
 * Sharpness modifier (the +1.25 attack damage per level boost) as the first registered entry.
 * Subsequent SMTCON-85 → SMTCON-88 tickets add the rest of the legacy roster (haste, beheading,
 * silk-touch, …) by appending entries here.
 *
 * <p>Modifiers live in the {@code tconstruct} namespace (not {@code sconstruct}) so addons coded
 * against legacy modifier ids resolve against this roster without remapping — same cross-mod
 * compat rationale as {@link slimeknights.sconstruct.port1211.data.material.TinkerMaterialBootstrap}.
 */
public final class TinkerModifierBootstrap {

    private static final String LEGACY_NAMESPACE = "tconstruct";

    private TinkerModifierBootstrap() {
    }

    /** Bootstrap callback registered against {@link Modifier#REGISTRY_KEY} in the datagen
     *  wiring. */
    public static void bootstrap(BootstrapContext<Modifier> context) {
        // Sharpness: +1.25 attack damage per level, cap 5, 1 slot cost. Only the per-level
        // damage value is mirrored runtime-side — {@code StatsBuilder.SHARPNESS_DAMAGE_PER_LEVEL}
        // holds the +1.25 constant the {@code compute} method multiplies against the stack's
        // sharpness level. The {@code maxLevel} and {@code slotCost} arguments to
        // {@link SimpleStatBoostType.Instance} below are the JSON-side metadata only — they
        // are not mirrored as constants in {@code StatsBuilder} (the cap is enforced at
        // application time by the tool-station UI; the slot cost feeds the {@code freeModifiers}
        // math through the general {@code Σ levels} aggregation).
        context.register(key("sharpness"), new SimpleStatBoostType.Instance(rl("sharpness"), 5, 1));

        // SMTCON-85: vanilla-equivalent modifier roster. Per-level boost constants live in
        // StatsBuilder; the maxLevel / slotCost shown here are the JSON-side caps the
        // tool-station UI enforces.
        // Redstone: +0.05 mining speed per level, cap 50, 1 slot cost.
        context.register(key("redstone"), new SimpleStatBoostType.Instance(rl("redstone"), 50, 1));
        // Quartz: +0.5 attack damage per level, cap 5, 1 slot cost (sibling to sharpness).
        context.register(key("quartz"), new SimpleStatBoostType.Instance(rl("quartz"), 5, 1));
        // Lapis: vanilla-Fortune-equivalent on block drops, cap 3, 1 slot cost. The drop
        // integration is a follow-up — this entry registers the modifier metadata so the
        // tool-station UI can surface it.
        context.register(key("lapis"), new SimpleStatBoostType.Instance(rl("lapis"), 3, 1));
        // Diamond: +500 max durability, max level 1 (one-shot), 2 slot cost.
        context.register(key("diamond"), new SimpleStatBoostType.Instance(rl("diamond"), 1, 2));
        // Emerald: +1 free modifier slot, max level 1 (one-shot), 1 slot cost. Lifts the
        // modifier-slot ceiling so a downstream application can land on a tool that would
        // otherwise have hit the default-3 baseline.
        context.register(key("emerald"), new SimpleStatBoostType.Instance(rl("emerald"), 1, 1));
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, path);
    }

    private static ResourceKey<Modifier> key(String path) {
        return ResourceKey.create(Modifier.REGISTRY_KEY, rl(path));
    }
}
