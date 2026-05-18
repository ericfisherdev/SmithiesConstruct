package slimeknights.sconstruct.world.block;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Coloured slime block. Extends vanilla {@link SlimeBlock} so the bounce-suppression check,
 * {@code updateEntityAfterFallOn}, and slow-walk {@code stepOn} inherit verbatim — the only
 * thing this subclass adds is a {@link SlimeColor} field plus a {@code fallOn} override that
 * dispatches the colour's optional side effect ({@code MAGMA} damages, {@code BLOOD} heals)
 * <em>before</em> calling super so the bounce mechanics still fire on the same tick.
 *
 * <p>Held final so subclassing this further would have to be a deliberate API change; the
 * current design keeps per-colour behaviour in the enum {@link SlimeColor} rather than in
 * subclasses, which keeps the inheritance hierarchy shallow and the unit-test surface
 * mockable.
 */
public final class TinkerSlimeBlock extends SlimeBlock {

    private final SlimeColor color;

    public TinkerSlimeBlock(SlimeColor color, BlockBehaviour.Properties properties) {
        super(properties);
        this.color = Objects.requireNonNull(color, "color");
    }

    /** The colour this instance was registered with. Drives the in-game side effect on landing. */
    public SlimeColor color() {
        return color;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        // Apply the colour's side effect first so a magma-block landing applies fire damage in
        // the same tick the bounce kicks off — sequencing matters because super.fallOn may
        // reset fallDistance to zero on the entity once the bounce mechanics run.
        color.applyFallEffect(level, entity, fallDistance);
        super.fallOn(level, state, pos, entity, fallDistance);
    }
}
