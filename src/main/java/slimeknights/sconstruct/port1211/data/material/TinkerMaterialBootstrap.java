package slimeknights.sconstruct.port1211.data.material;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import slimeknights.sconstruct.port1211.tools.PartType;
import slimeknights.sconstruct.port1211.tools.material.ArrowStats;
import slimeknights.sconstruct.port1211.tools.material.BowStats;
import slimeknights.sconstruct.port1211.tools.material.ExtraStats;
import slimeknights.sconstruct.port1211.tools.material.HandleStats;
import slimeknights.sconstruct.port1211.tools.material.HeadStats;
import slimeknights.sconstruct.port1211.tools.material.Material;
import slimeknights.sconstruct.port1211.tools.material.MaterialStats;

/**
 * Bootstrap entries for the {@link Material} datapack registry. Emits the base material roster
 * the tool defaults need — fifteen entries spanning the legacy 1.12 stat baseline: vanilla-tier
 * (wood/stone/iron/gold), specialty (flint/bone/paper/slime/blueslime), and Smeltery-tier
 * metals (cobalt/ardite/manyullyn/copper/silver/steel).
 *
 * <p>Materials live in the {@code tconstruct} namespace (not {@code sconstruct}) so addons
 * coded against legacy material ids resolve against this roster without remapping. Stat values
 * mirror legacy {@code TinkerMaterials.java} within the precision that survives the
 * {@code float} round-trip; tiers preserve the {@code wood=0, stone=1, iron=2, cobalt=4} ladder
 * the legacy AC pinned.
 */
public final class TinkerMaterialBootstrap {

    private static final String LEGACY_NAMESPACE = "tconstruct";

    private TinkerMaterialBootstrap() {
    }

    /** Bootstrap callback registered against {@link Material#REGISTRY_KEY} in the datagen wiring. */
    public static void bootstrap(BootstrapContext<Material> context) {
        // Vanilla-tier baseline ──────────────────────────────────────────────────────────────
        context.register(key("wood"), new Material(rl("wood"), 0, tag(ItemTags.PLANKS),
                stats(new HeadStats(35, 0, 2.0f, 2.0f), new HandleStats(1.0f, 1.0f, 1.0f), new ExtraStats(15), new BowStats(20, 1.0f, 0.0f), new ArrowStats(0.7f, 0)), List.of(), 0xFF8B5A2B));
        context.register(key("stone"), new Material(rl("stone"), 1, tag(ItemTags.STONE_TOOL_MATERIALS),
                stats(new HeadStats(120, 1, 4.0f, 3.0f), new HandleStats(0.6f, 1.0f, 0.8f), new ExtraStats(20), new BowStats(0, 0.0f, 0.0f), new ArrowStats(2.5f, 0)), List.of(), 0xFF888888));
        context.register(key("iron"), new Material(rl("iron"), 2, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/iron"))),
                stats(new HeadStats(250, 2, 6.0f, 4.0f), new HandleStats(1.0f, 1.0f, 1.0f), new ExtraStats(25), new BowStats(35, 1.1f, 0.5f), new ArrowStats(1.5f, 10)), List.of(), 0xFFD8D8D8));
        context.register(key("gold"), new Material(rl("gold"), 0, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/gold"))),
                stats(new HeadStats(32, 0, 12.0f, 2.0f), new HandleStats(1.3f, 1.4f, 1.2f), new ExtraStats(10), new BowStats(25, 1.2f, 0.0f), new ArrowStats(2.5f, 0)), List.of(), 0xFFFFE060));

        // Specialty (Phase-2 materials) ───────────────────────────────────────────────────────
        context.register(key("flint"), new Material(rl("flint"), 1, Optional.empty(),
                stats(new HeadStats(170, 1, 5.0f, 3.5f), new HandleStats(0.7f, 0.9f, 0.8f), new ExtraStats(15), new BowStats(0, 0.0f, 0.0f), new ArrowStats(1.0f, 0)), List.of(), 0xFF606060));
        context.register(key("bone"), new Material(rl("bone"), 1, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "bones"))),
                stats(new HeadStats(200, 1, 5.0f, 3.0f), new HandleStats(1.1f, 1.1f, 0.9f), new ExtraStats(20), new BowStats(25, 1.0f, 0.0f), new ArrowStats(0.7f, 5)), List.of(), 0xFFEED4A6));
        context.register(key("paper"), new Material(rl("paper"), 1, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "paper"))),
                stats(new HeadStats(30, 0, 3.0f, 1.0f), new HandleStats(0.3f, 1.0f, 1.0f), new ExtraStats(5), new BowStats(35, 0.8f, 0.0f), new ArrowStats(0.2f, 0)), List.of(), 0xFFFFFFFF));
        context.register(key("slime"), new Material(rl("slime"), 1, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "slimeballs/green"))),
                stats(new HeadStats(500, 0, 3.0f, 2.0f), new HandleStats(1.0f, 1.3f, 1.0f), new ExtraStats(40), new BowStats(0, 0.0f, 0.0f), new ArrowStats(0.5f, 10)), List.of(), 0xFF82C873));
        context.register(key("blueslime"), new Material(rl("blueslime"), 1, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "slimeballs/blue"))),
                stats(new HeadStats(800, 0, 3.5f, 2.0f), new HandleStats(1.0f, 1.4f, 1.0f), new ExtraStats(60), new BowStats(0, 0.0f, 0.0f), new ArrowStats(0.5f, 15)), List.of(), 0xFF63ACEC));

        // Smeltery-tier metals (Phase-5 materials, pre-declared so tool defaults resolve) ─────
        context.register(key("cobalt"), new Material(rl("cobalt"), 4, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/cobalt"))),
                stats(new HeadStats(800, 4, 12.0f, 3.0f), new HandleStats(1.2f, 1.2f, 1.0f), new ExtraStats(50), new BowStats(50, 1.2f, 0.5f), new ArrowStats(1.0f, 20)), List.of(), 0xFF2376DD));
        context.register(key("ardite"), new Material(rl("ardite"), 4, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/ardite"))),
                stats(new HeadStats(1100, 4, 8.0f, 4.0f), new HandleStats(1.4f, 0.7f, 0.9f), new ExtraStats(70), new BowStats(60, 0.9f, 1.0f), new ArrowStats(2.0f, 30)), List.of(), 0xFFE45E0D));
        context.register(key("manyullyn"), new Material(rl("manyullyn"), 5, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/manyullyn"))),
                stats(new HeadStats(900, 5, 9.0f, 6.0f), new HandleStats(1.2f, 1.0f, 1.0f), new ExtraStats(60), new BowStats(45, 1.2f, 1.0f), new ArrowStats(1.5f, 30)), List.of(), 0xFFAF6CC1));
        context.register(key("copper"), new Material(rl("copper"), 2, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/copper"))),
                stats(new HeadStats(210, 2, 5.0f, 3.0f), new HandleStats(1.1f, 1.1f, 1.0f), new ExtraStats(20), new BowStats(30, 1.0f, 0.0f), new ArrowStats(1.3f, 8)), List.of(), 0xFFE0834D));
        context.register(key("silver"), new Material(rl("silver"), 3, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/silver"))),
                stats(new HeadStats(330, 3, 7.0f, 3.5f), new HandleStats(1.1f, 1.0f, 1.1f), new ExtraStats(28), new BowStats(35, 1.05f, 0.5f), new ArrowStats(1.4f, 15)), List.of(), 0xFFD8E2F0));
        context.register(key("steel"), new Material(rl("steel"), 3, tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/steel"))),
                stats(new HeadStats(540, 3, 7.0f, 5.0f), new HandleStats(1.05f, 1.0f, 1.0f), new ExtraStats(35), new BowStats(40, 1.1f, 0.5f), new ArrowStats(1.5f, 18)), List.of(), 0xFF888899));
    }

    /** Build the stats map. Wraps all five stat values so callers don't litter {@code Map.of} sites. */
    private static Map<PartType, MaterialStats> stats(HeadStats head, HandleStats handle, ExtraStats extra, BowStats bow, ArrowStats arrow) {
        return Map.of(PartType.PICKHEAD, head, PartType.HANDLE, handle, PartType.BINDING, extra, PartType.BOWLIMB, bow, PartType.ARROWSHAFT, arrow);
    }

    /** Convenience: wrap an ItemTags-style tag key in the Optional this codec field expects. */
    private static Optional<TagKey<Item>> tag(TagKey<Item> tag) {
        return Optional.of(tag);
    }

    /** Convenience: build the {@code tconstruct:<path>} ResourceLocation for a material id. */
    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, path);
    }

    /** Convenience: build the registry key for a material id. */
    private static ResourceKey<Material> key(String path) {
        return ResourceKey.create(Material.REGISTRY_KEY, rl(path));
    }

}
