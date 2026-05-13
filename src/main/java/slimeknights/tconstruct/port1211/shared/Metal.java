package slimeknights.tconstruct.port1211.shared;

import java.util.Objects;

import net.minecraft.world.level.material.MapColor;

/**
 * Driver record for a Phase-2 metal. Each Phase-2 provider (block storage block,
 * ingot/nugget items, common tags, smelting recipes, models, lang) iterates the same
 * {@link SharedMetals#ALL} list and consumes the fields it cares about, replacing the legacy
 * codebase's 14-times-repeated per-metal boilerplate with a single table.
 *
 * @param id            registry path under {@code tconstruct:}; lower-snake — used verbatim for
 *                      the storage block, ingot, nugget, fluid name, lang key, and tag suffix
 * @param mapColor      {@link MapColor} the storage block paints on a vanilla map
 * @param needsDiamond  {@code true} → the storage block requires a diamond pickaxe
 *                      (i.e. it joins {@code BlockTags.NEEDS_DIAMOND_TOOL});
 *                      {@code false} → an iron pickaxe is enough
 *                      ({@code BlockTags.NEEDS_IRON_TOOL}).
 *                      Per plan/03-shared-pulse only the three Nether-tier metals
 *                      ({@code cobalt}, {@code ardite}, {@code manyullyn}) are diamond-tier
 * @param tintHex       RGB tint applied to the molten fluid and any per-metal recolored
 *                      sprite; hex format {@code 0xRRGGBB}. Carried over verbatim from the
 *                      legacy {@code TinkerMaterials.mat(...)} declarations so existing
 *                      content stays color-identical post-port
 */
public record Metal(String id, MapColor mapColor, boolean needsDiamond, int tintHex) {

    public Metal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(mapColor, "mapColor");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must be non-blank");
        }
        if ((tintHex & ~0xFFFFFF) != 0) {
            throw new IllegalArgumentException("tintHex must fit in 24 bits (0xRRGGBB); got 0x" + Integer.toHexString(tintHex));
        }
    }
}
