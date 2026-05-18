package slimeknights.sconstruct.world;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.registries.DeferredBlock;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.world.block.SlimeColor;

/**
 * Pinned-behaviour tests for the Phase-3 slime-log additions to {@link WorldBlocks}. Verifies
 * the four-per-variant roster ({@link WorldBlocks#SLIME_LOGS},
 * {@link WorldBlocks#STRIPPED_SLIME_LOGS}), the flat {@link WorldBlocks#ALL_LOGS} iteration
 * order (4 normal followed by 4 stripped), the registry-path convention for both variants,
 * and the {@link RotatedPillarBlock} type of every entry.
 */
class WorldBlocksLogsTest {

    @Test
    void fourLogsAndFourStrippedVariantsAreRegisteredOnePerSlimeColor() {
        assertEquals(SlimeColor.values().length, WorldBlocks.SLIME_LOGS.size());
        assertEquals(SlimeColor.values().length, WorldBlocks.STRIPPED_SLIME_LOGS.size());
        for (SlimeColor color : SlimeColor.values()) {
            assertNotNull(WorldBlocks.SLIME_LOGS.get(color), "no normal log for " + color);
            assertNotNull(WorldBlocks.STRIPPED_SLIME_LOGS.get(color), "no stripped log for " + color);
        }
    }

    @Test
    void allLogsFlatViewListsNormalThenStrippedInColorOrder() {
        // ALL_LOGS is the iteration surface for blockstate / item-model / loot / tag
        // generators. Pinning the order here means a future refactor that, say, interleaves
        // the variants would trip the test instead of silently corrupting a downstream
        // provider that assumes the flat shape.
        List<String> expected = new ArrayList<>();
        WorldBlocks.SLIME_LOGS.values().forEach(holder -> expected.add(holder.getId().toString()));
        WorldBlocks.STRIPPED_SLIME_LOGS.values().forEach(holder -> expected.add(holder.getId().toString()));
        List<String> actual = WorldBlocks.ALL_LOGS.stream().map(holder -> holder.getId().toString()).toList();
        assertEquals(8, actual.size());
        assertEquals(expected, actual);
    }

    @Test
    void normalLogRegistryPathsFollowSlimeColorLogConvention() {
        assertAll(WorldBlocks.SLIME_LOGS.entrySet().stream().map(entry -> () -> {
            DeferredBlock<RotatedPillarBlock> holder = entry.getValue();
            assertEquals(SConstruct.MOD_ID, holder.getId().getNamespace());
            assertEquals("slime_" + entry.getKey().id() + "_log", holder.getId().getPath());
        }));
    }

    @Test
    void strippedLogRegistryPathsPrefixWithStripped() {
        assertAll(WorldBlocks.STRIPPED_SLIME_LOGS.entrySet().stream().map(entry -> () -> {
            DeferredBlock<RotatedPillarBlock> holder = entry.getValue();
            assertEquals(SConstruct.MOD_ID, holder.getId().getNamespace());
            assertEquals("stripped_slime_" + entry.getKey().id() + "_log", holder.getId().getPath());
        }));
    }

    @Test
    void everyLogResolvesToARotatedPillarBlock() {
        // RotatedPillarBlock carries the AXIS state property that vanilla cube_column blockstates
        // expect — a regression to plain Block would silently break the axis-aware blockstate
        // generator's logBlock(...) call which casts to RotatedPillarBlock.
        for (DeferredBlock<RotatedPillarBlock> holder : WorldBlocks.ALL_LOGS) {
            assertTrue(holder.get() instanceof RotatedPillarBlock, holder.getId() + " must be a RotatedPillarBlock");
        }
    }
}
