package slimeknights.sconstruct.port1211.tools;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.common.data.ToolModifiers;
import slimeknights.sconstruct.port1211.common.data.ToolStats;
import slimeknights.sconstruct.port1211.tools.material.ArrowStats;
import slimeknights.sconstruct.port1211.tools.material.BowStats;
import slimeknights.sconstruct.port1211.tools.material.ExtraStats;
import slimeknights.sconstruct.port1211.tools.material.HandleStats;
import slimeknights.sconstruct.port1211.tools.material.HeadStats;
import slimeknights.sconstruct.port1211.tools.material.Material;
import slimeknights.sconstruct.port1211.tools.material.MaterialStats;
import slimeknights.sconstruct.port1211.tools.material.MaterialTrait;

/**
 * Pinned-behaviour tests for {@link StatsBuilder}. Asserts the aggregation contract against
 * the legacy {@code ToolNBT} math (durability multiplier vs head average, attack damage +
 * sharpness, mining speed × handle modifier + redstone bonus, max harvest level across heads,
 * free modifier slots = base − Σ levels). Covers a pure-wood pickaxe, a pure-iron pickaxe,
 * a mixed wood-handle / iron-head / wood-binding pickaxe, and the modifier-bonus contribution.
 */
class StatsBuilderTest {

    private static final ResourceLocation WOOD_ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "wood");
    private static final ResourceLocation IRON_ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");

    // Legacy TinkerMaterialBootstrap values for wood / iron — pinned here so a future shift in
    // the bootstrap defaults forces a synchronised update in the builder math.
    private static final HeadStats WOOD_HEAD = new HeadStats(35, 0, 2.0F, 2.0F);
    private static final HandleStats WOOD_HANDLE = new HandleStats(1.0F, 1.0F, 1.0F);
    private static final ExtraStats WOOD_EXTRA = new ExtraStats(15);
    private static final BowStats WOOD_BOW = new BowStats(20, 1.0F, 0.0F);
    private static final ArrowStats WOOD_ARROW = new ArrowStats(0.7F, 0);

    private static final HeadStats IRON_HEAD = new HeadStats(250, 2, 6.0F, 4.0F);
    private static final HandleStats IRON_HANDLE = new HandleStats(1.0F, 1.0F, 1.0F);
    private static final ExtraStats IRON_EXTRA = new ExtraStats(25);

    private static final Material WOOD = mat(WOOD_ID, 0, WOOD_HEAD, WOOD_HANDLE, WOOD_EXTRA, WOOD_BOW, WOOD_ARROW);
    private static final Material IRON = mat(IRON_ID, 2, IRON_HEAD, IRON_HANDLE, IRON_EXTRA, new BowStats(35, 1.1F, 0.5F), new ArrowStats(1.5F, 10));

    // ----------------------------------------------------------------------------- pure-wood

    @Test
    void pureWoodPickaxeMatchesLegacyBaseline() {
        // Legacy ToolNBT for a wood pickaxe: durability = wood-head(35) * wood-handle(1.0) +
        // wood-extra(15) = 50; attack = 2.0; miningSpeed = 2.0 * 1.0 = 2.0; harvestLevel = 0;
        // freeModifiers = 3 (no modifiers).
        ToolStats stats = StatsBuilder.compute(parts(WOOD, WOOD, WOOD), ToolModifiers.empty(), ToolDefinition.PICKAXE);
        assertAll(() -> assertEquals(50, stats.maxDurability()), () -> assertEquals(2.0F, stats.attackDamage(), 1e-4),
                () -> assertEquals(1.0F, stats.attackSpeed(), 1e-4, "handle attackSpeedModifier = 1.0"), () -> assertEquals(2.0F, stats.miningSpeed(), 1e-4),
                () -> assertEquals(0, stats.harvestLevel()), () -> assertEquals(3, stats.freeModifiers()));
    }

    // ----------------------------------------------------------------------------- pure-iron

    @Test
    void pureIronPickaxeMatchesLegacyBaseline() {
        // durability = 250 * 1.0 + 25 = 275; attack = 4.0; miningSpeed = 6.0; harvestLevel = 2.
        ToolStats stats = StatsBuilder.compute(parts(IRON, IRON, IRON), ToolModifiers.empty(), ToolDefinition.PICKAXE);
        assertAll(() -> assertEquals(275, stats.maxDurability()), () -> assertEquals(4.0F, stats.attackDamage(), 1e-4), () -> assertEquals(6.0F, stats.miningSpeed(), 1e-4),
                () -> assertEquals(2, stats.harvestLevel()), () -> assertEquals(3, stats.freeModifiers()));
    }

    // -------------------------------------------------------------------------------- mixed

    @Test
    void mixedMaterialsAverageHandleModifierAndPropagateHeadHarvestLevel() {
        // handle = WOOD (modifier 1.0), head = IRON (durability 250, harvest 2, attack 4,
        // mining 6), extra = WOOD (extra 15). Expect: durability = round(250 * 1.0) + 15 =
        // 265; attack = 4; mining = 6 * 1.0 = 6; harvestLevel = 2.
        ToolStats stats = StatsBuilder.compute(parts(WOOD, IRON, WOOD), ToolModifiers.empty(), ToolDefinition.PICKAXE);
        assertAll(() -> assertEquals(265, stats.maxDurability()), () -> assertEquals(4.0F, stats.attackDamage(), 1e-4), () -> assertEquals(6.0F, stats.miningSpeed(), 1e-4),
                () -> assertEquals(2, stats.harvestLevel(), "max harvest level wins across heads"));
    }

    // --------------------------------------------------------------------- modifier bonuses

    @Test
    void sharpnessRaisesAttackDamageByHalfPerLevel() {
        ToolModifiers withSharpness = ToolModifiers.empty().with(StatsBuilder.SHARPNESS_ID, 3);
        ToolStats stats = StatsBuilder.compute(parts(WOOD, IRON, WOOD), withSharpness, ToolDefinition.PICKAXE);
        // 4.0 base + 3 * 0.5 = 5.5
        assertEquals(5.5F, stats.attackDamage(), 1e-4);
    }

    @Test
    void hasteRaisesMiningSpeedByEightHundredthsPerLevel() {
        ToolModifiers withHaste = ToolModifiers.empty().with(StatsBuilder.REDSTONE_ID, 5);
        ToolStats stats = StatsBuilder.compute(parts(WOOD, IRON, WOOD), withHaste, ToolDefinition.PICKAXE);
        // 6.0 base + 5 * 0.08 = 6.4
        assertEquals(6.4F, stats.miningSpeed(), 1e-4);
    }

    @Test
    void modifierSlotCostsReduceFreeModifierCount() {
        // baseModifierSlots = 3 on PICKAXE; sharpness level 2 = 2 cost units. freeModifiers
        // = max(0, 3 − 2) = 1.
        ToolModifiers withSharpness = ToolModifiers.empty().with(StatsBuilder.SHARPNESS_ID, 2);
        ToolStats stats = StatsBuilder.compute(parts(WOOD, WOOD, WOOD), withSharpness, ToolDefinition.PICKAXE);
        assertEquals(1, stats.freeModifiers());
    }

    @Test
    void freeModifiersFloorAtZeroWhenOverdrawn() {
        // sharpness 5 + haste 5 = 10 cost units, exceeds base 3 — must not go negative
        // (ToolStats compact constructor would reject a negative).
        ToolModifiers heavy = ToolModifiers.empty().with(StatsBuilder.SHARPNESS_ID, 5).with(StatsBuilder.REDSTONE_ID, 5);
        ToolStats stats = StatsBuilder.compute(parts(WOOD, WOOD, WOOD), heavy, ToolDefinition.PICKAXE);
        assertEquals(0, stats.freeModifiers());
    }

    // ---------------------------------------------------------------------- wood fallbacks

    @Test
    void missingMaterialSlotFoldsInWoodDefault() {
        // Only first slot supplied — second and third fall back to wood defaults so the math
        // still produces a finite, non-NaN result (defensive contract from the ticket).
        ToolStats stats = StatsBuilder.compute(parts(WOOD), ToolModifiers.empty(), ToolDefinition.PICKAXE);
        // wood pickaxe with all three slots = 50 durability, 2 attack, 2 mining, 0 harvest
        assertEquals(50, stats.maxDurability());
        assertEquals(2.0F, stats.attackDamage(), 1e-4);
    }

    @Test
    void emptyMaterialsListBuildsAFullWoodTool() {
        ToolStats stats = StatsBuilder.compute(List.of(), ToolModifiers.empty(), ToolDefinition.PICKAXE);
        assertEquals(50, stats.maxDurability(), "empty materials must build the wood baseline");
    }

    @Test
    void nullMaterialsListBuildsAFullWoodTool() {
        // Ticket: "if any material is null/missing, fall back to wood defaults". Allow the
        // whole list to be null for symmetry with per-slot null entries.
        ToolStats stats = StatsBuilder.compute(null, ToolModifiers.empty(), ToolDefinition.PICKAXE);
        assertEquals(50, stats.maxDurability());
    }

    @Test
    void nullEntryInMaterialsListFoldsInWoodDefault() {
        // Mixed list: head present, handle null. Builder must not NPE.
        List<Holder<Material>> mats = java.util.Arrays.asList(null, Holder.direct(IRON), Holder.direct(WOOD));
        ToolStats stats = StatsBuilder.compute(mats, ToolModifiers.empty(), ToolDefinition.PICKAXE);
        assertNotNull(stats);
        assertEquals(2, stats.harvestLevel(), "iron head still drives harvest level");
    }

    // ----------------------------------------------------------------------- contract guards

    @Test
    void computeRejectsNullModsOrDef() {
        assertAll(() -> assertThrows(NullPointerException.class, () -> StatsBuilder.compute(parts(WOOD), null, ToolDefinition.PICKAXE)),
                () -> assertThrows(NullPointerException.class, () -> StatsBuilder.compute(parts(WOOD), ToolModifiers.empty(), null)));
    }

    @Test
    void multiHeadToolAveragesHeadAttackAndMaxesHarvestLevel() {
        // HAMMER = TOUGHHANDLE + HAMMERHEAD + LARGEPLATE + LARGEPLATE. Head slots produce two
        // entries here (HAMMERHEAD + 2× LARGEPLATE wood-extra). Use mixed iron HAMMERHEAD with
        // wood plates to assert the head only contributes from the HAMMERHEAD slot — plates
        // are extras, not heads.
        ToolStats stats = StatsBuilder.compute(parts(WOOD, IRON, WOOD, WOOD), ToolModifiers.empty(), ToolDefinition.HAMMER);
        // head iron only (250 dur, 2 harvest, 4 attack, 6 mining) * wood handle (1.0) + 2× wood
        // extra (15 each) + arrow extras none = 250 + 30 = 280
        assertEquals(280, stats.maxDurability());
        assertEquals(2, stats.harvestLevel());
        assertEquals(4.0F, stats.attackDamage(), 1e-4);
    }

    @Test
    void bowDrawSpeedAveragesBowMaterialStatsAcrossLimbAndStringSlots() {
        // SHORTBOW = BOWLIMB + BOWLIMB + BOWSTRING. In the current port every bow-coded
        // PartType (BOWLIMB and BOWSTRING) carries BowStats, so the average folds all three
        // entries together. Pure-iron bow: drawSpeed = avg(35, 35, 35) = 35.
        ToolStats stats = StatsBuilder.compute(parts(IRON, IRON, IRON), ToolModifiers.empty(), ToolDefinition.SHORTBOW);
        assertEquals(35.0F, stats.drawSpeed(), 1e-4);
    }

    // -------------------------------------------------------------------- fixture helpers

    private static List<Holder<Material>> parts(Material... mats) {
        java.util.List<Holder<Material>> out = new java.util.ArrayList<>(mats.length);
        for (Material m : mats) {
            out.add(Holder.direct(m));
        }
        return out;
    }

    private static Material mat(ResourceLocation id, int tier, HeadStats head, HandleStats handle, ExtraStats extra, BowStats bow, ArrowStats arrow) {
        Map<PartType, MaterialStats> stats = new java.util.EnumMap<>(PartType.class);
        for (PartType pt : PartType.values()) {
            MaterialStats s = switch (pt) {
            case PICKHEAD, AXEHEAD, SHOVELHEAD, SWORDBLADE, BROADAXEHEAD, BROADBLADE, HAMMERHEAD -> head;
            case HANDLE, TOUGHHANDLE -> handle;
            case BINDING, TOUGHBINDING, WIDEGUARD, LARGEPLATE -> extra;
            case BOWLIMB, BOWSTRING -> bow;
            case ARROWSHAFT, ARROW_HEAD, FLETCHING -> arrow;
            };
            stats.put(pt, s);
        }
        return new Material(id, tier, Optional.empty(), stats, List.<MaterialTrait> of(), 0);
    }
}
