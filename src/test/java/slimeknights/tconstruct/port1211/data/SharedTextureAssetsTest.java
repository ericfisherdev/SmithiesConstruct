package slimeknights.tconstruct.port1211.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import slimeknights.tconstruct.port1211.shared.SharedBlocks;
import slimeknights.tconstruct.port1211.shared.SharedItems;
import slimeknights.tconstruct.port1211.shared.SharedMetals;

/**
 * Pinned-presence tests for Phase-2 texture assets. {@link TinkerBlockStateProvider} and
 * {@link TinkerItemModelProvider} emit JSON models that reference
 * {@code tconstruct:block/<id>} and {@code tconstruct:item/<id>}; this test verifies every
 * referenced texture PNG actually lives at
 * {@code src/main/resources/assets/tconstruct/textures/{block,item}/<id>.png} so the in-game
 * renderer doesn't fall back to the missing-texture sprite.
 *
 * <p>Textures live under {@code src/main/resources/} (not {@code generated/resources/}) —
 * they're committed verbatim from the legacy 1.12 asset tree (re-pathed from {@code blocks/}
 * to the modern singular {@code block/}) with solid-colour placeholders generated for
 * metals the legacy mod did not ship a block/ingot/nugget texture for.
 */
class SharedTextureAssetsTest {

    private static final String BLOCK_TEXTURE_ROOT = "assets/tconstruct/textures/block/";
    private static final String ITEM_TEXTURE_ROOT = "assets/tconstruct/textures/item/";

    @Test
    void everyMetalStorageBlockHasABlockTexture() {
        assertAll(SharedBlocks.METAL_BLOCKS.keySet().stream().map(id -> () -> {
            String blockPath = "block_" + id;
            assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + blockPath + ".png"), blockPath + ".png missing — copy from legacy or generate a placeholder?");
        }));
    }

    @Test
    void decorativeBlocksHaveTextures() {
        assertAll(() -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "glow.png")), () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "firewood.png")),
                () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "lavawood.png")));
    }

    @Test
    void firewoodHasAnimationMcmeta() {
        // The legacy mod animates the firewood top texture. Preserving the mcmeta keeps the
        // animation intact post-port.
        assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "firewood.png.mcmeta"));
    }

    @Test
    void bloodFluidTexturesAndAnimationStillPresent() {
        // Shipped earlier by SMTCON-39; pin here so a future texture refactor doesn't
        // accidentally rebase the fluid into the wrong subdirectory.
        assertAll(() -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "fluid/bloodstill.png")), () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "fluid/bloodflow.png")),
                () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "fluid/bloodflow.png.mcmeta")));
    }

    @Test
    void everyMetalIngotAndNuggetHasAnItemTexture() {
        // Coverage driven from SharedMetals.ALL — a new metal lights up the test by
        // appending one entry to the driver, no edit here.
        assertAll(SharedMetals.ALL.stream().map(metal -> () -> {
            String ingot = "ingot_" + metal.id();
            String nugget = "nugget_" + metal.id();
            assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + ingot + ".png"), ingot + ".png missing");
            assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + nugget + ".png"), nugget + ".png missing");
        }));
    }

    @Test
    void slimeballsBaconMudbrickBloodBucketHaveItemTextures() {
        assertAll(() -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_BLUE.getId().getPath() + ".png")),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_PURPLE.getId().getPath() + ".png")),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_BLOOD.getId().getPath() + ".png")),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_MAGMA.getId().getPath() + ".png")),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.BACON.getId().getPath() + ".png")),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.MUDBRICK.getId().getPath() + ".png")),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.BUCKET_BLOOD.getId().getPath() + ".png")));
    }

    @SuppressWarnings("PMD.UseProperClassLoader") // proper context loader checked first; fallback fires only when null
    private static ClassLoader loader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = SharedTextureAssetsTest.class.getClassLoader();
        }
        return cl;
    }
}
