package slimeknights.sconstruct.port1211.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import slimeknights.sconstruct.port1211.world.WorldBlocks;
import slimeknights.sconstruct.port1211.world.WorldStructures;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;
import slimeknights.sconstruct.port1211.world.block.SlimePlantSet;

/**
 * Single-piece structure body for {@link SlimeIslandStructure}. Lays down a horizontal disc of
 * the matching colour's {@link SlimePlantSet#dirt() slime dirt} at the anchor Y, the
 * {@link SlimePlantSet#grass() slime grass} cap one block above, and a small spray of
 * {@link SlimePlantSet#leaves() leaves} plus 1–2 {@link SlimePlantSet#sapling() saplings}
 * placed within the disc footprint.
 *
 * <p>NBT round-trips three values: the {@link SlimeColor} ordinal (so reloading a save resolves
 * the matching plant set), the sampled radius (so post-processing can re-derive the same disc
 * shape if the piece is unloaded and reloaded), and the structure piece's bounding box +
 * orientation (handled by the {@link StructurePiece} parent).
 *
 * <p>The piece intentionally generates procedurally rather than via a structure-template NBT
 * so a future colour or radius change is a single Java edit instead of an asset re-author.
 */
public final class SlimeIslandPiece extends StructurePiece {

    private static final String TAG_COLOR = "Color";
    private static final String TAG_RADIUS = "Radius";

    /** Slim padding above the disc so the bounding box covers leaves + saplings (3 blocks tall is enough for both). */
    private static final int VERTICAL_PADDING = 3;

    /** Number of leaf blocks scattered within the disc footprint per generation; tuned for "garnish, not canopy". */
    private static final int LEAF_COUNT = 6;

    private final SlimeColor color;
    private final int radius;

    public SlimeIslandPiece(SlimeColor color, int radius, BlockPos origin) {
        super(WorldStructures.SLIME_ISLAND_PIECE.get(), 0, buildBoundingBox(origin, radius));
        this.color = color;
        this.radius = radius;
    }

    public SlimeIslandPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(WorldStructures.SLIME_ISLAND_PIECE.get(), tag);
        this.color = SlimeColor.values()[tag.getInt(TAG_COLOR)];
        this.radius = tag.getInt(TAG_RADIUS);
    }

    /** Slime colour this piece will place blocks for. */
    public SlimeColor color() {
        return color;
    }

    /** Radius (block count from disc centre) sampled at structure-generation time. */
    public int radius() {
        return radius;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt(TAG_COLOR, color.ordinal());
        tag.putInt(TAG_RADIUS, radius);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        SlimePlantSet plants = WorldBlocks.PLANT_SETS.get(color);
        BlockState dirtState = plants.dirt().get().defaultBlockState();
        BlockState grassState = plants.grass().get().defaultBlockState();
        BlockState leavesState = plants.leaves().get().defaultBlockState();
        BlockState saplingState = plants.sapling().get().defaultBlockState();

        BoundingBox pieceBox = this.getBoundingBox();
        int centerX = (pieceBox.minX() + pieceBox.maxX()) / 2;
        int centerZ = (pieceBox.minZ() + pieceBox.maxZ()) / 2;
        int discY = pieceBox.minY();
        int radiusSquared = radius * radius;

        // Disc fill: dirt at y, grass at y+1. Iterate the bounding-box X/Z range and gate by
        // squared-distance against the centre so the result is a circle, not a square.
        for (int x = pieceBox.minX(); x <= pieceBox.maxX(); x++) {
            for (int z = pieceBox.minZ(); z <= pieceBox.maxZ(); z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }
                BlockPos dirtPos = new BlockPos(x, discY, z);
                BlockPos grassPos = dirtPos.above();
                if (!chunkBox.isInside(dirtPos) || !chunkBox.isInside(grassPos)) {
                    // postProcess is invoked per chunk while the piece's bounding box may span
                    // multiple chunks — skip out-of-chunk blocks so each chunk handles its own
                    // slice of the disc. The next chunk's postProcess call will fill them.
                    continue;
                }
                level.setBlock(dirtPos, dirtState, 2);
                level.setBlock(grassPos, grassState, 2);
            }
        }

        // Leaf scatter — pick random offsets within the disc and place leaves a block above the
        // grass cap. The seeded RandomSource keeps the spray deterministic per chunk.
        for (int i = 0; i < LEAF_COUNT; i++) {
            int leafDx = random.nextInt(radius * 2 + 1) - radius;
            int leafDz = random.nextInt(radius * 2 + 1) - radius;
            if (leafDx * leafDx + leafDz * leafDz > radiusSquared) {
                continue;
            }
            BlockPos leafPos = new BlockPos(centerX + leafDx, discY + 2, centerZ + leafDz);
            if (chunkBox.isInside(leafPos)) {
                level.setBlock(leafPos, leavesState, 2);
            }
        }

        // Sapling at the disc centre. AC asks for "optional sapling"; planting exactly one at
        // the centre gives a deterministic anchor point that SMTCON-55's TreeGrower can grow
        // into a colour-matched slime tree when the chunk ticks.
        BlockPos saplingPos = new BlockPos(centerX, discY + 1, centerZ);
        if (chunkBox.isInside(saplingPos)) {
            level.setBlock(saplingPos, saplingState, 2);
        }
    }

    /**
     * Build the piece's overall bounding box from the disc centre. Width covers a full
     * radius-square footprint; height is the disc + {@link #VERTICAL_PADDING} so leaves and
     * saplings fall inside the box.
     */
    private static BoundingBox buildBoundingBox(BlockPos origin, int radius) {
        return new BoundingBox(origin.getX() - radius, origin.getY(), origin.getZ() - radius, origin.getX() + radius, origin.getY() + VERTICAL_PADDING, origin.getZ() + radius);
    }
}
