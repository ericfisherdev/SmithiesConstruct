package slimeknights.sconstruct.port1211.tools.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

/**
 * Pinned-shape tests for {@link AoePattern}. The pattern math is pure so positions can be checked
 * without any world setup — the legacy hammer / excavator / mattock shape contract is asserted
 * here so a future refactor that breaks the perpendicular-to-face plane selection fails loudly.
 */
class AoePatternTest {

    private static final BlockPos CENTRE = BlockPos.ZERO;

    @Test
    void full3x3PerpendicularToUpReturnsExactEightHorizontalNeighbours() {
        // Pin the exact 3x3 ring around the centre on the XZ plane so a bent / off-axis pattern
        // (e.g. wrong axis pair, drifted offset) fails loudly rather than passing on size alone.
        Set<BlockPos> positions = Set.copyOf(AoePattern.FULL3x3.positions(CENTRE, Direction.UP));
        Set<BlockPos> expected = Set.of(CENTRE.offset(-1, 0, -1), CENTRE.offset(0, 0, -1), CENTRE.offset(1, 0, -1), CENTRE.offset(-1, 0, 0), CENTRE.offset(1, 0, 0), CENTRE.offset(-1, 0, 1),
                CENTRE.offset(0, 0, 1), CENTRE.offset(1, 0, 1));
        assertEquals(expected, positions);
    }

    @Test
    void full3x3PerpendicularToNorthReturnsExactEightXyNeighbours() {
        Set<BlockPos> positions = Set.copyOf(AoePattern.FULL3x3.positions(CENTRE, Direction.NORTH));
        Set<BlockPos> expected = Set.of(CENTRE.offset(-1, -1, 0), CENTRE.offset(0, -1, 0), CENTRE.offset(1, -1, 0), CENTRE.offset(-1, 0, 0), CENTRE.offset(1, 0, 0), CENTRE.offset(-1, 1, 0),
                CENTRE.offset(0, 1, 0), CENTRE.offset(1, 1, 0));
        assertEquals(expected, positions);
    }

    @Test
    void plus3x3PerpendicularToUpReturnsFourHorizontalCardinals() {
        Set<BlockPos> positions = Set.copyOf(AoePattern.PLUS3x3.positions(CENTRE, Direction.UP));
        assertEquals(Set.of(CENTRE.east(), CENTRE.west(), CENTRE.north(), CENTRE.south()), positions);
    }

    @Test
    void plus3x3PerpendicularToNorthReturnsVerticalAndHorizontalCardinals() {
        Set<BlockPos> positions = Set.copyOf(AoePattern.PLUS3x3.positions(CENTRE, Direction.NORTH));
        assertEquals(Set.of(CENTRE.above(), CENTRE.below(), CENTRE.east(), CENTRE.west()), positions);
    }

    @Test
    void column1x3ReturnsAboveAndBelowIgnoringFace() {
        // COLUMN_1x3 ignores the struck face — mattock dig always extends vertically.
        for (Direction face : Direction.values()) {
            List<BlockPos> positions = AoePattern.COLUMN_1x3.positions(CENTRE, face);
            assertEquals(List.of(CENTRE.above(), CENTRE.below()), positions, "face " + face + " must not change column orientation");
        }
    }

    @Test
    void treeReturnsEmptyHere() {
        // The tree walk runs in AoeHelper#walkConnectedLogs because it needs world access;
        // the enum-level position generator returns empty so callers that don't route through
        // AoeHelper get a safe no-op rather than a partial pattern.
        assertEquals(List.of(), AoePattern.TREE.positions(CENTRE, Direction.UP));
    }
}
