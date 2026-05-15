package slimeknights.sconstruct.port1211.data.material;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.tools.material.Material;

/**
 * Pinned-behaviour tests for {@link TinkerMaterialBootstrap}. Captures the entries the bootstrap
 * registers into a {@link BootstrapContext} mock, then asserts the roster surface SMTCON-71
 * promises: 15 materials at the right ids in the {@code tconstruct} namespace with the
 * tier-ladder the AC pins (iron=2, cobalt=4, manyullyn=5, wood=0).
 */
class TinkerMaterialBootstrapTest {

    @Test
    void registersFifteenMaterialsInTheTconstructNamespaceWithTheExpectedTiers() {
        Map<ResourceLocation, Material> registered = captureBootstrap();

        assertEquals(15, registered.size(), "SMTCON-71 ships fifteen base materials");

        // Namespace contract: every material lives under tconstruct: so legacy addon material
        // lookups resolve against this roster. Any future drift would split addon compatibility.
        registered.keySet().forEach(id -> assertEquals("tconstruct", id.getNamespace(), () -> "non-tconstruct material id: " + id));

        // Tier ladder pinned by AC: wood=0, iron=2, cobalt=4, manyullyn=5. Drift in any of these
        // breaks downstream tool-default tier math.
        assertAll(() -> assertEquals(0, registered.get(rl("wood")).tier()), () -> assertEquals(1, registered.get(rl("stone")).tier()), () -> assertEquals(2, registered.get(rl("iron")).tier()),
                () -> assertEquals(0, registered.get(rl("gold")).tier()), () -> assertEquals(4, registered.get(rl("cobalt")).tier()), () -> assertEquals(4, registered.get(rl("ardite")).tier()),
                () -> assertEquals(5, registered.get(rl("manyullyn")).tier()));
    }

    @Test
    void everyRegisteredMaterialPopulatesFiveStatSlots() {
        // The bootstrap helper attaches HeadStats/HandleStats/ExtraStats/BowStats/ArrowStats to
        // every material. Catches a regression where one material drops a slot — downstream
        // tool builds rely on the full five-slot set being present.
        Map<ResourceLocation, Material> registered = captureBootstrap();
        registered.forEach((id, material) -> {
            assertNotNull(material.stats(), () -> "stats null for " + id);
            assertEquals(5, material.stats().size(), () -> id + " must have all five stat slots populated");
        });
    }

    @Test
    void smelteryTierMetalsCarryRepairTags() {
        // Tier-4+ metals each ship a repair tag in c:ingots/<metal> so anvil repair stays
        // consistent with the legacy material registry.
        Map<ResourceLocation, Material> registered = captureBootstrap();
        assertAll(() -> assertTrue(registered.get(rl("cobalt")).repairTag().isPresent(), "cobalt repair tag missing"),
                () -> assertTrue(registered.get(rl("ardite")).repairTag().isPresent(), "ardite repair tag missing"),
                () -> assertTrue(registered.get(rl("manyullyn")).repairTag().isPresent(), "manyullyn repair tag missing"),
                () -> assertTrue(registered.get(rl("iron")).repairTag().isPresent(), "iron repair tag missing"));
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
