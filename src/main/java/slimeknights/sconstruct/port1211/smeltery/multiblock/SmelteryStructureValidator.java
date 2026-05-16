package slimeknights.sconstruct.port1211.smeltery.multiblock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import slimeknights.sconstruct.port1211.smeltery.SearedBlocks;
import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;

/**
 * Hand-rolled multiblock validator for the smeltery (SMTCON-115). Given the controller's
 * position and the horizontal direction from the controller into the smeltery, it walks the
 * surrounding blocks and — if they form a well-formed bowl — returns the {@link SmelteryStructure}
 * describing it.
 *
 * <p><strong>The shape it accepts.</strong> A valid v1 smeltery is a rectangular open-topped
 * bowl: a solid seared floor slab of up to {@value #MAX_INTERIOR_SIZE}&times;{@value
 * #MAX_INTERIOR_SIZE} blocks, a seared wall ring {@value #MIN_WALL_HEIGHT}–{@value
 * #MAX_WALL_HEIGHT} blocks tall enclosing the interior on all four sides, an interior column
 * that is clear of obstructions, and an open (air) layer above. The controller is itself one of
 * the wall blocks, sitting in the bottom ring directly behind the interior.
 *
 * <p><strong>Why a {@link BlockClassifier} indirection.</strong> The world-facing
 * {@link #validate(LevelReader, BlockPos, Direction)} entry point reduces each block to a single
 * {@link BlockRole} up front, then runs the geometry walk against that reduction. Splitting the
 * "what is this block" question from the "do these blocks form a bowl" question keeps the walk a
 * pure function of {@code (classifier, controllerPos, facing)} — which is what the unit tests
 * exercise via {@link #validate(BlockClassifier, BlockPos, Direction)}, with no live
 * {@code Level} and no frozen block registry needed.
 *
 * <p>The walk performs no allocation per block beyond the result collections and visits each
 * shell block a small constant number of times, so a maximal {@value #MAX_INTERIOR_SIZE}&times;{@value
 * #MAX_INTERIOR_SIZE}&times;{@value #MAX_WALL_HEIGHT} validation completes well under a millisecond.
 */
public final class SmelteryStructureValidator {

    /** Maximum interior width and depth in blocks for a v1 smeltery. */
    public static final int MAX_INTERIOR_SIZE = 5;

    /** Minimum wall height in blocks — a smeltery must enclose at least one interior layer. */
    public static final int MIN_WALL_HEIGHT = 1;

    /** Maximum wall height in blocks for a v1 smeltery. */
    public static final int MAX_WALL_HEIGHT = 4;

    /** The number of controller blocks a well-formed smeltery has — exactly one. */
    private static final int REQUIRED_CONTROLLER_COUNT = 1;

    private SmelteryStructureValidator() {
    }

    /**
     * Classification of a single block as the validator sees it. Plain seared construction
     * blocks are {@link #STRUCTURE}; the four functional component blocks each get their own
     * role so the walk can both treat them as valid wall material and catalogue their type and
     * position; air is {@link #INTERIOR}; everything else is {@link #INVALID} and aborts validation.
     */
    public enum BlockRole {
        /** A plain seared construction block — valid floor or wall material, no behaviour. */
        STRUCTURE(false, null),
        /** Air — valid interior volume and valid open top; any non-air block is {@link #INVALID}. */
        INTERIOR(false, null),
        /** Any block that cannot be part of a smeltery — aborts validation. */
        INVALID(false, null),
        /** The smeltery controller — valid wall material, catalogued as a component. */
        CONTROLLER(true, ComponentType.CONTROLLER),
        /** A seared tank — valid wall material, catalogued as a component. */
        TANK(true, ComponentType.TANK),
        /** A seared drain — valid wall material, catalogued as a component. */
        DRAIN(true, ComponentType.DRAIN),
        /** A seared chute — valid wall material, catalogued as a component. */
        CHUTE(true, ComponentType.CHUTE);

        private final boolean component;
        private final ComponentType componentType;

        BlockRole(boolean component, ComponentType componentType) {
            this.component = component;
            this.componentType = componentType;
        }

        /** Whether a block of this role may form part of the smeltery wall ring. */
        boolean isWall() {
            return this == STRUCTURE || component;
        }

        /** Whether this role is a functional component block that must be catalogued by position. */
        boolean isComponent() {
            return component;
        }

        /** The component type to catalogue this block as, or {@code null} when {@link #isComponent()} is false. */
        ComponentType componentType() {
            return componentType;
        }
    }

    /** Reduces a world position to the single {@link BlockRole} the geometry walk operates on. */
    @FunctionalInterface
    public interface BlockClassifier {
        BlockRole classify(BlockPos pos);
    }

    /** The four horizontal directions the interior flood-fill expands along. */
    private static final Direction[] HORIZONTAL = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

    /**
     * Initialization-on-demand holder for the plain seared construction blocks. Resolution is
     * deferred out of {@link SmelteryStructureValidator}'s own class-init because
     * {@link SearedBlocks}' holders only carry a block once the block registry has frozen —
     * touching them at class-load time would race mod construction. The JVM guarantees this
     * nested class is initialised exactly once, on first access, with no explicit locking.
     */
    private static final class StructureBlocksHolder {
        private static final Set<Block> BLOCKS;

        static {
            Set<Block> resolved = new HashSet<>();
            SearedBlocks.ALL.forEach(holder -> resolved.add(holder.get()));
            BLOCKS = Set.copyOf(resolved);
        }

        private StructureBlocksHolder() {
        }
    }

    /**
     * Validate the smeltery around {@code controllerPos} in a live world.
     *
     * @param level         the world to read blocks from
     * @param controllerPos the position of the smeltery controller block
     * @param facing        the horizontal direction from the controller into the interior
     * @return the assembled structure, or empty if the surrounding blocks do not form a smeltery
     */
    public static Optional<SmelteryStructure> validate(LevelReader level, BlockPos controllerPos, Direction facing) {
        Objects.requireNonNull(level, "level");
        return validate(classifierFor(level), controllerPos, facing);
    }

    /** A {@link BlockClassifier} backed by a live world's block states. */
    public static BlockClassifier classifierFor(LevelReader level) {
        Objects.requireNonNull(level, "level");
        return pos -> roleOf(level.getBlockState(pos));
    }

    /**
     * Validate the smeltery against a pre-reduced view of the world. This is the pure core of
     * the validator: the result depends only on the three arguments, which is what makes it
     * unit-testable without a {@code Level}.
     *
     * @param classifier    the per-position block-role view of the world
     * @param controllerPos the position of the smeltery controller block
     * @param facing        the horizontal direction from the controller into the interior
     * @return the assembled structure, or empty if the classified blocks do not form a smeltery
     */
    public static Optional<SmelteryStructure> validate(BlockClassifier classifier, BlockPos controllerPos, Direction facing) {
        Objects.requireNonNull(classifier, "classifier");
        Objects.requireNonNull(controllerPos, "controllerPos");
        Objects.requireNonNull(facing, "facing");
        if (facing.getAxis().isVertical()) {
            return Optional.empty();
        }
        if (classifier.classify(controllerPos) != BlockRole.CONTROLLER) {
            return Optional.empty();
        }

        // Step 1: the block in front of the controller is the first interior cell, and the
        // block directly under it is the floor — so the controller sits in the bottom wall ring.
        BlockPos interiorStart = controllerPos.relative(facing);
        if (classifier.classify(interiorStart) != BlockRole.INTERIOR) {
            return Optional.empty();
        }
        if (classifier.classify(interiorStart.below()) != BlockRole.STRUCTURE) {
            return Optional.empty();
        }
        int interiorBaseY = interiorStart.getY();

        // Step 2: flood-fill the interior base layer to discover the bowl's footprint.
        Optional<Set<BlockPos>> floodResult = floodInteriorBase(classifier, interiorStart);
        if (floodResult.isEmpty()) {
            return Optional.empty();
        }
        Set<BlockPos> interiorBase = floodResult.get();
        Bounds footprint = Bounds.of(interiorBase);
        // The footprint must fit within the v1 interior size cap.
        if (footprint.width() > MAX_INTERIOR_SIZE || footprint.depth() > MAX_INTERIOR_SIZE) {
            return Optional.empty();
        }
        // The footprint must be a gap-free rectangle — a flood that stops short of filling its
        // own bounding rectangle means the floor has a notch or hole.
        if (interiorBase.size() != footprint.width() * footprint.depth()) {
            return Optional.empty();
        }

        // Step 3: the floor slab is the layer of seared blocks directly under the interior.
        Set<BlockPos> floor = new HashSet<>(interiorBase.size() * 2);
        for (BlockPos cell : interiorBase) {
            BlockPos below = cell.below();
            if (classifier.classify(below) != BlockRole.STRUCTURE) {
                return Optional.empty();
            }
            floor.add(below);
        }

        // Step 4: stack wall rings upward while each ring fully encloses a clear interior layer.
        Set<BlockPos> walls = new HashSet<>();
        Map<BlockPos, ComponentType> components = new HashMap<>();
        int controllerCount = 0;
        int height = 0;
        while (height < MAX_WALL_HEIGHT) {
            int y = interiorBaseY + height;
            List<BlockPos> ring = ringAt(footprint, y);
            if (!ringIsWall(classifier, ring)) {
                break;
            }
            // A ring of walls around a blocked interior layer is not a smeltery — abort rather
            // than silently accepting a bowl the player cannot pour metal into.
            if (!interiorLayerIsClear(classifier, interiorBase, y)) {
                return Optional.empty();
            }
            for (BlockPos wall : ring) {
                walls.add(wall);
                BlockRole role = classifier.classify(wall);
                if (role == BlockRole.CONTROLLER) {
                    controllerCount++;
                }
                if (role.isComponent()) {
                    components.put(wall, role.componentType());
                }
            }
            height++;
        }
        if (height < MIN_WALL_HEIGHT) {
            return Optional.empty();
        }
        // A complete ring one layer above the cap means the shell is taller than the v1 limit —
        // reject rather than silently treating an over-tall shell as a MAX_WALL_HEIGHT smeltery.
        if (height == MAX_WALL_HEIGHT && ringIsWall(classifier, ringAt(footprint, interiorBaseY + height))) {
            return Optional.empty();
        }
        // A smeltery requires exactly one controller; the controller at controllerPos sits in
        // the bottom wall ring and so is counted during ring iteration — it must be the sole
        // CONTROLLER-role block encountered.
        if (controllerCount != REQUIRED_CONTROLLER_COUNT) {
            return Optional.empty();
        }

        // The layer above the topmost wall ring must be open so the bowl is genuinely open-topped.
        if (!interiorLayerIsClear(classifier, interiorBase, interiorBaseY + height)) {
            return Optional.empty();
        }

        BoundingBox bounds = new BoundingBox(footprint.minX, interiorBaseY, footprint.minZ, footprint.maxX, interiorBaseY + height - 1, footprint.maxZ);
        int bowlVolume = footprint.width() * footprint.depth() * height;
        return Optional.of(new SmelteryStructure(bounds, floor, walls, components, bowlVolume));
    }

    /**
     * Flood-fill the interior base layer from {@code start} across {@link BlockRole#INTERIOR}
     * cells. Returns empty if the region overflows the {@value #MAX_INTERIOR_SIZE}-square cap,
     * which both bounds the walk and rejects an unenclosed (leaking) interior.
     */
    private static Optional<Set<BlockPos>> floodInteriorBase(BlockClassifier classifier, BlockPos start) {
        int cap = MAX_INTERIOR_SIZE * MAX_INTERIOR_SIZE;
        Set<BlockPos> interior = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        interior.add(start);
        frontier.add(start);
        while (!frontier.isEmpty()) {
            BlockPos cell = frontier.poll();
            for (Direction dir : HORIZONTAL) {
                BlockPos neighbour = cell.relative(dir);
                if (interior.contains(neighbour)) {
                    continue;
                }
                if (classifier.classify(neighbour) == BlockRole.INTERIOR) {
                    if (interior.size() >= cap) {
                        return Optional.empty();
                    }
                    interior.add(neighbour);
                    frontier.add(neighbour);
                }
            }
        }
        return Optional.of(interior);
    }

    /** Whether every block in {@code ring} is valid wall material. */
    private static boolean ringIsWall(BlockClassifier classifier, List<BlockPos> ring) {
        for (BlockPos pos : ring) {
            if (!classifier.classify(pos).isWall()) {
                return false;
            }
        }
        return true;
    }

    /** Whether every interior cell, lifted to height {@code y}, is clear ({@link BlockRole#INTERIOR}). */
    private static boolean interiorLayerIsClear(BlockClassifier classifier, Set<BlockPos> interiorBase, int y) {
        for (BlockPos cell : interiorBase) {
            if (classifier.classify(new BlockPos(cell.getX(), y, cell.getZ())) != BlockRole.INTERIOR) {
                return false;
            }
        }
        return true;
    }

    /**
     * The wall-ring positions enclosing {@code footprint} at height {@code y} — the border of
     * the footprint rectangle expanded outward by one block. Corners are emitted once.
     */
    private static List<BlockPos> ringAt(Bounds footprint, int y) {
        int x0 = footprint.minX - 1;
        int x1 = footprint.maxX + 1;
        int z0 = footprint.minZ - 1;
        int z1 = footprint.maxZ + 1;
        List<BlockPos> ring = new ArrayList<>(2 * (x1 - x0 + 1) + 2 * (z1 - z0 - 1));
        for (int x = x0; x <= x1; x++) {
            ring.add(new BlockPos(x, y, z0));
            ring.add(new BlockPos(x, y, z1));
        }
        for (int z = z0 + 1; z < z1; z++) {
            ring.add(new BlockPos(x0, y, z));
            ring.add(new BlockPos(x1, y, z));
        }
        return ring;
    }

    /**
     * Whether {@code state} is a block the smeltery shell is built from — a plain seared
     * construction block or one of the functional component blocks. Breaking such a block can
     * change a smeltery's validity, so the SMTCON-117 disassembly listener uses this to decide
     * whether a {@code BlockEvent.BreakEvent} is worth re-validating a controller for.
     */
    public static boolean isSmelteryShellBlock(BlockState state) {
        BlockRole role = roleOf(state);
        return role == BlockRole.STRUCTURE || role.isComponent();
    }

    /** Classify a world block state into the {@link BlockRole} the geometry walk consumes. */
    private static BlockRole roleOf(BlockState state) {
        if (state.is(SmelteryComponents.SMELTERY_CONTROLLER.get())) {
            return BlockRole.CONTROLLER;
        }
        if (state.is(SmelteryComponents.SEARED_DRAIN.get())) {
            return BlockRole.DRAIN;
        }
        if (state.is(SmelteryComponents.SEARED_CHUTE.get())) {
            return BlockRole.CHUTE;
        }
        if (state.is(SmelteryComponents.SEARED_TANK_IO.get()) || state.is(SmelteryComponents.SEARED_TANK_IN.get()) || state.is(SmelteryComponents.SEARED_TANK_GAUGE.get())) {
            return BlockRole.TANK;
        }
        if (structureBlocks().contains(state.getBlock())) {
            return BlockRole.STRUCTURE;
        }
        if (state.isAir()) {
            return BlockRole.INTERIOR;
        }
        return BlockRole.INVALID;
    }

    /** The set of plain seared construction blocks, resolved once on first access. */
    private static Set<Block> structureBlocks() {
        return StructureBlocksHolder.BLOCKS;
    }

    /**
     * Immutable bounding rectangle holding the min/max x and z extents of a set of positions.
     * The extents are accumulated inside {@link #of(Set)}; a constructed instance never mutates.
     */
    private static final class Bounds {
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;

        private Bounds(int minX, int maxX, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        static Bounds of(Set<BlockPos> positions) {
            if (positions.isEmpty()) {
                throw new IllegalArgumentException("cannot compute bounds of an empty position set");
            }
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (BlockPos pos : positions) {
                minX = Math.min(minX, pos.getX());
                maxX = Math.max(maxX, pos.getX());
                minZ = Math.min(minZ, pos.getZ());
                maxZ = Math.max(maxZ, pos.getZ());
            }
            return new Bounds(minX, maxX, minZ, maxZ);
        }

        int width() {
            return maxX - minX + 1;
        }

        int depth() {
            return maxZ - minZ + 1;
        }
    }
}
