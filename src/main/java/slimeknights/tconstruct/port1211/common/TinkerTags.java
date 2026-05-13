package slimeknights.tconstruct.port1211.common;

import static slimeknights.tconstruct.port1211.lib.util.Util.rl;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * Central declaration of every {@link TagKey} the mod relies on at registration time.
 * Tag JSON content is produced by per-pulse data providers in later phases; this class only
 * pins the <em>keys</em> so registration code can reference a single, typo-proof constant
 * instead of constructing {@code TagKey.create(...)} ad hoc at every call site.
 *
 * <p>Tags are grouped by their target registry into nested holder classes ({@link Blocks},
 * {@link Items}, {@link Fluids}, {@link Entities}, {@link Biomes}) so a misuse like passing
 * a block tag to an item registrar fails at compile time, not at data-load time.
 *
 * <p>Adding a tag: pick the matching nested class, declare
 * {@code public static final TagKey<T> NAME = TagKey.create(Registries.X, rl("path"));},
 * and add a one-line comment describing the tag's intended purpose — readers should not have
 * to grep call sites to learn what membership in a tag means.
 */
public final class TinkerTags {

    private TinkerTags() {
    }

    /** Block-registry tags. */
    public static final class Blocks {

        private Blocks() {
        }

        /** Blocks that are valid as smeltery structural components (controllers, drains, IO blocks). */
        public static final TagKey<Block> SMELTERY_COMPONENT = TagKey.create(Registries.BLOCK, rl("smeltery/component"));

        /** Blocks accepted as the bottom layer of a smeltery multiblock (seared bricks and equivalents). */
        public static final TagKey<Block> SMELTERY_FLOOR = TagKey.create(Registries.BLOCK, rl("smeltery/floor"));

        /** Blocks accepted as the vertical wall layers of a smeltery multiblock. */
        public static final TagKey<Block> SMELTERY_WALL = TagKey.create(Registries.BLOCK, rl("smeltery/wall"));

        /** All slime-grass variants (green, blue, magma, etc.) for shared grass-spread + drop behaviour. */
        public static final TagKey<Block> SLIMEGRASS = TagKey.create(Registries.BLOCK, rl("slimegrass"));

        /** All slime-tree log variants for shared stripping, axe-mining, and recipe input behaviour. */
        public static final TagKey<Block> SLIMELOGS = TagKey.create(Registries.BLOCK, rl("slimelogs"));
    }

    /** Item-registry tags. */
    public static final class Items {

        private Items() {
        }

        /** All slimeball variants (green, blue, magma, vanilla, etc.) usable as a slimeball ingredient. */
        public static final TagKey<Item> SLIMEBALLS = TagKey.create(Registries.ITEM, rl("slimeballs"));

        /** Items recognised as tool parts (heads, bindings, tool rods, etc.) for tool-building UIs and JEI. */
        public static final TagKey<Item> TOOLPARTS = TagKey.create(Registries.ITEM, rl("toolparts"));
    }

    /** Fluid-registry tags. */
    public static final class Fluids {

        private Fluids() {
        }

        /** Fluids that can be burned in a smeltery as fuel (lava and any addon-defined equivalents). */
        public static final TagKey<Fluid> SMELTERY_FUEL = TagKey.create(Registries.FLUID, rl("smelteryfuel"));
    }

    /** Entity-registry tags. Empty in Phase 1; entries arrive with the entity-owning pulses. */
    public static final class Entities {

        private Entities() {
        }
    }

    /** Biome-registry tags. */
    public static final class Biomes {

        private Biomes() {
        }

        /** Biomes flagged as slime islands — gates slime-island world-gen, mob spawns, and ambience. */
        public static final TagKey<Biome> SLIME_ISLANDS = TagKey.create(Registries.BIOME, rl("slime_islands"));
    }
}
