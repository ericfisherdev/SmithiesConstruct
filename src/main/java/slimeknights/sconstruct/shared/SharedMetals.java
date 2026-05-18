package slimeknights.sconstruct.shared;

import java.util.List;

import net.minecraft.world.level.material.MapColor;

/**
 * Canonical list of every metal the shared pulse registers. Phase-2 content providers iterate
 * {@link #ALL} to derive their per-metal output (blocks, items, tags, recipes, lang) — adding
 * a new metal means adding one entry here, not editing 6+ providers.
 *
 * <p>The list contains 14 entries. Copper is intentionally absent — it is a vanilla material
 * since Minecraft 1.17, so the mod defers to {@code minecraft:copper_ingot} /
 * {@code copper_block} rather than registering a duplicate. The molten-copper smeltery fluid
 * and the copper tool material are kept; both consume vanilla copper via the
 * {@code c:ingots/copper} common tag. Per-row comments mark each as <strong>real-world</strong>
 * (an element or alloy that exists outside Minecraft) or <strong>fictional</strong> (a
 * Tinkers' Construct invention). This distinction matters for the {@code c:} common-tag
 * decision in plan/03-shared-pulse: real-world metals are tagged in the shared {@code c:}
 * namespace so other mods can interoperate; fictional metals stay under {@code sconstruct:}
 * to avoid polluting common tags with names other mods cannot meaningfully consume.
 *
 * <p>Tint hexes are copied verbatim from the legacy {@code TinkerMaterials.mat(...)} calls so
 * post-port content stays color-identical. Mining-tier flags follow plan/03 line 142
 * ("cobalt+ardite+manyullyn need diamond level") — the three Nether-tier metals are the only
 * diamond-tier entries.
 */
public final class SharedMetals {

    private SharedMetals() {
    }

    public static final List<Metal> ALL = List.of(
            // Fictional — Nether-tier, mined as ore on the Nether tier (diamond pickaxe).
            new Metal("cobalt", MapColor.COLOR_BLUE, true, 0x2882d4, false),
            // Fictional — Nether-tier counterpart to cobalt; alloys with cobalt to make manyullyn.
            new Metal("ardite", MapColor.TERRACOTTA_ORANGE, true, 0xd14210, false),
            // Fictional — cobalt + ardite alloy; the iconic late-game Tinkers metal.
            new Metal("manyullyn", MapColor.COLOR_PURPLE, true, 0xa15cf8, false),
            // Fictional — slime + iron alloy; named after the Knightslime gear set.
            new Metal("knightslime", MapColor.COLOR_PINK, false, 0xf18ff0, false),
            // Fictional — blood + iron alloy; the "smelt a zombie pigman" meme metal.
            new Metal("pigiron", MapColor.TERRACOTTA_PINK, false, 0xef9e9b, false),
            // Real-world — Ag, atomic number 47.
            new Metal("silver", MapColor.SNOW, false, 0xd1ecf6, true),
            // Real-world — Sn, atomic number 50.
            new Metal("tin", MapColor.COLOR_LIGHT_GRAY, false, 0xc1cddc, true),
            // Real-world — Zn, atomic number 30.
            new Metal("zinc", MapColor.COLOR_LIGHT_GRAY, false, 0xd3efe8, true),
            // Real-world alloy — copper + zinc.
            new Metal("brass", MapColor.GOLD, false, 0xede38b, true),
            // Fictional alloy — aluminum + brass; Tinkers' canonical cast-making metal.
            new Metal("alubrass", MapColor.GOLD, false, 0xece347, false),
            // Real-world alloy — gold + silver.
            new Metal("electrum", MapColor.GOLD, false, 0xe8db49, true),
            // Real-world alloy — iron + carbon.
            new Metal("steel", MapColor.METAL, false, 0xa7a7a7, true),
            // Real-world — Pb, atomic number 82.
            new Metal("lead", MapColor.COLOR_GRAY, false, 0x4d4968, true),
            // Real-world — Ni, atomic number 28.
            new Metal("nickel", MapColor.TERRACOTTA_YELLOW, false, 0xc8d683, true));
}
