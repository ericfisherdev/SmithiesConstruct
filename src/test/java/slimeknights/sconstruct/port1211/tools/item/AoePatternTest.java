package slimeknights.sconstruct.port1211.tools.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void full3x3PerpendicularToUpReturnsEightHorizontalNeighbours() {
        // Striking the top of a block expands across the XZ plane; the centre is excluded.
        List<BlockPos> positions = AoePattern.FULL3x3.positions(CENTRE, Direction.UP);
        assertEquals(8, positions.size());
        for (BlockPos pos : positions) {
            assertEquals(0, pos.getY(), "FULL3x3 perpendicular to UP must stay on the XZ plane");
            assertFalse(pos.equals(CENTRE), "centre must be excluded — vanilla mineBlock already broke it");
        }
    }

    @Test
    void full3x3PerpendicularToNorthExpandsAcrossXY() {
        // Striking the north face expands across the XY plane — vertical reach plus the X axis.
        List<BlockPos> positions = AoePattern.FULL3x3.positions(CENTRE, Direction.NORTH);
        assertEquals(8, positions.size());
        for (BlockPos pos : positions) {
            assertEquals(0, pos.getZ(), "FULL3x3 perpendicular to NORTH must stay on the XY plane");
        }
    }

    @Test
    void plus3x3PerpendicularToUpReturnsFourCardinals() {
        Set<BlockPos> positions = Set.copyOf(AoePattern.PLUS3x3.positions(CENTRE, Direction.UP));
        assertEquals(4, positions.size());
        assertTrue(positions.contains(CENTRE.east()));
        assertTrue(positions.contains(CENTRE.west()));
        assertTrue(positions.contains(CENTRE.north()));
        assertTrue(positions.contains(CENTRE.south()));
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
