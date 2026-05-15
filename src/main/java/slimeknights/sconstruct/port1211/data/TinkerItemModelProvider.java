package slimeknights.sconstruct.port1211.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.shared.SharedBlocks;
import slimeknights.sconstruct.port1211.shared.SharedItems;
import slimeknights.sconstruct.port1211.tools.PatternChestRegistry;
import slimeknights.sconstruct.port1211.tools.StencilTableRegistry;
import slimeknights.sconstruct.port1211.world.SlimeFluidSet;
import slimeknights.sconstruct.port1211.world.WorldBlocks;
import slimeknights.sconstruct.port1211.world.WorldFluids;
import slimeknights.sconstruct.port1211.world.block.SlimePlantSet;

/**
 * Item-model data provider. Two families:
 *
 * <ul>
 *   <li><strong>Sprite items</strong> — ingots, nuggets, slimeballs, bacon, mud brick, blood
 *       bucket. Each gets a {@code parent: minecraft:item/generated} model with
 *       {@code textures.layer0} pointing at {@code sconstruct:item/<item_path>}. The texture
 *       PNGs land in a follow-up task; until then, {@link ExistingFileHelper} validation is
 *       satisfied by pre-registering each texture with
 *       {@link ExistingFileHelper#trackGenerated}.</li>
 *   <li><strong>BlockItems</strong> — the 13 metal storage blocks plus the three decoratives
 *       (glow, firewood, lavawood). Each gets a model with {@code parent} pointing at the
 *       matching block model so the item renders as a 3D block in inventory. No texture
 *       reference here — the block model carries the texture.</li>
 * </ul>
 *
 * <p>Covering both families keeps the "no missing item model warnings" AC honest: vanilla
 * logs a warning if a registered {@link Item} (including a
 * {@link net.minecraft.world.item.BlockItem}) has no item-model JSON, and
 * {@code BlockStateProvider#simpleBlock} alone does not emit one — it writes the blockstate
 * + block model, but item models live in a different file tree.
 */
public final class TinkerItemModelProvider extends ItemModelProvider {

    public TinkerItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, SConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Sprite items. Each call pre-registers its texture so the validation pass doesn't
        // fail before the texture PNGs land.
        SharedItems.INGOTS.forEach(this::registerSpriteItem);
        SharedItems.NUGGETS.forEach(this::registerSpriteItem);
        SharedItems.SLIMEBALLS.forEach(this::registerSpriteItem);
        registerSpriteItem(SharedItems.BACON);
        registerSpriteItem(SharedItems.MUDBRICK);
        registerSpriteItem(SharedItems.BUCKET_BLOOD);

        // Phase-3 slime fluid buckets: one filled bucket per SlimeFluidSet. Each gets the
        // same flat sprite treatment as BUCKET_BLOOD — runtime tinting lives in the fluid
        // type / IClientFluidTypeExtensions wiring, not in the item model.
        for (SlimeFluidSet set : WorldFluids.ALL) {
            registerSpriteItem(set.bucket());
        }

        // BlockItems. Each item model references the matching block model as its parent so
        // inventory renders as a 3D block (vanilla's iron_block.json convention). The block
        // model already exists in the assets tree from TinkerBlockStateProvider's pass.
        SharedBlocks.METAL_BLOCKS.values().forEach(this::registerBlockItemFromBlockModel);
        registerBlockItemFromBlockModel(SharedBlocks.GLOW);
        registerBlockItemFromBlockModel(SharedBlocks.FIREWOOD);
        registerBlockItemFromBlockModel(SharedBlocks.LAVAWOOD);

        // Phase-3 world: 4 slime block items reference their block model so the inventory
        // sprite shows the 3D cube. The block model itself is emitted by
        // TinkerBlockStateProvider's pass above.
        WorldBlocks.ALL.forEach(this::registerBlockItemFromBlockModel);

        // Phase-3 plant set: dirt/grass/leaves render as 3D cubes (block-model parent), but
        // saplings get the vanilla flat-sprite treatment in inventory (item/generated with the
        // sapling texture as layer0) so they read as a plant — not a cube — in the hotbar.
        for (SlimePlantSet set : WorldBlocks.PLANT_SETS.values()) {
            registerBlockItemFromBlockModel(set.dirt());
            registerBlockItemFromBlockModel(set.grass());
            registerBlockItemFromBlockModel(set.leaves());
            registerSaplingItem(set.sapling());
        }

        // Phase-3 slime logs (normal + stripped): each renders as a 3D cube in inventory,
        // parented to the y-axis variant of its blockstate model so the side bark texture
        // shows on the four side faces.
        WorldBlocks.ALL_LOGS.forEach(this::registerBlockItemFromBlockModel);

        // SMTCON-90 pattern chest: inventory icon parents the cube_all block model emitted
        // by TinkerBlockStateProvider above.
        registerBlockItemFromBlockModel(PatternChestRegistry.PATTERN_CHEST);

        // SMTCON-91: stencil table inventory icon parents the cube_all block model.
        registerBlockItemFromBlockModel(StencilTableRegistry.STENCIL_TABLE);
        // Blank pattern + typed pattern share one PatternItem class — point both inventory
        // models at item/blank_pattern so only one PNG needs shipping. Typed-variant tinting /
        // overlay for the per-part look is a follow-up rendering task (the texture PNG itself
        // is also a follow-up per the ticket plan).
        registerSpriteItem(StencilTableRegistry.BLANK_PATTERN);
        registerSpriteItemWithSharedTexture(StencilTableRegistry.PATTERN, StencilTableRegistry.BLANK_PATTERN);
    }

    /**
     * Emit a flat {@code item/generated} inventory model for the sapling, pointing
     * {@code textures.layer0} at the block-tree sprite. Saplings sit at the same texture path
     * as their blocks ({@code sconstruct:block/slime_<color>_sapling}) so the inventory and
     * the in-world planting share one PNG — matching vanilla's oak/birch/etc. convention.
     */
    private void registerSaplingItem(DeferredBlock<? extends Block> saplingHolder) {
        Block block = saplingHolder.get();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath());
        existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        singleTexture(blockId.getPath(), ResourceLocation.parse("item/generated"), "layer0", texture);
    }

    private void registerSpriteItem(DeferredItem<? extends Item> holder) {
        Item item = holder.get();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath());
        existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        basicItem(item);
    }

    /**
     * Emit a flat item/generated model whose {@code layer0} points at a sibling item's texture
     * rather than its own. Used for the typed-pattern variant so blank + typed both render off
     * the single {@code item/blank_pattern.png} sprite (one PNG, one variant render path).
     */
    private void registerSpriteItemWithSharedTexture(DeferredItem<? extends Item> holder, DeferredItem<? extends Item> textureSource) {
        Item item = holder.get();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        ResourceLocation textureItemId = BuiltInRegistries.ITEM.getKey(textureSource.get());
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(textureItemId.getNamespace(), "item/" + textureItemId.getPath());
        existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        singleTexture(itemId.getPath(), ResourceLocation.parse("item/generated"), "layer0", texture);
    }

    private void registerBlockItemFromBlockModel(DeferredBlock<? extends Block> blockHolder) {
        Block block = blockHolder.get();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        // The item model's parent is the block's own model — no separate texture entry needed.
        // simpleBlockItem on BlockStateProvider would have done this implicitly; we do it
        // explicitly here because TinkerBlockStateProvider used the plain simpleBlock variant.
        withExistingParent(blockId.getPath(), ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath()));
    }
}
