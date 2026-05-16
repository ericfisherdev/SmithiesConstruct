package slimeknights.sconstruct.port1211.smeltery;

import java.util.Objects;

import net.minecraft.world.level.material.MapColor;

/**
 * Driver record for a Phase-5 molten-metal fluid. Each Phase-5 smeltery provider (fluid +
 * fluid-type registration, molten-block, bucket item, melting / casting recipes, client tint
 * metadata, lang) iterates the same {@link MoltenMetals#ALL} list and consumes the fields it
 * needs — the same single-table pattern the Phase-2 {@link slimeknights.sconstruct.port1211.shared.Metal}
 * driver established, replacing the legacy {@code TinkerFluids} per-fluid boilerplate.
 *
 * @param id          registry path under {@code sconstruct:}; lower-snake — used verbatim for
 *                    the fluid name ({@code molten_<id>}), molten block, bucket, lang key, and
 *                    tag suffix
 * @param mapColor    {@link MapColor} the molten-fluid block paints on a vanilla map
 * @param tint        RGB tint applied to the fluid texture and the per-metal recoloured bucket
 *                    sprite; {@code 0xRRGGBB}. Carried verbatim from the legacy
 *                    {@code TinkerFluids} / {@code TinkerMaterials} colour declarations so
 *                    post-port molten metals stay colour-identical
 * @param temperature fluid temperature in <strong>kelvin</strong>, the unit NeoForge's
 *                    {@code FluidType} expects (vanilla water is 300 K). Re-scaled from the
 *                    legacy 1.12 arbitrary values to a kelvin range anchored by the SMTCON-108
 *                    plan: iron 1500 K, cobalt 2400 K, ender 3000 K — the rest interpolate
 *                    while preserving the legacy relative ordering (tin coolest, ender hottest)
 * @param density     fluid density in g/L — molten metal sinks, so always positive. Loosely
 *                    tracks real molten-metal density (gold and lead are dense; the slime /
 *                    glass / obsidian melts are light); a gameplay tuning, not a physical
 *                    constant
 * @param luminosity  block light level 0-15 the molten fluid emits — scaled to temperature so
 *                    the hottest melts (manyullyn, obsidian, ender) glow brightest
 */
public record MoltenMetal(String id, MapColor mapColor, int tint, int temperature, int density, int luminosity) {

    public MoltenMetal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(mapColor, "mapColor");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must be non-blank");
        }
        if ((tint & ~0xFFFFFF) != 0) {
            throw new IllegalArgumentException("tint must fit in 24 bits (0xRRGGBB); got 0x" + Integer.toHexString(tint));
        }
        if (temperature <= 0) {
            throw new IllegalArgumentException("temperature must be a positive kelvin value; got " + temperature);
        }
        if (density <= 0) {
            throw new IllegalArgumentException("density must be positive (molten metal sinks); got " + density);
        }
        if (luminosity < 0 || luminosity > 15) {
            throw new IllegalArgumentException("luminosity must be a 0-15 block-light level; got " + luminosity);
        }
    }
}
