package slimeknights.sconstruct.port1211.smeltery;

import java.util.List;

import net.minecraft.world.level.material.MapColor;

/**
 * Canonical list of every molten-metal fluid the smeltery pulse registers. Phase-5 content
 * providers iterate {@link #ALL} to derive their per-fluid output (fluid + fluid-type, molten
 * block, bucket, melting / casting recipes, client tints, lang) — adding a molten metal means
 * adding one entry here, not editing 6+ providers. Mirrors the Phase-2
 * {@link slimeknights.sconstruct.port1211.shared.SharedMetals} table.
 *
 * <p>The list holds 20 entries: the metal melts (iron … pigiron), the slime-iron knightslime
 * melt, and the four non-metal melts the smeltery also produces (obsidian, glass, emerald,
 * ender). Tints are copied verbatim from the legacy {@code TinkerFluids} / {@code TinkerMaterials}
 * colour declarations so post-port content stays colour-identical.
 *
 * <p>Temperatures are kelvin, re-scaled from the legacy 1.12 values to the SMTCON-108 anchors
 * (iron 1500, cobalt 2400, ender 3000); the entries below stay monotonic in the legacy relative
 * ordering — tin is the coolest melt, ender the hottest. Density and luminosity are gameplay
 * tunings: density loosely tracks real molten-metal density, luminosity scales with temperature.
 */
public final class MoltenMetals {

    private MoltenMetals() {
    }

    /** Molten iron — the smeltery's baseline melt; temperature anchor at 1500 K. */
    public static final MoltenMetal IRON = new MoltenMetal("iron", MapColor.FIRE, 0xa81212, 1500, 7000, 10);
    /** Molten gold — dense, low-melting precious metal. */
    public static final MoltenMetal GOLD = new MoltenMetal("gold", MapColor.GOLD, 0xf6d609, 1150, 17000, 8);
    /** Molten copper. */
    public static final MoltenMetal COPPER = new MoltenMetal("copper", MapColor.COLOR_ORANGE, 0xed9f07, 1200, 8000, 8);
    /** Molten tin — the coolest melt in the roster. */
    public static final MoltenMetal TIN = new MoltenMetal("tin", MapColor.COLOR_LIGHT_GRAY, 0xc1cddc, 700, 6900, 6);
    /** Molten zinc. */
    public static final MoltenMetal ZINC = new MoltenMetal("zinc", MapColor.COLOR_LIGHT_GRAY, 0xd3efe8, 750, 6600, 6);
    /** Molten brass — copper + zinc alloy. */
    public static final MoltenMetal BRASS = new MoltenMetal("brass", MapColor.GOLD, 0xede38b, 950, 8400, 7);
    /** Molten aluminium brass — Tinkers' canonical cast-making alloy. */
    public static final MoltenMetal ALUBRASS = new MoltenMetal("alubrass", MapColor.GOLD, 0xece347, 1050, 4000, 7);
    /** Molten bronze — copper + tin alloy. */
    public static final MoltenMetal BRONZE = new MoltenMetal("bronze", MapColor.GOLD, 0xe3bd68, 970, 8800, 7);
    /** Molten silver. */
    public static final MoltenMetal SILVER = new MoltenMetal("silver", MapColor.SNOW, 0xd1ecf6, 1000, 9300, 7);
    /** Molten lead — the densest of the common metals. */
    public static final MoltenMetal LEAD = new MoltenMetal("lead", MapColor.COLOR_GRAY, 0x4d4968, 800, 10700, 6);
    /** Molten steel — iron + carbon alloy. */
    public static final MoltenMetal STEEL = new MoltenMetal("steel", MapColor.METAL, 0xa7a7a7, 1450, 7000, 9);
    /** Molten cobalt — Nether-tier metal; temperature anchor at 2400 K. */
    public static final MoltenMetal COBALT = new MoltenMetal("cobalt", MapColor.COLOR_BLUE, 0x2882d4, 2400, 8000, 13);
    /** Molten ardite — Nether-tier counterpart to cobalt. */
    public static final MoltenMetal ARDITE = new MoltenMetal("ardite", MapColor.TERRACOTTA_ORANGE, 0xd14210, 2100, 7400, 12);
    /** Molten manyullyn — cobalt + ardite alloy, the iconic late-game melt. */
    public static final MoltenMetal MANYULLYN = new MoltenMetal("manyullyn", MapColor.COLOR_PURPLE, 0xa15cf8, 2600, 8600, 14);
    /** Molten pig iron — blood + iron alloy. */
    public static final MoltenMetal PIGIRON = new MoltenMetal("pigiron", MapColor.TERRACOTTA_PINK, 0xef9e9b, 1350, 6800, 9);
    /** Molten knightslime — slime + iron alloy; a light, slimey melt. */
    public static final MoltenMetal KNIGHTSLIME = new MoltenMetal("knightslime", MapColor.COLOR_PINK, 0xf18ff0, 1100, 3000, 8);
    /** Molten obsidian — non-metal smeltery melt. */
    public static final MoltenMetal OBSIDIAN = new MoltenMetal("obsidian", MapColor.COLOR_BLACK, 0x2c0d59, 2700, 2600, 14);
    /** Molten glass — non-metal smeltery melt. */
    public static final MoltenMetal GLASS = new MoltenMetal("glass", MapColor.COLOR_LIGHT_BLUE, 0xc0f5fe, 1400, 2400, 9);
    /** Molten emerald — non-metal smeltery melt. */
    public static final MoltenMetal EMERALD = new MoltenMetal("emerald", MapColor.EMERALD, 0x58e78e, 2500, 2700, 13);
    /** Molten ender — the hottest melt in the roster; temperature anchor at 3000 K. */
    public static final MoltenMetal ENDER = new MoltenMetal("ender", MapColor.COLOR_CYAN, 0x149b83, 3000, 5000, 15);

    /**
     * Every molten-metal fluid the smeltery pulse registers, in declaration order. The single
     * iteration surface for the Phase-5 fluid / block / recipe / lang providers.
     */
    public static final List<MoltenMetal> ALL = List.of(IRON, GOLD, COPPER, TIN, ZINC, BRASS, ALUBRASS, BRONZE, SILVER, LEAD, STEEL, COBALT, ARDITE, MANYULLYN, PIGIRON, KNIGHTSLIME, OBSIDIAN, GLASS,
            EMERALD, ENDER);
}
