package slimeknights.sconstruct.port1211.world.block;

import java.util.Objects;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Dirt-like base block for slime islands, in one of the {@link SlimeColor} variants. Vanilla
 * {@code Blocks.DIRT} is hardcoded throughout the dirt-spreading machinery — instead of fighting
 * that, slime dirt is a plain {@link Block} and the matching {@link SlimeGrassBlock} owns the
 * spread-from-dirt-to-grass behaviour explicitly.
 *
 * <p>{@link #color()} is read by the grass-spreading randomTick to confine spread to the
 * matching colour: blue grass can spread onto blue dirt, not purple dirt.
 */
public final class SlimeDirtBlock extends Block {

    private final SlimeColor color;

    public SlimeDirtBlock(SlimeColor color, BlockBehaviour.Properties properties) {
        super(properties);
        this.color = Objects.requireNonNull(color, "color");
    }

    /** The slime colour this dirt block was registered with. */
    public SlimeColor color() {
        return color;
    }
}
