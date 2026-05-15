package slimeknights.sconstruct.port1211.tools.trait;

import net.minecraft.resources.ResourceLocation;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.data.ToolStats;

/**
 * Code-side singletons for the eight mining-focused legacy 1.12 traits. Each constant is a
 * record implementing {@link Trait}; stat-bearing traits override {@link Trait#applyStats}.
 * The traits live as constants here so the SMTCON-105 follow-up that wires materials to traits
 * can reference them by direct field rather than registry lookup.
 *
 * <p>Stat magnitudes match the legacy 1.12 baseline where applicable; behaviour-driven traits
 * (autosmelt, stonebound, jagged, crude, cheap, duritos) keep an empty stat contribution and
 * will gain their side effects in the SMTCON-83 modifier-hook follow-up via a {@code TraitType}
 * dispatch system mirroring {@code ModifierType}.
 */
public final class Traits {

    /** Auto-smelt ore drops into ingots. Behaviour hook only — no stat contribution. */
    public static final Trait AUTOSMELT = new TraitRecord(id("autosmelt"));

    /** Regenerate durability slowly while held in a natural biome. Behaviour hook only. */
    public static final Trait ECOLOGICAL = new EcologicalTraitRecord(id("ecological"));

    /** Mining speed scales up as durability drops. Behaviour hook only. */
    public static final Trait STONEBOUND = new TraitRecord(id("stonebound"));

    /** Attack damage scales up as durability drops. Behaviour hook only. */
    public static final Trait JAGGED = new TraitRecord(id("jagged"));

    /** Ignore harvest level on low-tier blocks. Behaviour hook only. */
    public static final Trait CRUDE = new TraitRecord(id("crude"));

    /** Reduced durability cost per swing. Behaviour hook only. */
    public static final Trait CHEAP = new TraitRecord(id("cheap"));

    /** Higher durability ceiling at the cost of mining speed. */
    public static final Trait DENSE = new DenseTraitRecord(id("dense"));

    /** Chance to ignore durability cost on a swing. Behaviour hook only. */
    public static final Trait DURITOS = new TraitRecord(id("duritos"));

    private Traits() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, path);
    }

    /** Bare trait record — behaviour-only, no stat contribution. */
    public record TraitRecord(ResourceLocation id) implements Trait {
    }

    /** Ecological: +20 max durability flat — small bonus matching legacy 1.12. */
    public record EcologicalTraitRecord(ResourceLocation id) implements Trait {

        /** Flat durability boost — legacy 1.12 ecological baseline. */
        public static final int DURABILITY_BONUS = 20;

        @Override
        public ToolStats applyStats(ToolStats stats) {
            return new ToolStats(stats.maxDurability() + DURABILITY_BONUS, stats.attackDamage(), stats.attackSpeed(), stats.miningSpeed(), stats.harvestLevel(), stats.freeModifiers(),
                    stats.drawSpeed(), stats.bowRange(), stats.projectileBonus());
        }
    }

    /** Dense: +50 max durability at the cost of -0.5 mining speed. */
    public record DenseTraitRecord(ResourceLocation id) implements Trait {

        /** Flat durability boost — legacy 1.12 dense baseline. */
        public static final int DURABILITY_BONUS = 50;

        /** Mining-speed penalty — keeps the dense trade-off legacy-accurate. */
        public static final float MINING_SPEED_PENALTY = 0.5F;

        @Override
        public ToolStats applyStats(ToolStats stats) {
            float miningSpeed = Math.max(0.0F, stats.miningSpeed() - MINING_SPEED_PENALTY);
            return new ToolStats(stats.maxDurability() + DURABILITY_BONUS, stats.attackDamage(), stats.attackSpeed(), miningSpeed, stats.harvestLevel(), stats.freeModifiers(), stats.drawSpeed(),
                    stats.bowRange(), stats.projectileBonus());
        }
    }
}
