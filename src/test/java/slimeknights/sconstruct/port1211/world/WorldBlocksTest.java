package slimeknights.sconstruct.port1211.world;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.neoforge.registries.DeferredBlock;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;
import slimeknights.sconstruct.port1211.world.block.TinkerSlimeBlock;

/**
 * Pinned-behaviour tests for {@link WorldBlocks}. Verifies the four-block roster, registry-path
 * convention ({@code slime_<color>_block}), namespace contract, ordered iteration over
 * {@link WorldBlocks#ALL}, the {@link WorldBlocks#SLIME_BLOCKS} keyed-by-{@link SlimeColor}
 * shape, the {@link WorldBlocks#acceptBlockItems} visitor used by the creative-tab listener,
 * and {@link WorldBlocks#init()} idempotency.
 */
class WorldBlocksTest {

    @Test
    void fourSlimeBlocksAreRegisteredInDeclarationOrder() {
        assertEquals(4, WorldBlocks.ALL.size());
        assertEquals(List.of(WorldBlocks.SLIMEBLUE, WorldBlocks.SLIMEPURPLE, WorldBlocks.SLIMEMAGMA, WorldBlocks.SLIMEBLOOD), WorldBlocks.ALL);
    }

    @Test
    void slimeBlocksMapKeysCoverEverySlimeColor() {
        // SLIME_BLOCKS must hold an entry for every SlimeColor — providers iterate the enum's
        // values() and pair them with the registered block; a missing key would crash datagen
        // and an extra key would mean a stale colour kept its block past its enum removal.
        assertEquals(SlimeColor.values().length, WorldBlocks.SLIME_BLOCKS.size());
        for (SlimeColor color : SlimeColor.values()) {
            DeferredBlock<TinkerSlimeBlock> holder = WorldBlocks.SLIME_BLOCKS.get(color);
            assertNotNull(holder, "no entry for " + color);
        }
    }

    @Test
    void registryPathsFollowTheSlimeColorBlockConvention() {
        // Path must be "slime_<color>_block" — this disambiguates from the WorldFluids
        // LiquidBlock at registry path "slime_<color>" (different registry, but the matching
        // BlockItem would clash on the items registry without the "_block" suffix).
        for (SlimeColor color : SlimeColor.values()) {
            DeferredBlock<TinkerSlimeBlock> holder = WorldBlocks.SLIME_BLOCKS.get(color);
            assertEquals(SConstruct.MOD_ID, holder.getId().getNamespace(), "namespace for " + color);
            assertEquals("slime_" + color.id() + "_block", holder.getId().getPath(), "path for " + color);
        }
    }

    @Test
    void everySlimeBlockCarriesTinkerSlimeBlockColorMatchingItsKey() {
        // The DeferredBlock factory wires the SlimeColor key into the TinkerSlimeBlock ctor;
        // verify the round-trip so a future refactor that decouples key from ctor can't
        // silently swap the per-colour fallOn dispatch.
        assertAll(WorldBlocks.SLIME_BLOCKS.entrySet().stream().map(entry -> () -> assertSlimeBlockColor(entry.getKey(), entry.getValue())));
    }

    private static void assertSlimeBlockColor(SlimeColor expected, DeferredBlock<TinkerSlimeBlock> holder) {
        TinkerSlimeBlock block = holder.get();
        assertEquals(expected, block.color(), "block at " + holder.getId() + " carries the wrong colour");
    }

    @Test
    void acceptBlockItemsVisitsEveryBlockInOrder() {
        // The world pulse's creative-tab listener calls acceptBlockItems to populate the tab.
        // Build the expected sequence from WorldBlocks.ALL and compare to the visited order so
        // a shuffled implementation fails here instead of corrupting the creative-tab column
        // order.
        List<String> seen = new ArrayList<>();
        WorldBlocks.acceptBlockItems(item -> seen.add(item.asItem().getDescriptionId()));
        List<String> expected = WorldBlocks.ALL.stream().map(set -> set.get().asItem().getDescriptionId()).toList();
        assertEquals(expected, seen, "acceptBlockItems order/content must match WorldBlocks.ALL");
        seen.forEach(id -> assertTrue(id.startsWith("block.sconstruct.slime_") && id.endsWith("_block"), "expected 'block.sconstruct.slime_<color>_block' id, got " + id));
    }

    @Test
    void initIsIdempotent() {
        // Touching the class twice must not duplicate registrations. The static field
        // initialiser runs once per ClassLoader; the second init() call is a no-op.
        int sizeBefore = WorldBlocks.ALL.size();
        WorldBlocks.init();
        WorldBlocks.init();
        assertEquals(sizeBefore, WorldBlocks.ALL.size());
    }
}
