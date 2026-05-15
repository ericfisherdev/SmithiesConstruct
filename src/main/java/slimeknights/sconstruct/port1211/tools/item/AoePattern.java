package slimeknights.sconstruct.port1211.tools.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * AOE break pattern selector for {@link AoeToolCore}. Pattern position generation is pure: given
 * the centre {@link BlockPos} struck by the player and the {@link Direction} of the face hit, the
 * pattern returns the additional positions (excluding the centre) the tool should attempt to
 * break. The centre is excluded from the returned list because vanilla's {@code mineBlock} hook
 * has already destroyed it by the time {@code AoeToolCore#mineBlock} fires — the AoE pass only
 * needs to extend outwards.
 *
 * <p>Pattern axes follow the legacy 1.12 hammer / excavator / scythe convention: the 3x3 plane
 * is perpendicular to the face the player struck. Striking the top of a block expands sideways
 * (XZ plane); striking a vertical side expands vertically and across the remaining horizontal
 * axis. The TREE pattern is special-cased and ignores {@code face} — see
 * {@link AoeHelper#walkConnectedLogs}.
 */
public enum AoePattern {
    /** Centre + 4 cardinal neighbours on the plane perpendicular to {@code face}. */
    PLUS3x3,
    /** 3x3 square on the plane perpendicular to {@code face} (centre excluded from return). */
    FULL3x3,
    /** 1x3 vertical column above and below the centre — for the mattock dig stroke. */
    COLUMN_1x3,
    /** Recursive walk over connected logs — driven by {@link AoeHelper#walkConnectedLogs}. */
    TREE;

    /**
     * Returns the set of positions the AoE pass should attempt to break, excluding {@code centre}.
     * The legacy hammer / excavator pattern is a 3x3 perpendicular to the struck face — see the
     * per-constant doc on this enum. {@link #TREE} returns an empty list here; the tree walk runs
     * in {@link AoeHelper} because it needs world access (the log connectivity check reads
     * neighbour block states).
     */
    public List<BlockPos> positions(BlockPos centre, Direction face) {
        List<BlockPos> positions = new ArrayList<>();
        switch (this) {
        case PLUS3x3:
            addPlanePlus(positions, centre, face);
            break;
        case FULL3x3:
            addPlaneFull(positions, centre, face);
            break;
        case COLUMN_1x3:
            positions.add(centre.above());
            positions.add(centre.below());
            break;
        case TREE:
            // Empty here — AoeHelper.walkConnectedLogs replaces this generator for trees.
            break;
        }
        return List.copyOf(positions);
    }

    /**
     * Pick the two unit-vector directions that span the plane perpendicular to {@code face}.
     * Striking the top / bottom of a block expands across X / Z; striking a side expands across
     * Y plus the remaining horizontal axis.
     */
    private static Direction[] planeAxes(Direction face) {
        return switch (face.getAxis()) {
        case Y -> new Direction[] { Direction.EAST, Direction.SOUTH };
        case X -> new Direction[] { Direction.UP, Direction.SOUTH };
        case Z -> new Direction[] { Direction.UP, Direction.EAST };
        };
    }

    private static void addPlanePlus(List<BlockPos> positions, BlockPos centre, Direction face) {
        Direction[] axes = planeAxes(face);
        positions.add(centre.relative(axes[0]));
        positions.add(centre.relative(axes[0].getOpposite()));
        positions.add(centre.relative(axes[1]));
        positions.add(centre.relative(axes[1].getOpposite()));
    }

    private static void addPlaneFull(List<BlockPos> positions, BlockPos centre, Direction face) {
        Direction[] axes = planeAxes(face);
        for (int u = -1; u <= 1; u++) {
            for (int v = -1; v <= 1; v++) {
                if (u == 0 && v == 0) {
                    continue;
                }
                positions.add(centre.relative(axes[0], u).relative(axes[1], v));
            }
        }
    }
}
