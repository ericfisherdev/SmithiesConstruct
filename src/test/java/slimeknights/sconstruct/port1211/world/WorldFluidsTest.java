package slimeknights.sconstruct.port1211.world;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;

/**
 * Pinned-behaviour tests for {@link WorldFluids}. Verifies the four-fluid roster, the namespace
 * contract every registered holder must satisfy, the bucket-acceptor visitor used by the world
 * pulse's creative-tab listener, and the {@link WorldFluids#TINTS} mapping the client extensions
 * registrar reads from.
 */
class WorldFluidsTest {

    @Test
    void fourSlimeFluidsAreRegisteredInDeclarationOrder() {
        // The world pulse, lang provider, client extensions, and creative tab all iterate
        // WorldFluids.ALL. Drifting the size or order silently corrupts every consumer.
        assertEquals(4, WorldFluids.ALL.size());
        assertEquals(List.of(WorldFluids.SLIMEBLUE, WorldFluids.SLIMEPURPLE, WorldFluids.SLIMEMAGMA, WorldFluids.SLIMEBLOOD), WorldFluids.ALL);
    }

    @Test
    void everyComponentIsNamespacedUnderTheModId() {
        // Every DeferredHolder must resolve to the mod's namespace — a drifted namespace would
        // make F3+I show a different mod's id at runtime and break datagen output paths.
        assertAll(WorldFluids.ALL.stream().map(set -> () -> {
            assertEquals(SConstruct.MOD_ID, set.type().getId().getNamespace(), "fluid type namespace");
            assertEquals(SConstruct.MOD_ID, set.source().getId().getNamespace(), "source fluid namespace");
            assertEquals(SConstruct.MOD_ID, set.flowing().getId().getNamespace(), "flowing fluid namespace");
            assertEquals(SConstruct.MOD_ID, set.block().getId().getNamespace(), "liquid block namespace");
            assertEquals(SConstruct.MOD_ID, set.bucket().getId().getNamespace(), "bucket namespace");
        }));
    }

    @Test
    void registryPathsFollowTheSlimeColorConvention() {
        // FluidType, source, and LiquidBlock share the registry path "slime_<color>". Flowing
        // uses "flowing_slime_<color>" and the bucket uses "slime_<color>_bucket". Pinning the
        // shapes here means a future rename forces an explicit test update rather than
        // silently desyncing data providers that hard-code the same shapes.
        for (SlimeFluidSet set : WorldFluids.ALL) {
            String typePath = set.type().getId().getPath();
            assertTrue(typePath.startsWith("slime_"), "type path should start with 'slime_' but was " + typePath);
            assertEquals(typePath, set.source().getId().getPath(), "type and source share path");
            assertEquals("flowing_" + typePath, set.flowing().getId().getPath(), "flowing prefixes 'flowing_'");
            assertEquals(typePath, set.block().getId().getPath(), "block shares path with source");
            assertEquals(typePath + "_bucket", set.bucket().getId().getPath(), "bucket appends '_bucket'");
        }
    }

    @Test
    void everyFluidIsBoundToATint() {
        // Tints map keys must cover every set in ALL one-to-one. Missing entries would skip
        // tinting for a fluid; extra entries would leak memory and signal a stale registration.
        assertEquals(WorldFluids.ALL.size(), WorldFluids.TINTS.size());
        for (SlimeFluidSet set : WorldFluids.ALL) {
            Integer tint = WorldFluids.TINTS.get(set);
            assertNotNull(tint, "tint missing for " + set.source().getId());
            // Tints carry full opacity in the alpha byte so the bucket sprite renders solid.
            assertEquals(0xFF, (tint.intValue() >>> 24) & 0xFF, "alpha must be 0xFF");
        }
    }

    @Test
    void tintsMatchTheNamedConstants() {
        assertAll(() -> assertEquals(WorldFluids.SLIMEBLUE_TINT, WorldFluids.TINTS.get(WorldFluids.SLIMEBLUE).intValue()),
                () -> assertEquals(WorldFluids.SLIMEPURPLE_TINT, WorldFluids.TINTS.get(WorldFluids.SLIMEPURPLE).intValue()),
                () -> assertEquals(WorldFluids.SLIMEMAGMA_TINT, WorldFluids.TINTS.get(WorldFluids.SLIMEMAGMA).intValue()),
                () -> assertEquals(WorldFluids.SLIMEBLOOD_TINT, WorldFluids.TINTS.get(WorldFluids.SLIMEBLOOD).intValue()));
    }

    @Test
    void acceptBucketsVisitsEverySetInOrder() {
        // The world pulse's creative-tab listener calls acceptBuckets to populate the tab. The
        // visit order must match WorldFluids.ALL so the buckets land in the same column the
        // player expects from the lang display order — assert the exact expected sequence,
        // not just count + suffix, so a shuffled implementation doesn't slip through.
        List<String> seen = new ArrayList<>();
        WorldFluids.acceptBuckets(item -> seen.add(item.asItem().getDescriptionId()));
        List<String> expected = WorldFluids.ALL.stream().map(set -> set.bucket().get().getDescriptionId()).toList();
        assertEquals(expected, seen, "acceptBuckets order/content must match WorldFluids.ALL");
        seen.forEach(id -> assertTrue(id.endsWith("_bucket"), "expected '*_bucket' description id, got " + id));
    }

    @Test
    void initIsIdempotent() {
        // Touching the class twice must not duplicate registrations. The static field initialiser
        // runs once per ClassLoader; the second init() call is a no-op. Asserting via size avoids
        // probing JVM-internal state.
        int sizeBefore = WorldFluids.ALL.size();
        WorldFluids.init();
        WorldFluids.init();
        assertEquals(sizeBefore, WorldFluids.ALL.size());
    }
}
