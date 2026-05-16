package slimeknights.sconstruct.port1211.smeltery;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;

/**
 * Pinned-roster tests for {@link SearedBlocks}. Asserts the 16-block roster by id so a future
 * variant change surfaces loudly, that every block is namespaced under the mod id, and that the
 * stair / slab variants resolve to the right vanilla block subclasses.
 */
class SearedBlocksTest {

    @Test
    void registersAllSixteenSearedBlocks() {
        assertEquals(16, SearedBlocks.ALL.size(), "the seared roster is 16 blocks");
        Set<String> ids = SearedBlocks.ALL.stream().map(b -> b.getId().getPath()).collect(Collectors.toSet());
        assertEquals(16, ids.size(), "seared block ids must be unique");
        for (String id : new String[] { "seared_stone", "seared_cobble", "seared_paver", "seared_brick", "seared_brick_chiseled", "seared_brick_squared", "seared_brick_creeper", "seared_brick_road",
                "seared_brick_fancy", "seared_brick_triangle", "seared_glass", "seared_window", "seared_brick_stairs", "seared_brick_slab", "seared_paver_stairs", "seared_paver_slab" }) {
            assertTrue(ids.contains(id), "missing seared block id: " + id);
        }
    }

    @Test
    void everyBlockIsNamespacedUnderTheModId() {
        assertAll(SearedBlocks.ALL.stream().map(block -> () -> assertEquals(SConstruct.MOD_ID, block.getId().getNamespace(), "namespace of " + block.getId())));
    }

    @Test
    void stairAndSlabVariantsResolveToTheVanillaSubclasses() {
        assertInstanceOf(StairBlock.class, SearedBlocks.SEARED_BRICK_STAIRS.get(), "seared_brick_stairs is a StairBlock");
        assertInstanceOf(StairBlock.class, SearedBlocks.SEARED_PAVER_STAIRS.get(), "seared_paver_stairs is a StairBlock");
        assertInstanceOf(SlabBlock.class, SearedBlocks.SEARED_BRICK_SLAB.get(), "seared_brick_slab is a SlabBlock");
        assertInstanceOf(SlabBlock.class, SearedBlocks.SEARED_PAVER_SLAB.get(), "seared_paver_slab is a SlabBlock");
    }

    @Test
    void initIsIdempotent() {
        int sizeBefore = SearedBlocks.ALL.size();
        SearedBlocks.init();
        SearedBlocks.init();
        assertEquals(sizeBefore, SearedBlocks.ALL.size());
    }
}
