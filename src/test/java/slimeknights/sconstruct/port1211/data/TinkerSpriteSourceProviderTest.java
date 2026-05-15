package slimeknights.sconstruct.port1211.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.tools.PartType;

/**
 * Pinned-behaviour tests for the static manifest emitted by {@link TinkerSpriteSourceProvider}.
 * The {@code SpriteSourceProvider} side requires a live {@code DataGenerator}/{@code PackOutput}
 * to construct, so the {@code gather()} call is exercised end-to-end by {@code runData} in CI;
 * the unit-test surface here pins the structural inputs the provider hands to vanilla's
 * {@code PalettedPermutations} so a regression in either array changes one test, not many.
 */
class TinkerSpriteSourceProviderTest {

    @Test
    void baseTexturesCoverEveryPartTypeInDeclarationOrder() {
        assertEquals(PartType.values().length, TinkerSpriteSourceProvider.BASE_TEXTURES.size(), "must emit one base texture per PartType — the per-material baked tool model needs every slot");

        List<String> expectedPaths = new ArrayList<>();
        for (PartType part : PartType.values()) {
            expectedPaths.add("item/parts/" + part.id() + "_grayscale");
        }
        List<String> actualPaths = TinkerSpriteSourceProvider.BASE_TEXTURES.stream().map(ResourceLocation::getPath).toList();
        assertEquals(expectedPaths, actualPaths, "base textures must follow PartType.values() order so the JSON permutation block is reproducible");
    }

    @Test
    void allBaseTexturesAreNamespacedToTheMod() {
        TinkerSpriteSourceProvider.BASE_TEXTURES
                .forEach(rl -> assertEquals(SConstruct.MOD_ID, rl.getNamespace(), "base texture must live under the mod's namespace — vanilla atlas resolution honours per-namespace overrides"));
    }

    @Test
    void materialPalettesCoverEveryBootstrappedMaterial() {
        // 15 materials registered by TinkerMaterialBootstrap (wood/stone/iron/gold + flint/bone/
        // paper/slime/blueslime + 6 metals). Tightening the count rather than a string-by-string
        // pin keeps this test resilient to material-id renames while still failing if a material
        // is dropped or accidentally double-listed.
        assertEquals(15, TinkerSpriteSourceProvider.MATERIAL_PALETTES.size(), "expected one palette per bootstrapped material");

        // Spot-check a representative from each material category to catch any wholesale loss.
        assertNotNull(TinkerSpriteSourceProvider.MATERIAL_PALETTES.get("wood"), "wood — base tier-0 material must be present");
        assertNotNull(TinkerSpriteSourceProvider.MATERIAL_PALETTES.get("iron"), "iron — common tier-2 material must be present");
        assertNotNull(TinkerSpriteSourceProvider.MATERIAL_PALETTES.get("paper"), "paper — specialty material must be present");
        assertNotNull(TinkerSpriteSourceProvider.MATERIAL_PALETTES.get("manyullyn"), "manyullyn — late-game smeltery metal must be present");
    }

    @Test
    void allPaletteEntriesUseConsistentPathFormat() {
        TinkerSpriteSourceProvider.MATERIAL_PALETTES.forEach((material, palette) -> {
            assertEquals(SConstruct.MOD_ID, palette.getNamespace(), "palette PNG must be mod-namespaced");
            String expected = "item/parts/_palette_" + material;
            assertEquals(expected, palette.getPath(), "palette path must follow item/parts/_palette_<material> — drift breaks the SMTCON-105 baked-model lookup");
        });
    }

    @Test
    void paletteKeyIsAtTheStandardLocation() {
        assertEquals(SConstruct.MOD_ID, TinkerSpriteSourceProvider.PALETTE_KEY.getNamespace());
        assertEquals("item/parts/_palette_key", TinkerSpriteSourceProvider.PALETTE_KEY.getPath(), "palette key path drives the grayscale gradient sampling — keep it under item/parts/");
    }

    @Test
    void materialPaletteMapIsImmutableSoNoOneCanMutateTheManifestAtRuntime() {
        // The manifest is exposed publicly for SMTCON-105 / -107 to read; a mutable view would
        // let a caller accidentally append a stale entry that survives the next datagen run.
        ResourceLocation rogue = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "item/parts/_palette_rogue");
        assertThrows(UnsupportedOperationException.class, () -> TinkerSpriteSourceProvider.MATERIAL_PALETTES.put("rogue", rogue), "MATERIAL_PALETTES must be unmodifiable — put() must throw");
        assertNull(TinkerSpriteSourceProvider.MATERIAL_PALETTES.get("rogue"), "mutation attempt must not have leaked into the cache");
    }
}
