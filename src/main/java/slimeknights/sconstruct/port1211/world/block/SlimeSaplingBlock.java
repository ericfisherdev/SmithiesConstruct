package slimeknights.sconstruct.port1211.world.block;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Sapling for a coloured slime tree. Extends vanilla {@link SaplingBlock} so the stage tracking
 * (sapling → tree advance over multiple random ticks), bonemeal target detection, and the
 * "must stand on dirt-or-grass" placement check inherit verbatim.
 *
 * <p>Each colour ships its own {@link TreeGrower} instance via {@link #stubGrowerFor}; SMTCON-55
 * replaces the empty {@link Optional}s with real {@link
 * net.minecraft.data.worldgen.features.TreeFeatures}-equivalent configured features. Until then
 * a bonemeal application consumes the bonemeal and advances the sapling stage; the
 * {@code advanceTree} call at stage 1 returns {@code false} from the empty grower so the
 * sapling simply disappears without producing a tree. The block, item, and placement
 * mechanics are nevertheless complete and shippable.
 */
public final class SlimeSaplingBlock extends SaplingBlock {

    private final SlimeColor color;

    public SlimeSaplingBlock(SlimeColor color, BlockBehaviour.Properties properties) {
        super(stubGrowerFor(color), properties);
        this.color = Objects.requireNonNull(color, "color");
    }

    /** The slime colour this sapling block was registered with. */
    public SlimeColor color() {
        return color;
    }

    /**
     * Build a placeholder {@link TreeGrower} for the supplied colour. SMTCON-55 will widen the
     * empty {@link Optional}s into actual configured-feature keys for the per-colour slime
     * tree shapes. Until then the grower carries a stable name so the resource key emitted by
     * datagen and runtime is unambiguous.
     */
    private static TreeGrower stubGrowerFor(SlimeColor color) {
        Objects.requireNonNull(color, "color");
        return new TreeGrower("sconstruct:slime_" + color.id(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
