package slimeknights.sconstruct.port1211.world.structure;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import slimeknights.sconstruct.port1211.world.WorldStructures;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * Slime-island worldgen structure. Each registered datapack instance pins a {@link SlimeColor}
 * (so {@code sconstruct:slime_island_blue} produces a blue-themed island, {@code _magma}
 * produces a magma island, etc.) and a sampled radius {@link IntProvider} in the
 * {@link #RADIUS_MIN}-{@link #RADIUS_MAX} range. The single {@link SlimeIslandPiece} this
 * structure adds carries the colour + sampled radius forward into block placement.
 *
 * <p>{@link Structure#onTopOfChunkCenter} anchors the island to the world's
 * {@link Heightmap.Types#WORLD_SURFACE_WG} so it sits at the natural terrain height of the
 * chunk's centre — overworld islands settle on top of normal terrain. The piece's bounding box
 * is sized to the maximum radius so chunk-by-chunk block placement can extend across chunk
 * boundaries without being trimmed.
 *
 * <p>The datapack JSON wiring (one entry per colour) lands in SMTCON-59; this ticket only
 * provides the {@link Structure} subclass, the {@link StructureType} {@link MapCodec}, and the
 * registration handles in {@link WorldStructures} so {@code /place structure
 * sconstruct:slime_island_<colour>} works on a manually-prepared datapack today.
 */
public final class SlimeIslandStructure extends Structure {

    /** Smallest sampled radius the codec accepts; matches the ticket's "8" lower bound. */
    public static final int RADIUS_MIN = 8;

    /** Largest sampled radius the codec accepts; matches the ticket's "24" upper bound. */
    public static final int RADIUS_MAX = 24;

    public static final MapCodec<SlimeIslandStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(settingsCodec(instance), SlimeColor.CODEC.fieldOf("color").forGetter(s -> s.color), IntProvider.codec(RADIUS_MIN, RADIUS_MAX).fieldOf("radius").forGetter(s -> s.radius))
            .apply(instance, SlimeIslandStructure::new));

    private final SlimeColor color;
    private final IntProvider radius;

    public SlimeIslandStructure(StructureSettings settings, SlimeColor color, IntProvider radius) {
        super(settings);
        this.color = color;
        this.radius = radius;
    }

    /** Slime colour this island generates with. Read by the piece during {@code postProcess}. */
    public SlimeColor color() {
        return color;
    }

    /** Radius provider; sampled once per generation against the chunk's random source. */
    public IntProvider radius() {
        return radius;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // Anchor to the world-surface heightmap (no leaves filter) so islands hover on the
        // visible surface, not at the void-floor or buried under non-leaf foliage. The lambda
        // sample is per-structure-instance; radius() is sampled here so the piece can pin the
        // value for save/load round-tripping.
        RandomSource random = context.random();
        int sampledRadius = radius.sample(random);
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            BlockPos chunkCenter = new BlockPos(context.chunkPos().getMiddleBlockX(), 0, context.chunkPos().getMiddleBlockZ());
            builder.addPiece(new SlimeIslandPiece(color, sampledRadius, chunkCenter));
        });
    }

    @Override
    public StructureType<?> type() {
        return WorldStructures.SLIME_ISLAND_TYPE.get();
    }
}
