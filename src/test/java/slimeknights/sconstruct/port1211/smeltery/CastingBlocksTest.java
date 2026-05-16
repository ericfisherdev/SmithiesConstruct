package slimeknights.sconstruct.port1211.smeltery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.smeltery.block.CastingBasinBlock;
import slimeknights.sconstruct.port1211.smeltery.block.CastingTableBlock;

/**
 * Pinned-roster tests for {@link CastingBlocks}. Asserts the two casting blocks register under
 * the expected ids and concrete classes, namespaced under the mod id, and that both
 * block-entity types resolve.
 */
class CastingBlocksTest {

    @Test
    void registersBothCastingBlocks() {
        assertEquals(2, CastingBlocks.ALL.size(), "the casting roster is two blocks");
        assertEquals("casting_table", CastingBlocks.CASTING_TABLE.getId().getPath());
        assertEquals("casting_basin", CastingBlocks.CASTING_BASIN.getId().getPath());
        assertEquals(SConstruct.MOD_ID, CastingBlocks.CASTING_TABLE.getId().getNamespace());
        assertEquals(SConstruct.MOD_ID, CastingBlocks.CASTING_BASIN.getId().getNamespace());
    }

    @Test
    void blocksResolveToTheirConcreteClasses() {
        assertInstanceOf(CastingTableBlock.class, CastingBlocks.CASTING_TABLE.get());
        assertInstanceOf(CastingBasinBlock.class, CastingBlocks.CASTING_BASIN.get());
    }

    @Test
    void bothBlockEntityTypesResolve() {
        assertNotNull(CastingBlocks.CASTING_TABLE_BE.get(), "casting_table BE type resolves");
        assertNotNull(CastingBlocks.CASTING_BASIN_BE.get(), "casting_basin BE type resolves");
    }
}
