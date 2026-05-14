package slimeknights.sconstruct.port1211.world.block;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;

import slimeknights.sconstruct.port1211.world.WorldFeatures;

/**
 * Sapling for a coloured slime tree. Extends vanilla {@link SaplingBlock} so the stage tracking
 * (sapling → tree advance over multiple random ticks), bonemeal target detection, and the
 * "must stand on dirt-or-grass" placement check inherit verbatim.
 *
 * <p>Each colour ships its own {@link TreeGrower} pointing at the per-colour
 * {@link WorldFeatures#configuredTreeKey configured feature} as the {@code normalTree}
 * argument. SMTCON-55 wired the real keys; the bonemeal-grow flow now invokes
 * {@link net.minecraft.world.level.levelgen.feature.Feature#TREE} via {@code TreeGrower}'s
 * internal advance pipeline, producing a 5-block trunk + 2-radius foliage canopy in the
 * matching colour. Mega / flowering tree slots stay empty — slime trees ship one shape.
 */
public final class SlimeSaplingBlock extends SaplingBlock {

    private final SlimeColor color;

    public SlimeSaplingBlock(SlimeColor color, BlockBehaviour.Properties properties) {
        super(growerFor(color), properties);
        this.color = Objects.requireNonNull(color, "color");
    }

    /** The slime colour this sapling block was registered with. */
    public SlimeColor color() {
        return color;
    }

    /**
     * Build the per-colour {@link TreeGrower}. The {@code name} doubles as the
     * {@code TreeGrower}'s self-reference id; the {@code normalTree} optional points at the
     * configured-feature key {@link WorldFeatures} writes during datagen, so a bonemeal grow
     * resolves to the matching slime-coloured tree.
     */
    private static TreeGrower growerFor(SlimeColor color) {
        Objects.requireNonNull(color, "color");
        return new TreeGrower("sconstruct:slime_" + color.id(), Optional.empty(), Optional.of(WorldFeatures.configuredTreeKey(color)), Optional.empty());
    }
}
