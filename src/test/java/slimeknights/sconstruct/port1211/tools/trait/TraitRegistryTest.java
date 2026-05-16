package slimeknights.sconstruct.port1211.tools.trait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.data.ToolStats;
import slimeknights.sconstruct.port1211.tools.PartType;
import slimeknights.sconstruct.port1211.tools.material.MaterialTrait;

/**
 * Pinned-roster tests for {@link TraitRegistry}. The eighteen traits the mod ships — eight
 * mining-focused (SMTCON-103) and ten specialty (SMTCON-104) — are asserted by id so a future
 * trait swap surfaces loudly; stat contributions for the stat-bearing traits
 * ({@link Traits.EcologicalTraitRecord}, {@link Traits.DenseTraitRecord},
 * {@link Traits.FracturedTraitRecord}) are pinned against the legacy 1.12 baseline.
 */
class TraitRegistryTest {

    @Test
    void registersAllEighteenTraits() {
        Set<ResourceLocation> ids = Set.copyOf(TraitRegistry.all().stream().map(Trait::id).toList());
        assertEquals(18, ids.size(), "registry must hold exactly the 18 traits");
        for (String path : new String[] { "autosmelt", "ecological", "stonebound", "jagged", "crude", "cheap", "dense", "duritos", "aquadynamic", "featherweight", "holy", "insatiable", "magnetic",
                "prickly", "slimey", "squeaky", "fractured", "splintering" }) {
            assertTrue(ids.contains(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, path)), "missing trait id: " + path);
        }
    }

    @Test
    void lookupReturnsTheSameInstanceAsTheConstant() {
        assertSame(Traits.ECOLOGICAL, TraitRegistry.lookup(Traits.ECOLOGICAL.id()).orElseThrow());
        assertSame(Traits.DENSE, TraitRegistry.lookup(Traits.DENSE.id()).orElseThrow());
    }

    @Test
    void ecologicalAppliesFlatDurabilityBoost() {
        ToolStats stats = new ToolStats(100, 5.0F, 1.0F, 4.0F, 2, 3, 0.0F, 0.0F, 0.0F);
        ToolStats out = Traits.ECOLOGICAL.applyStats(stats);
        assertEquals(100 + Traits.EcologicalTraitRecord.DURABILITY_BONUS, out.maxDurability(), "ecological should add the durability bonus");
        // Other fields unchanged.
        assertEquals(stats.attackDamage(), out.attackDamage());
        assertEquals(stats.miningSpeed(), out.miningSpeed());
    }

    @Test
    void denseTradesMiningSpeedForDurability() {
        ToolStats stats = new ToolStats(100, 5.0F, 1.0F, 4.0F, 2, 3, 0.0F, 0.0F, 0.0F);
        ToolStats out = Traits.DENSE.applyStats(stats);
        assertEquals(100 + Traits.DenseTraitRecord.DURABILITY_BONUS, out.maxDurability());
        assertEquals(4.0F - Traits.DenseTraitRecord.MINING_SPEED_PENALTY, out.miningSpeed(), 0.0001F);
    }

    @Test
    void fracturedTradesDurabilityForAttackDamage() {
        ToolStats stats = new ToolStats(100, 5.0F, 1.0F, 4.0F, 2, 3, 0.0F, 0.0F, 0.0F);
        ToolStats out = Traits.FRACTURED.applyStats(stats);
        assertEquals(50, out.maxDurability(), "fractured should halve max durability");
        assertEquals(5.0F + Traits.FracturedTraitRecord.ATTACK_DAMAGE_BONUS, out.attackDamage(), 0.0001F);
        assertEquals(stats.miningSpeed(), out.miningSpeed());
    }

    @Test
    void applyAllSkipsUnknownTraitIds() {
        // Datapack drift sanity-check — an unknown trait id must not crash the rebuild.
        ToolStats stats = ToolStats.zero();
        List<MaterialTrait> grants = List.of(new MaterialTrait(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "unknown_trait"), PartType.PICKHEAD));
        ToolStats out = TraitRegistry.applyAll(stats, grants);
        assertEquals(stats, out, "unknown trait id must fall through to the input stats");
    }

    @Test
    void applyAllFoldsKnownGrantsInOrder() {
        ToolStats stats = new ToolStats(100, 5.0F, 1.0F, 4.0F, 2, 3, 0.0F, 0.0F, 0.0F);
        List<MaterialTrait> grants = List.of(new MaterialTrait(Traits.ECOLOGICAL.id(), PartType.PICKHEAD), new MaterialTrait(Traits.DENSE.id(), PartType.PICKHEAD));
        ToolStats out = TraitRegistry.applyAll(stats, grants);
        // Ecological (+20 durability) then Dense (+50 durability, -0.5 mining speed).
        assertEquals(100 + 20 + 50, out.maxDurability());
        assertEquals(3.5F, out.miningSpeed(), 0.0001F);
    }
}
