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

        // SMTCON-86: utility / specialty modifier roster. Metadata-only entries — the
        // per-modifier behaviour (silk-touch drop swap, beheading head-drop, smite / bane
        // situational damage, knockback enchantment stamp) is the follow-up integration; the
        // JSON registrations here let the tool-station UI surface the modifier and gate
        // application against the slot-cost / cap before the runtime hooks land.
        // Silk Touch: drop change for grass / leaves / glass, max level 1 (one-shot), 1 slot.
        context.register(key("silktouch"), new SimpleStatBoostType.Instance(rl("silktouch"), 1, 1));
        // Beheading: chance per level to drop the mob's head on kill, cap 3, 1 slot.
        context.register(key("beheading"), new SimpleStatBoostType.Instance(rl("beheading"), 3, 1));
        // Smite: bonus damage to undead, +2.5 per level, cap 5, 1 slot.
        context.register(key("smite"), new SimpleStatBoostType.Instance(rl("smite"), 5, 1));
        // Bane of Arthropods: bonus damage + slow on arthropods, +2.5 per level, cap 5, 1 slot.
        context.register(key("bane_of_arthropods"), new SimpleStatBoostType.Instance(rl("bane_of_arthropods"), 5, 1));
        // Knockback: +1 per level (vanilla parity), cap 2, 1 slot.
        context.register(key("knockback"), new SimpleStatBoostType.Instance(rl("knockback"), 2, 1));

        // SMTCON-87: effect-driven specialty modifier roster. Same metadata-only contract as
        // SMTCON-86 — the per-modifier behaviour (fire-aspect set-on-fire, necrotic siphon,
        // moss / auto-repair durability regen, mending XP absorb) is the follow-up integration
        // on the SMTCON-83 hooks dispatcher. The JSON entries here let the tool-station UI
        // surface each modifier and gate application against the cap and slot cost.
        // Fiery: set target on fire 5s per level on attack, cap 5, 1 slot (legacy 1.12 parity).
        context.register(key("fiery"), new SimpleStatBoostType.Instance(rl("fiery"), 5, 1));
        // Necrotic: heal attacker on hit, chance scales with level, one-shot, 1 slot.
        context.register(key("necrotic"), new SimpleStatBoostType.Instance(rl("necrotic"), 1, 1));
        // Moss: passive durability regen tied to day cycle, one-shot, 1 slot.
        context.register(key("moss"), new SimpleStatBoostType.Instance(rl("moss"), 1, 1));
        // Mending: XP absorb repairs durability, one-shot, 1 slot.
        context.register(key("mending"), new SimpleStatBoostType.Instance(rl("mending"), 1, 1));
        // Auto-Repair: passive durability regen at idle, cap 5, 1 slot.
        context.register(key("auto_repair"), new SimpleStatBoostType.Instance(rl("auto_repair"), 5, 1));
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, path);
    }

    private static ResourceKey<Modifier> key(String path) {
        return ResourceKey.create(Modifier.REGISTRY_KEY, rl(path));
    }
}
