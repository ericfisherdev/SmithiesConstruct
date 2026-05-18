package slimeknights.sconstruct.smeltery;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.SConstruct;

/**
 * Pinned-behaviour tests for {@link SmelteryFluids}. Verifies the 20-fluid roster, the
 * {@code molten_<metal>} registry-path contract every component must satisfy, the
 * {@link SmelteryFluids#MOLTEN} lookup map, and the {@link SmelteryFluids#TINTS} mapping the
 * SMTCON-110 client extensions registrar reads from. Mirrors {@code WorldFluidsTest}.
 */
class SmelteryFluidsTest {

    @Test
    void everyMoltenMetalIsRegisteredAsAFluidSet() {
        assertEquals(MoltenMetals.ALL.size(), SmelteryFluids.ALL.size(), "one fluid set per molten metal");
        assertEquals(20, SmelteryFluids.ALL.size(), "the roster is 20 molten metals");
        assertEquals(20, SmelteryFluids.MOLTEN.size(), "MOLTEN must key every metal");
        for (MoltenMetal metal : MoltenMetals.ALL) {
            assertNotNull(SmelteryFluids.MOLTEN.get(metal), "no fluid set registered for " + metal.id());
        }
    }

    @Test
    void getResolvesTheSameSetAsTheMoltenMap() {
        for (MoltenMetal metal : MoltenMetals.ALL) {
            assertSame(SmelteryFluids.MOLTEN.get(metal), SmelteryFluids.get(metal), "get() must return the MOLTEN entry for " + metal.id());
        }
    }

    @Test
    void everyComponentIsNamespacedUnderTheModId() {
        assertAll(SmelteryFluids.ALL.stream().map(set -> () -> {
            assertEquals(SConstruct.MOD_ID, set.type().getId().getNamespace(), "fluid type namespace");
            assertEquals(SConstruct.MOD_ID, set.source().getId().getNamespace(), "source fluid namespace");
            assertEquals(SConstruct.MOD_ID, set.flowing().getId().getNamespace(), "flowing fluid namespace");
            assertEquals(SConstruct.MOD_ID, set.block().getId().getNamespace(), "liquid block namespace");
            assertEquals(SConstruct.MOD_ID, set.bucket().getId().getNamespace(), "bucket namespace");
        }));
    }

    @Test
    void registryPathsFollowTheMoltenConvention() {
        // FluidType, source, and LiquidBlock share the path "molten_<metal>"; flowing prefixes
        // "flowing_", the bucket appends "_bucket". Pinning the shapes forces a future rename to
        // update this test rather than silently desyncing the recipe / lang data providers.
        for (MoltenMetal metal : MoltenMetals.ALL) {
            MoltenFluidSet set = SmelteryFluids.get(metal);
            String typePath = set.type().getId().getPath();
            assertEquals("molten_" + metal.id(), typePath, "type path is molten_<metal>");
            assertEquals(typePath, set.source().getId().getPath(), "type and source share path");
            assertEquals("flowing_" + typePath, set.flowing().getId().getPath(), "flowing prefixes 'flowing_'");
            assertEquals(typePath, set.block().getId().getPath(), "block shares path with source");
            assertEquals(typePath + "_bucket", set.bucket().getId().getPath(), "bucket appends '_bucket'");
        }
    }

    @Test
    void everyFluidIsBoundToAnOpaqueTintFromItsDriver() {
        assertEquals(SmelteryFluids.ALL.size(), SmelteryFluids.TINTS.size(), "TINTS must cover every set one-to-one");
        for (MoltenMetal metal : MoltenMetals.ALL) {
            Integer tint = SmelteryFluids.TINTS.get(SmelteryFluids.get(metal));
            assertNotNull(tint, "tint missing for molten_" + metal.id());
            assertEquals(0xFF, (tint.intValue() >>> 24) & 0xFF, "alpha must be 0xFF (opaque)");
            assertEquals(metal.tint(), tint.intValue() & 0xFFFFFF, "RGB must match the MoltenMetal driver tint");
        }
    }

    @Test
    void initIsIdempotent() {
        int sizeBefore = SmelteryFluids.ALL.size();
        SmelteryFluids.init();
        SmelteryFluids.init();
        assertEquals(sizeBefore, SmelteryFluids.ALL.size());
        assertTrue(sizeBefore > 0, "init must have registered the roster");
    }
}
