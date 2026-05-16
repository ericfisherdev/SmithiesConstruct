package slimeknights.sconstruct.port1211.smeltery.multiblock;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.core.BlockPos;
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
}
