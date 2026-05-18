package slimeknights.sconstruct.world.block;

import java.util.Objects;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Four-block registration bundle for the plant life of a single coloured slime island:
 * {@link SlimeDirtBlock}, {@link SlimeGrassBlock}, {@link SlimeLeavesBlock}, and
 * {@link SlimeSaplingBlock}. Held together so a downstream provider (lang, tags, loot, creative
 * tab) can iterate {@link slimeknights.sconstruct.world.WorldBlocks#PLANT_SETS} and
 * deal with the whole-set surface without reaching into named fields.
 *
 * <p>All four components are stored as {@link DeferredBlock}s so the set can be constructed
 * before any of the underlying registries fire — the holders only resolve during the registry
 * event chain. Mirrors the {@code SlimeFluidSet} pattern from SMTCON-51.
 */
public record SlimePlantSet(DeferredBlock<SlimeDirtBlock> dirt, DeferredBlock<SlimeGrassBlock> grass, DeferredBlock<SlimeLeavesBlock> leaves, DeferredBlock<SlimeSaplingBlock> sapling) {

    public SlimePlantSet {
        Objects.requireNonNull(dirt, "dirt");
        Objects.requireNonNull(grass, "grass");
        Objects.requireNonNull(leaves, "leaves");
        Objects.requireNonNull(sapling, "sapling");
    }

    /** Visit each of the four blocks in declaration order. Used by datagen iterations. */
    public java.util.List<DeferredBlock<? extends Block>> all() {
        return java.util.List.of(dirt, grass, leaves, sapling);
    }
}
