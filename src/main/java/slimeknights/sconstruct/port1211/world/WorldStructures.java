package slimeknights.sconstruct.port1211.world;

import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.world.structure.SlimeIslandPiece;
import slimeknights.sconstruct.port1211.world.structure.SlimeIslandStructure;

/**
 * Phase-3 worldgen structure registrations.
 *
 * <ul>
 *   <li>{@link #SLIME_ISLAND_TYPE} — the {@link StructureType} bound to
 *       {@link SlimeIslandStructure#CODEC}; one type covers every colour because the
 *       per-colour difference is encoded as a field on each datapack JSON instance.</li>
 *   <li>{@link #SLIME_ISLAND_PIECE} — the {@link StructurePieceType} that lets the chunk
 *       reload path re-instantiate a {@link SlimeIslandPiece} from its NBT after a save/load
 *       cycle (the {@code context, tag} ctor is the deserialiser).</li>
 * </ul>
 *
 * <p>{@link #init()} forces this class to load so the static initialisers populate
 * {@link TinkerRegistries#STRUCTURE_TYPES} and {@link TinkerRegistries#STRUCTURE_PIECE_TYPES}
 * before the matching registry events fire. Same pattern as {@link WorldFluids#init()} and
 * {@link WorldBlocks#init()}.
 */
public final class WorldStructures {

    public static final DeferredHolder<StructureType<?>, StructureType<SlimeIslandStructure>> SLIME_ISLAND_TYPE = TinkerRegistries.STRUCTURE_TYPES.register("slime_island",
            () -> () -> SlimeIslandStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SLIME_ISLAND_PIECE = TinkerRegistries.STRUCTURE_PIECE_TYPES.register("slime_island", () -> SlimeIslandPiece::new);

    private WorldStructures() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. Same pattern as {@link WorldBlocks#init()}.
    }
}
