package slimeknights.sconstruct.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.shared.SharedBlocks;
import slimeknights.sconstruct.shared.SharedItems;
import slimeknights.sconstruct.shared.SharedMetals;
import slimeknights.sconstruct.world.SlimeFluidSet;
import slimeknights.sconstruct.world.WorldFluids;
import slimeknights.sconstruct.world.block.SlimeColor;

/**
 * Pinned-presence tests for Phase-2 texture assets. {@link TinkerBlockStateProvider} and
 * {@link TinkerItemModelProvider} emit JSON models that reference
 * {@code sconstruct:block/<id>} and {@code sconstruct:item/<id>}; this test verifies every
 * referenced texture PNG actually lives at
 * {@code src/main/resources/assets/sconstruct/textures/{block,item}/<id>.png} so the in-game
 * renderer doesn't fall back to the missing-texture sprite.
 *
 * <p>Textures live under {@code src/main/resources/} (not {@code generated/resources/}) —
 * they're committed verbatim from the legacy 1.12 asset tree (re-pathed from {@code blocks/}
 * to the modern singular {@code block/}) with solid-colour placeholders generated for
 * metals the legacy mod did not ship a block/ingot/nugget texture for.
 */
class SharedTextureAssetsTest {

    private static final String BLOCK_TEXTURE_ROOT = "assets/sconstruct/textures/block/";
    private static final String ITEM_TEXTURE_ROOT = "assets/sconstruct/textures/item/";

    @Test
    void everyMetalStorageBlockHasABlockTexture() {
        assertAll(SharedBlocks.METAL_BLOCKS.keySet().stream().map(id -> () -> {
            String blockPath = "block_" + id;
            assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + blockPath + ".png"), blockPath + ".png missing — copy from legacy or generate a placeholder?");
        }));
    }

    @Test
    void decorativeBlocksHaveTextures() {
        assertAll(() -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "glow.png"), BLOCK_TEXTURE_ROOT + "glow.png missing"),
                () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "firewood.png"), BLOCK_TEXTURE_ROOT + "firewood.png missing"),
                () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "lavawood.png"), BLOCK_TEXTURE_ROOT + "lavawood.png missing"));
    }

    @Test
    void firewoodHasAnimationMcmeta() {
        // The legacy mod animates the firewood top texture. Preserving the mcmeta keeps the
        // animation intact post-port.
        assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "firewood.png.mcmeta"), BLOCK_TEXTURE_ROOT + "firewood.png.mcmeta missing");
    }

    @Test
    void bloodFluidTexturesAndAnimationStillPresent() {
        // Shipped earlier by SMTCON-39; pin here so a future texture refactor doesn't
        // accidentally rebase the fluid into the wrong subdirectory.
        assertAll(() -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "fluid/bloodstill.png"), BLOCK_TEXTURE_ROOT + "fluid/bloodstill.png missing"),
                () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "fluid/bloodflow.png"), BLOCK_TEXTURE_ROOT + "fluid/bloodflow.png missing"),
                () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "fluid/bloodflow.png.mcmeta"), BLOCK_TEXTURE_ROOT + "fluid/bloodflow.png.mcmeta missing"));
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
        assertAll(
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_BLUE.getId().getPath() + ".png"),
                        ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_BLUE.getId().getPath() + ".png missing"),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_PURPLE.getId().getPath() + ".png"),
                        ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_PURPLE.getId().getPath() + ".png missing"),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_BLOOD.getId().getPath() + ".png"),
                        ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_BLOOD.getId().getPath() + ".png missing"),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_MAGMA.getId().getPath() + ".png"),
                        ITEM_TEXTURE_ROOT + SharedItems.SLIMEBALL_MAGMA.getId().getPath() + ".png missing"),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.BACON.getId().getPath() + ".png"), ITEM_TEXTURE_ROOT + SharedItems.BACON.getId().getPath() + ".png missing"),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.MUDBRICK.getId().getPath() + ".png"),
                        ITEM_TEXTURE_ROOT + SharedItems.MUDBRICK.getId().getPath() + ".png missing"),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + SharedItems.BUCKET_BLOOD.getId().getPath() + ".png"),
                        ITEM_TEXTURE_ROOT + SharedItems.BUCKET_BLOOD.getId().getPath() + ".png missing"));
    }

    @Test
    void everySlimeColorHasItsFivePhase3WorldBlockTextures() {
        // Phase-3 plant set + bouncy block per colour: block, dirt, grass, leaves, sapling.
        // Driven from SlimeColor.values() so a new colour lights up the test by adding the
        // enum entry plus the matching PNGs — no edit here.
        assertAll(java.util.Arrays.stream(SlimeColor.values()).flatMap(color -> java.util.stream.Stream.of("block", "dirt", "grass", "leaves", "sapling").map(suffix -> () -> {
            String path = "slime_" + color.id() + "_" + suffix;
            assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + path + ".png"), BLOCK_TEXTURE_ROOT + path + ".png missing");
        })));
    }

    @Test
    void everySlimeColorHasItsLogAndStrippedLogTextures() {
        // Slime logs ship side + top PNGs for both axis faces; the stripped variant doubles
        // the count. Two-axis-per-log shape pinned so a future refactor that drops the _top
        // texture (and falls back to the side on the bark face) surfaces here.
        assertAll(java.util.Arrays.stream(SlimeColor.values()).flatMap(color -> java.util.stream.Stream
                .of("slime_" + color.id() + "_log", "slime_" + color.id() + "_log_top", "stripped_slime_" + color.id() + "_log", "stripped_slime_" + color.id() + "_log_top").map(path -> () -> {
                    assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + path + ".png"), BLOCK_TEXTURE_ROOT + path + ".png missing");
                })));
    }

    @Test
    void slimeFluidStillAndFlowTexturesArePresent() {
        // Each WorldFluids set lands two PNGs (still + flow) under block/fluid/, plus the
        // mcmeta animation descriptors. Drift here would manifest as a flat untinted fluid
        // in-world.
        assertAll(WorldFluids.ALL.stream().flatMap(set -> {
            String id = set.source().getId().getPath();
            return java.util.stream.Stream.of("fluid/" + id + "_still.png", "fluid/" + id + "_flow.png", "fluid/" + id + "_still.png.mcmeta", "fluid/" + id + "_flow.png.mcmeta").map(path -> () -> {
                assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + path), BLOCK_TEXTURE_ROOT + path + " missing");
            });
        }));
    }

    @Test
    void slimeFluidBucketItemTexturesArePresent() {
        // Generated from blood_bucket.png via BT.601 desaturate + per-fluid tint multiply.
        // Without these PNGs, the four slime bucket items render the missing-texture sprite.
        assertAll(WorldFluids.ALL.stream().map((SlimeFluidSet set) -> () -> {
            String path = set.bucket().getId().getPath();
            assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + path + ".png"), ITEM_TEXTURE_ROOT + path + ".png missing");
        }));
    }

    @Test
    void blueslimeAndHugeslimeEntityTexturesArePresent() {
        // Both renderers reference these PNGs; missing files surface as a black-and-purple
        // entity model in-game.
        assertAll(() -> assertNotNull(loader().getResource("assets/sconstruct/textures/entity/slime/blueslime.png"), "entity/slime/blueslime.png missing"),
                () -> assertNotNull(loader().getResource("assets/sconstruct/textures/entity/slime/hugeslime.png"), "entity/slime/hugeslime.png missing"));
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
