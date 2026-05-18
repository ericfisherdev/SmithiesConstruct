package slimeknights.sconstruct.world.block;

import java.util.Objects;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Leaves for a coloured slime tree. Extends vanilla {@link LeavesBlock} so the decay-when-far-
 * from-a-log mechanics, persistent / distance state values, and the "drop sapling sometimes"
 * loot pipeline inherit verbatim. The only thing this subclass adds is a {@link SlimeColor}
 * field so a {@link
 * net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block} listener in a later
 * task can pick the correct tint index per colour.
 *
 * <p>Per-colour leaf textures ship pre-tinted under {@code sconstruct:block/slime_<color>_leaves},
 * so the live block-render tint multiplier is a no-op for now (the texture sampler returns the
 * coloured pixel directly). The tint hook is registered anyway so future work can layer a
 * dynamic biome-driven tint on top.
 */
public final class SlimeLeavesBlock extends LeavesBlock {

    private final SlimeColor color;

    public SlimeLeavesBlock(SlimeColor color, BlockBehaviour.Properties properties) {
        super(properties);
        this.color = Objects.requireNonNull(color, "color");
    }

    /** The slime colour this leaves block was registered with. */
    public SlimeColor color() {
        return color;
    }
}
