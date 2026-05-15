package slimeknights.sconstruct.port1211.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.data.ToolModifiers;
import slimeknights.sconstruct.port1211.common.data.ToolStats;
import slimeknights.sconstruct.port1211.tools.material.ArrowStats;
import slimeknights.sconstruct.port1211.tools.material.BowStats;
import slimeknights.sconstruct.port1211.tools.material.ExtraStats;
import slimeknights.sconstruct.port1211.tools.material.HandleStats;
import slimeknights.sconstruct.port1211.tools.material.HeadStats;
import slimeknights.sconstruct.port1211.tools.material.Material;
import slimeknights.sconstruct.port1211.tools.material.MaterialStats;

/**
 * Pure function that turns {@code (materials, modifiers, definition)} into a {@link ToolStats}
 * snapshot. Replaces the legacy {@code ToolBuilder} / {@code ToolNBT} math from 1.12
 * (see {@code src/main/java/slimeknights/tconstruct/library/utils/ToolNBT.java} and
 * {@code ToolCore.buildDefaultTag}). {@link ToolHelper} drives this hook on every
 * {@code setMaterials} / {@code addModifier} via the {@code rebuildStats} indirection wired
 * up in <a href="https://ericfisherdev.atlassian.net/browse/SMTCON-77">SMTCON-77</a>.
 *
 * <p><b>Aggregation rules</b> (ticket SMTCON-75 implementation plan):
 * <ul>
 *   <li>{@code durability = head.durability × handle.durabilityModifier + extra.extraDurability}</li>
 *   <li>{@code attackDamage = head.attackDamage + sharpness level × {@value SHARPNESS_DAMAGE_PER_LEVEL}}</li>
 *   <li>{@code miningSpeed = head.miningSpeed × handle.miningSpeedModifier + redstone level × {@value REDSTONE_SPEED_PER_LEVEL}}</li>
 *   <li>{@code harvestLevel = max(head.harvestLevel)}</li>
 *   <li>{@code freeModifiers = def.baseModifierSlots − Σ(modifier slot costs)} — the slot-cost
 *       model is a per-level-1 placeholder pending the modifier registry in
 *       <a href="https://ericfisherdev.atlassian.net/browse/SMTCON-83">SMTCON-83</a>.</li>
 * </ul>
 *
 * <p>Multi-slot tools (Hammer = 2× LARGEPLATE, Hammer/LumberAxe = TOUGHHANDLE +
 * BROADAXEHEAD/HAMMERHEAD, etc.) average head-stat / handle-modifier contributions across
 * every slot whose {@link MaterialStats} matches the relevant type. Extras (bindings,
 * plates) sum into the durability bonus per legacy parity.
 *
 * <p><b>Wood fallback</b>: any missing / null material slot folds in the canonical wood
 * baseline from <a href="https://ericfisherdev.atlassian.net/browse/SMTCON-71">SMTCON-71</a>'s
 * {@code TinkerMaterialBootstrap}, so an unfinished tool always produces a meaningful stat
 * snapshot rather than a NaN or zero crash downstream.
 *
 * <p>Side-effect free — no registry I/O, no caching, no logging. Easy to unit-test against
 * legacy expected outputs.
 */
public final class StatsBuilder {

    // Wood baseline (legacy parity — TinkerMaterialBootstrap.java wood entry). Materials at this
    // fallback are intentionally not registry-resolved so the builder stays pure even in unit
    // tests with no datapack registry attached.
    static final HeadStats WOOD_HEAD = new HeadStats(35, 0, 2.0F, 2.0F);
    static final HandleStats WOOD_HANDLE = new HandleStats(1.0F, 1.0F, 1.0F);
    static final ExtraStats WOOD_EXTRA = new ExtraStats(15);
    static final BowStats WOOD_BOW = new BowStats(20, 1.0F, 0.0F);
    static final ArrowStats WOOD_ARROW = new ArrowStats(0.7F, 0);

    /**
     * Modifier ids whose contribution {@link #compute} folds into the snapshot. The set is
     * intentionally small — adding more modifier bonuses without a corresponding
     * {@code Modifier} entry in <a href="https://ericfisherdev.atlassian.net/browse/SMTCON-83">SMTCON-83</a>'s
     * registry would silently shift base stats away from legacy parity.
     */
    static final ResourceLocation SHARPNESS_ID = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "sharpness");

    static final ResourceLocation REDSTONE_ID = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "haste");

    /** +1.25 attack damage per Sharpness level (SMTCON-84). Replaces the legacy 1.12 +0.5
     *  baseline — the 1.25 value matches the Sharpness modifier JSON shipped under
     *  {@code data/tconstruct/modifier/sharpness.json}, so a tool stack with 5 Sharpness levels
     *  gains +6.25 attack damage on top of the head's contribution. */
    static final float SHARPNESS_DAMAGE_PER_LEVEL = 1.25F;

    /** Legacy +0.08 mining speed per Redstone level (TConstruct 1.12). */
    static final float REDSTONE_SPEED_PER_LEVEL = 0.08F;

    private StatsBuilder() {
    }

    /**
     * Compute the cached {@link ToolStats} for a tool with the given materials, modifiers, and
     * definition. The materials list is positional — entry {@code i} pairs with
     * {@code def.getPartSlot(i)}. A null entry or out-of-bounds index folds in the wood
     * fallback for that slot so the math always produces a finite result.
     */
    public static ToolStats compute(List<Holder<Material>> mats, ToolModifiers mods, ToolDefinition def) {
        Objects.requireNonNull(mods, "mods");
        Objects.requireNonNull(def, "def");
        List<Holder<Material>> materials = mats == null ? List.of() : mats;

        // Per-stat-type contributor buckets — collected by walking the tool's part slots in
        // order so a multi-head tool (Hammer = 2× plate + head) gets every head's contribution.
        List<HeadStats> heads = new ArrayList<>();
        List<HandleStats> handles = new ArrayList<>();
        List<ExtraStats> extras = new ArrayList<>();
        List<BowStats> bows = new ArrayList<>();
        List<ArrowStats> arrows = new ArrayList<>();

        for (int i = 0; i < def.getPartCount(); i++) {
            PartType slot = def.getPartSlot(i);
            MaterialStats stats = lookupStats(materials, i, slot);
            // MaterialStats is a sealed interface with these five permits — exhaustive.
            switch (stats) {
            case HeadStats h -> heads.add(h);
            case HandleStats h -> handles.add(h);
            case ExtraStats e -> extras.add(e);
            case BowStats b -> bows.add(b);
            case ArrowStats a -> arrows.add(a);
            }
        }

        // ── Head aggregates ────────────────────────────────────────────────────────────────
        double headDurability = average(heads, HeadStats::durability);
        float headAttackDamage = (float) average(heads, h -> (double) h.attackDamage());
        float headMiningSpeed = (float) average(heads, h -> (double) h.miningSpeed());
        int harvestLevel = max(heads, HeadStats::harvestLevel);

        // ── Handle aggregates (multipliers; default 1.0 if no handle slot present) ─────────
        float durabilityMod = (float) averageOrDefault(handles, h -> (double) h.durabilityModifier(), 1.0);
        float miningSpeedMod = (float) averageOrDefault(handles, h -> (double) h.miningSpeedModifier(), 1.0);
        float attackSpeedMod = (float) averageOrDefault(handles, h -> (double) h.attackSpeedModifier(), 1.0);

        // ── Extras (sum across every binding/plate) ────────────────────────────────────────
        int extraDurability = extras.stream().mapToInt(ExtraStats::extraDurability).sum() + arrows.stream().mapToInt(ArrowStats::extraDurability).sum();

        // ── Modifier bonuses ───────────────────────────────────────────────────────────────
        int sharpness = mods.levels().getOrDefault(SHARPNESS_ID, 0);
        int redstone = mods.levels().getOrDefault(REDSTONE_ID, 0);

        // ── Combine ────────────────────────────────────────────────────────────────────────
        int maxDurability = Math.max(1, Math.round((float) headDurability * durabilityMod) + extraDurability);
        float attackDamage = headAttackDamage + sharpness * SHARPNESS_DAMAGE_PER_LEVEL;
        float miningSpeed = headMiningSpeed * miningSpeedMod + redstone * REDSTONE_SPEED_PER_LEVEL;
        int totalSlotCost = mods.levels().values().stream().mapToInt(Integer::intValue).sum();
        int freeModifiers = Math.max(0, def.baseModifierSlots() - totalSlotCost);

        // ── Bow / arrow lanes ──────────────────────────────────────────────────────────────
        float drawSpeed = (float) averageOrDefault(bows, b -> (double) b.drawSpeed(), 0.0);
        float bowRange = (float) averageOrDefault(bows, b -> (double) b.rangeMultiplier(), 0.0);
        float projectileBonus = (float) (averageOrDefault(bows, b -> (double) b.damageBonus(), 0.0) + sumD(arrows, a -> (double) a.weight()));

        return new ToolStats(maxDurability, attackDamage, attackSpeedMod, miningSpeed, harvestLevel, freeModifiers, drawSpeed, bowRange, projectileBonus);
    }

    /**
     * Look up the material stat for slot {@code i} of part type {@code slot}, falling back to
     * the wood baseline whenever the caller didn't provide a matching material or the material
     * has no entry for that part type.
     */
    private static MaterialStats lookupStats(List<Holder<Material>> materials, int index, PartType slot) {
        if (index < materials.size()) {
            Holder<Material> holder = materials.get(index);
            if (holder != null && holder.isBound()) {
                MaterialStats explicit = holder.value().stats().get(slot);
                if (explicit != null) {
                    return explicit;
                }
            }
        }
        return woodDefaultFor(slot);
    }

    /**
     * Wood-tier default for a given part-type slot. Switch is exhaustive over the PartType
     * enum so a new value added in a future ticket forces a compile-time decision about which
     * stat class it belongs to.
     */
    private static MaterialStats woodDefaultFor(PartType slot) {
        return switch (slot) {
        case PICKHEAD, AXEHEAD, SHOVELHEAD, SWORDBLADE, BROADAXEHEAD, BROADBLADE, HAMMERHEAD -> WOOD_HEAD;
        case HANDLE, TOUGHHANDLE -> WOOD_HANDLE;
        case BINDING, TOUGHBINDING, WIDEGUARD, LARGEPLATE -> WOOD_EXTRA;
        case BOWLIMB, BOWSTRING -> WOOD_BOW;
        case ARROWSHAFT, ARROW_HEAD, FLETCHING -> WOOD_ARROW;
        };
    }

    private static <T> double average(Collection<T> items, java.util.function.ToDoubleFunction<T> field) {
        return items.isEmpty() ? 0.0 : items.stream().mapToDouble(field).average().orElse(0.0);
    }

    private static <T> double averageOrDefault(Collection<T> items, java.util.function.ToDoubleFunction<T> field, double fallback) {
        return items.isEmpty() ? fallback : items.stream().mapToDouble(field).average().orElse(fallback);
    }

    private static <T> int max(Collection<T> items, java.util.function.ToIntFunction<T> field) {
        return items.stream().mapToInt(field).max().orElse(0);
    }

    private static <T> double sumD(Collection<T> items, java.util.function.ToDoubleFunction<T> field) {
        return items.stream().mapToDouble(field).sum();
    }
}
