package slimeknights.sconstruct.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.shared.SharedBlocks;
import slimeknights.sconstruct.shared.SharedItems;
import slimeknights.sconstruct.shared.SharedMetals;
import slimeknights.sconstruct.tools.PartType;
import slimeknights.sconstruct.tools.item.ToolItems;
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

    /**
     * SMTCON-194: every cube-textured seared block. The four stair/slab blocks are omitted —
     * their models reuse the matching base block's texture ({@code seared_brick} /
     * {@code seared_paver}), already covered here.
     */
    private static final java.util.List<String> SEARED_CUBE_TEXTURES = java.util.List.of("seared_stone", "seared_cobble", "seared_paver", "seared_brick", "seared_brick_chiseled",
            "seared_brick_squared", "seared_brick_creeper", "seared_brick_road", "seared_brick_fancy", "seared_brick_triangle", "seared_glass", "seared_window");

    @Test
    void everySearedBlockHasABlockTexture() {
        // Ported from the legacy smeltery texture tree. Drift here renders the seared block as
        // the missing-texture sprite in-world and in inventory.
        assertAll(SEARED_CUBE_TEXTURES.stream().map(id -> () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + id + ".png"), BLOCK_TEXTURE_ROOT + id + ".png missing")));
    }

    @Test
    void everySmelteryComponentHasABlockTexture() {
        // SMTCON-197: the six SmelteryComponents blocks, each modelled as a horizontal cube_all
        // with one texture per id. Drift here renders the component as the missing-texture sprite.
        assertAll(java.util.stream.Stream.of("smeltery_controller", "seared_tank_io", "seared_tank_in", "seared_tank_gauge", "seared_drain", "seared_chute")
                .map(id -> () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + id + ".png"), BLOCK_TEXTURE_ROOT + id + ".png missing")));
    }

    @Test
    void everyAssembledToolHasOneLayerTexturePerPartSlot() {
        // SMTCON-198: each ToolCore ships one greyscale layer sprite per part slot under
        // item/tool/<tool>/<N>.png. A missing layer renders that part as the missing-texture
        // sprite while the rest of the tool draws fine.
        assertAll(ToolItems.ALL_TOOLS.stream().flatMap(holder -> {
            String tool = holder.getId().getPath();
            int parts = holder.get().definition.getPartCount();
            return java.util.stream.IntStream.range(0, parts).mapToObj(layer -> () -> {
                String path = ITEM_TEXTURE_ROOT + "tool/" + tool + "/" + layer + ".png";
                assertNotNull(loader().getResource(path), path + " missing");
            });
        }));
    }

    @Test
    void shurikenHasAnItemTexture() {
        // SMTCON-198: the shuriken is a flat sprite (plain Item, not a ToolCore). Pin its
        // texture so it cannot go missing alongside the layered tool textures.
        String path = ITEM_TEXTURE_ROOT + ToolItems.SHURIKEN.getId().getPath() + ".png";
        assertNotNull(loader().getResource(path), path + " missing");
    }

    @Test
    void castingBlockAndPatternTexturesArePresent() {
        // SMTCON-196: the two cube_all casting blocks plus the blank pattern sprite (shared by
        // the typed pattern item). Drift here renders them as the missing-texture sprite.
        assertAll(() -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "casting_table.png"), BLOCK_TEXTURE_ROOT + "casting_table.png missing"),
                () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + "casting_basin.png"), BLOCK_TEXTURE_ROOT + "casting_basin.png missing"),
                () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + "blank_pattern.png"), ITEM_TEXTURE_ROOT + "blank_pattern.png missing"));
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
    void everyToolPartHasAnItemTexture() {
        // SMTCON-195: one greyscale sprite per PartType, ported from the legacy tool texture
        // tree. Drift here renders the part item as the missing-texture sprite in inventory.
        assertAll(java.util.Arrays.stream(PartType.values())
                .map(part -> () -> assertNotNull(loader().getResource(ITEM_TEXTURE_ROOT + part.id() + ".png"), ITEM_TEXTURE_ROOT + part.id() + ".png missing")));
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
    void metalIngotNuggetAndBlockTexturesAreShapedSpritesNotFlatSwatches() {
        // SMTCON-200: every metal ingot, nugget, and storage-block texture must be a shaped
        // sprite. A single-colour image is a leftover placeholder swatch — it renders as a
        // plain coloured square in the creative menu instead of a real ingot/nugget/block icon.
        assertAll(SharedMetals.ALL.stream().flatMap(metal -> {
            java.util.List<String> paths = new java.util.ArrayList<>();
            paths.add(ITEM_TEXTURE_ROOT + "ingot_" + metal.id() + ".png");
            paths.add(ITEM_TEXTURE_ROOT + "nugget_" + metal.id() + ".png");
            if (SharedBlocks.METAL_BLOCKS.containsKey(metal.id())) {
                paths.add(BLOCK_TEXTURE_ROOT + "block_" + metal.id() + ".png");
            }
            return paths.stream().map(path -> () -> assertTrue(distinctColours(path) > 1, path + " is a flat single-colour placeholder swatch, not a shaped sprite"));
        }));
    }

    /** Count of distinct ARGB pixel values in a classpath texture — 1 means a flat colour swatch. */
    private static int distinctColours(String resource) {
        URL url = loader().getResource(resource);
        assertNotNull(url, resource + " missing");
        try {
            BufferedImage img = ImageIO.read(url);
            assertNotNull(img, resource + " is not a readable image");
            Set<Integer> colours = new HashSet<>();
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    colours.add(img.getRGB(x, y));
                }
            }
            return colours.size();
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + resource, e);
        }
    }

    @Test
    void slimeballsBaconAndMudbrickHaveItemTextures() {
        // SMTCON-202: the blood bucket is no longer a flat sprite — it renders through the
        // neoforge:fluid_container model, so it has no item/blood_bucket.png to assert.
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
                        ITEM_TEXTURE_ROOT + SharedItems.MUDBRICK.getId().getPath() + ".png missing"));
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
    void moltenMetalSharedFluidTexturesArePresent() {
        // SMTCON-193: unlike slime fluids, all 20 molten metals share one greyscale still/flow
        // texture pair and differ only by the per-metal runtime tint. Drift here would render
        // every molten metal as the missing-texture sprite in-world and in its bucket.
        assertAll(java.util.stream.Stream.of("fluid/molten_metal_still.png", "fluid/molten_metal_flow.png", "fluid/molten_metal_still.png.mcmeta", "fluid/molten_metal_flow.png.mcmeta")
                .map(path -> () -> assertNotNull(loader().getResource(BLOCK_TEXTURE_ROOT + path), BLOCK_TEXTURE_ROOT + path + " missing")));
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
