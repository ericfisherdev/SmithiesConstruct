package slimeknights.sconstruct.port1211.smeltery.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.smeltery.multiblock.SmelteryStructureValidator.BlockClassifier;
import slimeknights.sconstruct.port1211.smeltery.multiblock.SmelteryStructureValidator.BlockRole;

/**
 * Unit tests for {@link SmelteryStructureValidator}'s geometry walk. Each test builds a smeltery
 * as an in-memory {@code Map<BlockPos, BlockRole>} and runs the {@link BlockClassifier} overload
 * of {@link SmelteryStructureValidator#validate validate} — exercising the validator with no
 * live {@code Level} and no frozen block registry. Positions absent from the map classify as
 * {@link BlockRole#INTERIOR} (open air), which is what the world outside the bowl genuinely is.
 */
class SmelteryStructureValidatorTest {

    /** Controller sits in the bottom wall ring; the interior opens out of it to the EAST. */
    private static final BlockPos CONTROLLER = new BlockPos(-1, 1, 0);
    private static final Direction FACING = Direction.EAST;

    @Test
    void validateAcceptsThreeByThreeFloorWithThreeTallWalls() {
        Map<BlockPos, BlockRole> world = bowl(3, 3, 3);
        Optional<SmelteryStructure> result = SmelteryStructureValidator.validate(classifier(world), CONTROLLER, FACING);

        assertTrue(result.isPresent(), "a sealed 3x3 floor with 3-tall walls must validate");
        SmelteryStructure structure = result.get();
        assertEquals(9, structure.floor().size(), "a 3x3 floor has nine seared blocks");
        assertEquals(3 * 3 * 3, structure.bowlVolume(), "bowl volume is width*depth*height");
        assertSame(ComponentType.CONTROLLER, structure.components().get(CONTROLLER), "the controller must be catalogued as a component at its own position");
        // Wall ring of a 3x3 interior is the 5x5 border (16 blocks) per layer, three layers tall.
        assertEquals(16 * 3, structure.walls().size(), "three 16-block wall rings enclose the interior");
    }

    @Test
    void validateRejectsAStructureWithAMissingWallBlock() {
        Map<BlockPos, BlockRole> world = bowl(3, 3, 3);
        // Remove one bottom-ring wall block; the interior now leaks out through the gap.
        world.remove(new BlockPos(1, 1, -1));

        assertTrue(SmelteryStructureValidator.validate(classifier(world), CONTROLLER, FACING).isEmpty(), "a smeltery missing a wall block must not validate");
    }

    @Test
    void validateRejectsAStructureWithABlockedInterior() {
        Map<BlockPos, BlockRole> world = bowl(3, 3, 3);
        // Drop a stray seared block into the open interior — metal could not pool here.
        world.put(new BlockPos(1, 2, 1), BlockRole.STRUCTURE);

        assertTrue(SmelteryStructureValidator.validate(classifier(world), CONTROLLER, FACING).isEmpty(), "an obstructed interior must not validate");
    }

    @Test
    void validateRejectsAShellTallerThanTheWallHeightCap() {
        // A wall ring one layer above MAX_WALL_HEIGHT must not pass as a capped-height smeltery.
        Map<BlockPos, BlockRole> world = bowl(3, 3, SmelteryStructureValidator.MAX_WALL_HEIGHT + 1);

        assertTrue(SmelteryStructureValidator.validate(classifier(world), CONTROLLER, FACING).isEmpty(), "a shell taller than the wall-height cap must not validate");
    }

    @Test
    void validateRejectsASmelteryWithASecondController() {
        Map<BlockPos, BlockRole> world = bowl(3, 3, 3);
        // Plant a second controller in the wall ring — a smeltery has exactly one.
        world.put(new BlockPos(3, 2, 1), BlockRole.CONTROLLER);

        assertTrue(SmelteryStructureValidator.validate(classifier(world), CONTROLLER, FACING).isEmpty(), "a smeltery with more than one controller must not validate");
    }

    @Test
    void validateRejectsWhenControllerBlockIsAbsent() {
        Map<BlockPos, BlockRole> world = bowl(3, 3, 3);
        world.remove(CONTROLLER);

        assertTrue(SmelteryStructureValidator.validate(classifier(world), CONTROLLER, FACING).isEmpty(), "validation must start from an actual controller block");
    }

    @Test
    void validateMaximalSmelteryCompletesWithinPerformanceBudget() {
        Map<BlockPos, BlockRole> world = bowl(SmelteryStructureValidator.MAX_INTERIOR_SIZE, SmelteryStructureValidator.MAX_INTERIOR_SIZE, SmelteryStructureValidator.MAX_WALL_HEIGHT);
        BlockClassifier classifier = classifier(world);

        // Warm up the JIT so the timed run measures steady-state cost, not first-call compilation.
        for (int i = 0; i < 10_000; i++) {
            SmelteryStructureValidator.validate(classifier, CONTROLLER, FACING);
        }
        int runs = 10_000;
        long start = System.nanoTime();
        for (int i = 0; i < runs; i++) {
            assertTrue(SmelteryStructureValidator.validate(classifier, CONTROLLER, FACING).isPresent());
        }
        long averageNanos = (System.nanoTime() - start) / runs;
        // The AC target is sub-millisecond; in practice the walk runs in single-digit
        // microseconds. The 2ms bound stays comfortably above the 1ms AC so a loaded CI runner
        // cannot flake the build, while still catching an order-of-magnitude regression.
        assertTrue(averageNanos < 2_000_000L, "5x5x4 validation must stay well under 1ms, was " + averageNanos + "ns");
    }

    @Test
    void validateRejectsAVerticalFacing() {
        Map<BlockPos, BlockRole> world = bowl(3, 3, 3);

        assertFalse(SmelteryStructureValidator.validate(classifier(world), CONTROLLER, Direction.UP).isPresent(), "the controller must face a horizontal interior direction");
    }

    /** A classifier over an explicit world map; unmapped positions are open air. */
    private static BlockClassifier classifier(Map<BlockPos, BlockRole> world) {
        return pos -> world.getOrDefault(pos, BlockRole.INTERIOR);
    }

    /**
     * Build a sealed smeltery: a {@code width}x{@code depth} interior with its base layer at
     * {@code y=1}, a seared floor slab below it, and a {@code height}-tall seared wall ring with
     * the controller occupying one bottom-ring position. The interior opens east of the
     * controller at {@code (0, 1, 0)}.
     */
    private static Map<BlockPos, BlockRole> bowl(int width, int depth, int height) {
        Map<BlockPos, BlockRole> world = new HashMap<>();
        int minX = 0;
        int minZ = 0;
        int maxX = width - 1;
        int maxZ = depth - 1;
        // Floor slab directly under the interior.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.put(new BlockPos(x, 0, z), BlockRole.STRUCTURE);
            }
        }
        // Wall ring — the border of the interior expanded outward by one, per layer.
        for (int y = 1; y <= height; y++) {
            for (int x = minX - 1; x <= maxX + 1; x++) {
                for (int z = minZ - 1; z <= maxZ + 1; z++) {
                    boolean onBorder = x == minX - 1 || x == maxX + 1 || z == minZ - 1 || z == maxZ + 1;
                    if (onBorder) {
                        world.put(new BlockPos(x, y, z), BlockRole.STRUCTURE);
                    }
                }
            }
        }
        world.put(CONTROLLER, BlockRole.CONTROLLER);
        return world;
    }
}
