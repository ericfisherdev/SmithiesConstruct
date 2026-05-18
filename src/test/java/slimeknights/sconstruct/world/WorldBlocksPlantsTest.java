package slimeknights.sconstruct.world;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.world.block.SlimeColor;
import slimeknights.sconstruct.world.block.SlimePlantSet;

/**
 * Pinned-behaviour tests for the Phase-3 plant-set additions to {@link WorldBlocks}. Verifies
 * the four-set roster ({@link WorldBlocks#PLANT_SETS}), the flattened iteration
 * ({@link WorldBlocks#ALL_PLANTS}) shape, the registry-path convention each plant block
 * follows ({@code slime_<color>_{dirt,grass,leaves,sapling}}), the
 * {@link WorldBlocks#acceptPlantItems} visitor used by the creative-tab listener, and the
 * matching-colour-dirt wiring on each grass block.
 */
class WorldBlocksPlantsTest {

    @Test
    void fourPlantSetsAreRegisteredOnePerSlimeColor() {
        assertEquals(SlimeColor.values().length, WorldBlocks.PLANT_SETS.size());
        for (SlimeColor color : SlimeColor.values()) {
            SlimePlantSet set = WorldBlocks.PLANT_SETS.get(color);
            assertNotNull(set, "no plant set for " + color);
        }
    }

    @Test
    void allPlantsHasSixteenEntriesInDeclarationOrder() {
        // 4 colours × 4 plant types per set = 16. Order: (blue dirt, blue grass, blue leaves,
        // blue sapling, purple dirt, …) — drifting the order silently swaps lang or loot or
        // tag entries across blocks. Build the expected list from PLANT_SETS the same way
        // ALL_PLANTS is built and compare element-wise.
        List<String> expected = new ArrayList<>();
        for (SlimePlantSet set : WorldBlocks.PLANT_SETS.values()) {
            set.all().forEach(holder -> expected.add(holder.getId().toString()));
        }
        List<String> actual = WorldBlocks.ALL_PLANTS.stream().map(holder -> holder.getId().toString()).toList();
        assertEquals(16, actual.size());
        assertEquals(expected, actual);
    }

    @Test
    void plantBlockRegistryPathsFollowTheSlimeColorTypeConvention() {
        // Path shape "slime_<color>_<type>" disambiguates from the bouncy slime blocks
        // ("slime_<color>_block") and from the slime LiquidBlocks ("slime_<color>"). Pinning
        // the shape here means a future rename forces a deliberate test update rather than
        // silently desyncing every data provider that hard-codes these paths.
        assertAll(WorldBlocks.PLANT_SETS.entrySet().stream().map(entry -> () -> {
            SlimeColor color = entry.getKey();
            SlimePlantSet set = entry.getValue();
            assertEquals(SConstruct.MOD_ID, set.dirt().getId().getNamespace());
            assertEquals("slime_" + color.id() + "_dirt", set.dirt().getId().getPath());
            assertEquals("slime_" + color.id() + "_grass", set.grass().getId().getPath());
            assertEquals("slime_" + color.id() + "_leaves", set.leaves().getId().getPath());
            assertEquals("slime_" + color.id() + "_sapling", set.sapling().getId().getPath());
        }));
    }

    @Test
    void grassPairsWithItsMatchingColorDirt() {
        // The spread target on every grass block must point at the dirt block of the same
        // colour — a cross-wired pair would spread blue grass onto purple dirt at the next
        // random tick, corrupting the visual identity of every island.
        assertAll(WorldBlocks.PLANT_SETS.entrySet().stream().map(entry -> () -> {
            SlimePlantSet set = entry.getValue();
            assertSame(set.dirt().get(), set.grass().get().matchingDirt(), "grass spread target for " + entry.getKey());
        }));
    }

    @Test
    void acceptPlantItemsVisitsAllSixteenBlocksInDeclarationOrder() {
        List<String> seen = new ArrayList<>();
        WorldBlocks.acceptPlantItems(item -> seen.add(item.asItem().getDescriptionId()));
        List<String> expected = WorldBlocks.ALL_PLANTS.stream().map(holder -> holder.get().asItem().getDescriptionId()).toList();
        assertEquals(16, seen.size());
        assertEquals(expected, seen, "acceptPlantItems order/content must match ALL_PLANTS");
        seen.forEach(id -> assertTrue(id.startsWith("block.sconstruct.slime_"), "expected 'block.sconstruct.slime_*' description id, got " + id));
    }
}
