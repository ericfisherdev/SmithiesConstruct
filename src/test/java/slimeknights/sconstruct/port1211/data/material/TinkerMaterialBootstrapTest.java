package slimeknights.sconstruct.port1211.data.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.tools.PartType;
import slimeknights.sconstruct.port1211.tools.material.Material;

/**
 * Pinned-behaviour tests for {@link TinkerMaterialBootstrap}. Captures the entries the bootstrap
 * registers into a {@link BootstrapContext} mock, then asserts the roster surface SMTCON-71
 * promises: 15 specific materials at the expected ids with the AC-pinned tier ladder, the
 * canonical five stat-slot keys, and a repair tag on every smeltery-tier metal.
 */
class TinkerMaterialBootstrapTest {

    /** Canonical roster — id → tier — pinned by SMTCON-71 AC. Any drift breaks downstream
     *  tool-default math; the test compares this map against the bootstrap output as a set. */
    private static final Map<ResourceLocation, Integer> EXPECTED_TIERS = Map.ofEntries(Map.entry(rl("wood"), 0), Map.entry(rl("stone"), 1), Map.entry(rl("iron"), 2), Map.entry(rl("gold"), 0),
            Map.entry(rl("flint"), 1), Map.entry(rl("bone"), 1), Map.entry(rl("paper"), 1), Map.entry(rl("slime"), 1), Map.entry(rl("blueslime"), 1), Map.entry(rl("copper"), 2),
            Map.entry(rl("silver"), 3), Map.entry(rl("steel"), 3), Map.entry(rl("cobalt"), 4), Map.entry(rl("ardite"), 4), Map.entry(rl("manyullyn"), 5));

    /** Canonical five stat-slot keys every material must populate so tool builds resolve. */
    private static final Set<PartType> EXPECTED_STAT_SLOTS = Set.of(PartType.PICKHEAD, PartType.HANDLE, PartType.BINDING, PartType.BOWLIMB, PartType.ARROWSHAFT);

    @Test
    void registersTheExactMaterialRosterWithItsPinnedTierLadder() {
        Map<ResourceLocation, Material> registered = captureBootstrap();
        assertEquals(EXPECTED_TIERS.keySet(), registered.keySet(), "material roster drifted from SMTCON-71 spec");
        EXPECTED_TIERS.forEach((id, tier) -> assertEquals(tier.intValue(), registered.get(id).tier(), () -> "tier drift for " + id));
    }

    @Test
    void everyRegisteredMaterialPopulatesTheCanonicalFiveStatSlotKeys() {
        // Size-only check would pass a map with five wrong keys; pin the key set explicitly so
        // a typo (e.g. swapping PICKHEAD → AXEHEAD on the helper) surfaces here.
        Map<ResourceLocation, Material> registered = captureBootstrap();
        registered.forEach((id, material) -> {
            assertNotNull(material.stats(), () -> "stats null for " + id);
            assertEquals(EXPECTED_STAT_SLOTS, material.stats().keySet(), () -> "unexpected stat slot keys for " + id);
        });
    }

    @Test
    void everySmelteryTierMetalCarriesARepairTag() {
        // The "smeltery-tier" cohort that needs a repair tag covers iron + every metal at
        // tier >= 2: copper, silver, steel, cobalt, ardite, manyullyn. The earlier draft of
        // this test only checked the tier-4+ subset; the scope matches its claim now.
        Map<ResourceLocation, Material> registered = captureBootstrap();
        for (String metal : new String[] { "iron", "copper", "silver", "steel", "cobalt", "ardite", "manyullyn" }) {
            Material material = registered.get(rl(metal));
            assertNotNull(material, () -> metal + " missing from bootstrap roster");
            assertTrue(material.repairTag().isPresent(), () -> metal + " repair tag missing");
        }
    }

    /** Run {@link TinkerMaterialBootstrap#bootstrap} against a recording {@link BootstrapContext}
     *  mock; returns the captured (id → material) map for downstream assertions. */
    @SuppressWarnings("unchecked")
    private static Map<ResourceLocation, Material> captureBootstrap() {
        BootstrapContext<Material> context = mock(BootstrapContext.class);
        Map<ResourceLocation, Material> captured = new HashMap<>();
        doAnswer(invocation -> {
            ResourceKey<Material> key = invocation.getArgument(0);
            Material material = invocation.getArgument(1);
            captured.put(key.location(), material);
            return null;
        }).when(context).register(any(ResourceKey.class), any(Material.class));
        TinkerMaterialBootstrap.bootstrap(context);
        return captured;
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("tconstruct", path);
    }
}
