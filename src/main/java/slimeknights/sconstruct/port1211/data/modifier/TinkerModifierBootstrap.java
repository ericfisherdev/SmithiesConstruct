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
        // Sharpness: +1.25 attack damage per level, cap 5, 1 slot cost. The numeric values are
        // mirrored as constants in {@link slimeknights.sconstruct.port1211.tools.StatsBuilder}
        // so a future shift in the per-level value lands in both the JSON-side metadata and
        // the runtime stat math at once.
        context.register(key("sharpness"), new SimpleStatBoostType.Instance(rl("sharpness"), 5, 1));
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, path);
    }

    private static ResourceKey<Modifier> key(String path) {
        return ResourceKey.create(Modifier.REGISTRY_KEY, rl(path));
    }
}
