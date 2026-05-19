package slimeknights.sconstruct.smeltery.multiblock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The result of a successful {@link SmelteryStructureValidator#validate validate} pass — the
 * geometry of one assembled multiblock smeltery. Produced once when the controller assembles
 * and consumed by the controller block-entity to size its tank and melting slots (SMTCON-115's
 * downstream wiring) and by the renderer to know where the molten metal sits.
 *
 * <p>The three position sets are disjoint: {@link #floor} is the seared slab the interior rests
 * on, {@link #walls} is the seared ring enclosing it, and {@link #components} keys the subset of
 * those wall positions that are functional component blocks (controller, tanks, drain, chute).
 * Every collection is defensively copied and made immutable by the canonical constructor so a
 * stored structure cannot be mutated after validation.
 *
 * @param bounds      the interior bounding box — the open volume metal pools in, excluding the
 *                    floor slab and wall ring
 * @param floor       the seared blocks forming the floor slab directly under the interior
 * @param walls       the seared blocks (plain and component) forming the enclosing wall ring
 * @param components  the wall positions that are functional component blocks, keyed to their type
 * @param bowlVolume  the interior block count ({@code width * depth * height}) — the proxy for
 *                    the smeltery's molten-metal capacity
 */
public record SmelteryStructure(BoundingBox bounds, Set<BlockPos> floor, Set<BlockPos> walls, Map<BlockPos, ComponentType> components, int bowlVolume) {

    /**
     * Defensively copies the bounding box and collections so a validated structure is fully
     * immutable — {@link BoundingBox} is itself mutable ({@code move}/{@code encapsulate}), so a
     * direct reference would let a caller warp the structure after validation.
     */
    public SmelteryStructure {
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(floor, "floor");
        Objects.requireNonNull(walls, "walls");
        Objects.requireNonNull(components, "components");
        if (bowlVolume <= 0) {
            throw new IllegalArgumentException("bowlVolume must be positive: " + bowlVolume);
        }
        bounds = new BoundingBox(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
        floor = Set.copyOf(floor);
        walls = Set.copyOf(walls);
        components = Map.copyOf(components);
    }

    /** Six-int extents of the interior bounding box — keyed at the persisted top level. */
    private static final String TAG_BOUNDS = "Bounds";
    private static final String TAG_FLOOR = "Floor";
    private static final String TAG_WALLS = "Walls";
    private static final String TAG_COMPONENTS = "Components";
    private static final String TAG_COMPONENT_POS = "Pos";
    private static final String TAG_COMPONENT_TYPE = "Type";
    private static final String TAG_BOWL_VOLUME = "BowlVolume";

    /** Number of integers in the persisted {@link #TAG_BOUNDS} array — six box corners. */
    private static final int BOUNDS_LENGTH = 6;

    /** Number of integers in a persisted {@link BlockPos} — x, y, z. */
    private static final int BLOCK_POS_LENGTH = 3;

    /**
     * Serialises this structure into a compound suitable for stashing on the controller's NBT —
     * the bounding box, the floor and wall sets, the component-type map, and the bowl volume.
     * Round-trips with {@link #readFromTag(CompoundTag)} so a saved smeltery can re-assemble on
     * world load (SMTCON-218) without re-running the full validator.
     */
    public CompoundTag writeToTag() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray(TAG_BOUNDS, new int[] { bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ() });
        tag.putIntArray(TAG_FLOOR, flattenBlockPosSet(floor));
        tag.putIntArray(TAG_WALLS, flattenBlockPosSet(walls));
        ListTag componentList = new ListTag();
        for (Map.Entry<BlockPos, ComponentType> entry : components.entrySet()) {
            CompoundTag componentTag = new CompoundTag();
            BlockPos pos = entry.getKey();
            componentTag.putIntArray(TAG_COMPONENT_POS, new int[] { pos.getX(), pos.getY(), pos.getZ() });
            componentTag.putString(TAG_COMPONENT_TYPE, entry.getValue().name());
            componentList.add(componentTag);
        }
        tag.put(TAG_COMPONENTS, componentList);
        tag.putInt(TAG_BOWL_VOLUME, bowlVolume);
        return tag;
    }

    /**
     * Reconstructs a structure from a compound previously produced by {@link #writeToTag()}.
     * Returns {@link Optional#empty()} when the tag is malformed (missing required keys, wrong
     * lengths, or an unknown {@link ComponentType} name) so a corrupt save loads as "no structure"
     * rather than crashing the world load — the controller's next tick will fall back to a fresh
     * validation pass.
     */
    public static Optional<SmelteryStructure> readFromTag(CompoundTag tag) {
        int[] boundsArr = tag.getIntArray(TAG_BOUNDS);
        if (boundsArr.length != BOUNDS_LENGTH) {
            return Optional.empty();
        }
        BoundingBox bounds = new BoundingBox(boundsArr[0], boundsArr[1], boundsArr[2], boundsArr[3], boundsArr[4], boundsArr[5]);
        Optional<Set<BlockPos>> floor = readBlockPosSet(tag, TAG_FLOOR);
        Optional<Set<BlockPos>> walls = readBlockPosSet(tag, TAG_WALLS);
        // A missing tag or a non-int-array of the wrong type both decode as an empty {@code int[]}
        // and parse as {@code Optional.of(emptySet)} — reject that here so a malformed save cannot
        // restore a structure with no floor or walls.
        if (floor.isEmpty() || walls.isEmpty() || floor.get().isEmpty() || walls.get().isEmpty()) {
            return Optional.empty();
        }
        Map<BlockPos, ComponentType> components = new HashMap<>();
        ListTag componentList = tag.getList(TAG_COMPONENTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < componentList.size(); i++) {
            CompoundTag componentTag = componentList.getCompound(i);
            int[] posArr = componentTag.getIntArray(TAG_COMPONENT_POS);
            if (posArr.length != BLOCK_POS_LENGTH) {
                return Optional.empty();
            }
            ComponentType type;
            try {
                type = ComponentType.valueOf(componentTag.getString(TAG_COMPONENT_TYPE));
            }
            catch (IllegalArgumentException unknown) {
                // A component-type name not in the current enum (mod downgrade, code rename) is
                // not recoverable — bail out rather than silently dropping the component.
                return Optional.empty();
            }
            components.put(new BlockPos(posArr[0], posArr[1], posArr[2]), type);
        }
        int bowlVolume = tag.getInt(TAG_BOWL_VOLUME);
        if (bowlVolume <= 0 || components.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SmelteryStructure(bounds, floor.get(), walls.get(), components, bowlVolume));
    }

    /** Flattens a {@link BlockPos} set into a {@code [x, y, z, x, y, z, ...]} int array for NBT storage. */
    private static int[] flattenBlockPosSet(Set<BlockPos> positions) {
        int[] flat = new int[positions.size() * BLOCK_POS_LENGTH];
        int offset = 0;
        for (BlockPos pos : positions) {
            flat[offset] = pos.getX();
            flat[offset + 1] = pos.getY();
            flat[offset + 2] = pos.getZ();
            offset += BLOCK_POS_LENGTH;
        }
        return flat;
    }

    /**
     * Reverses {@link #writeBlockPosSet} — reads {@code x, y, z} triples until the array is
     * exhausted. A length that is not a multiple of three is malformed (a corrupt save or a
     * partial write); the empty {@link Optional} propagates to {@link #readFromTag}, which then
     * rejects the whole structure rather than silently dropping the trailing positions.
     */
    private static Optional<Set<BlockPos>> readBlockPosSet(CompoundTag tag, String key) {
        int[] flat = tag.getIntArray(key);
        if (flat.length % BLOCK_POS_LENGTH != 0) {
            return Optional.empty();
        }
        int count = flat.length / BLOCK_POS_LENGTH;
        Set<BlockPos> positions = new HashSet<>(count);
        for (int i = 0; i < flat.length; i += BLOCK_POS_LENGTH) {
            positions.add(new BlockPos(flat[i], flat[i + 1], flat[i + 2]));
        }
        return Optional.of(positions);
    }
}
