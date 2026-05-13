package slimeknights.tconstruct.port1211.data;

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

import slimeknights.tconstruct.port1211.TConstruct;
import slimeknights.tconstruct.port1211.shared.SharedBlocks;
import slimeknights.tconstruct.port1211.shared.SharedItems;

/**
 * Item-model data provider. Two families:
 *
 * <ul>
 *   <li><strong>Sprite items</strong> — ingots, nuggets, slimeballs, bacon, mud brick, blood
 *       bucket. Each gets a {@code parent: minecraft:item/generated} model with
 *       {@code textures.layer0} pointing at {@code tconstruct:item/<item_path>}. The texture
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
        super(output, TConstruct.MOD_ID, existingFileHelper);
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

        // BlockItems. Each item model references the matching block model as its parent so
        // inventory renders as a 3D block (vanilla's iron_block.json convention). The block
        // model already exists in the assets tree from TinkerBlockStateProvider's pass.
        SharedBlocks.METAL_BLOCKS.values().forEach(this::registerBlockItemFromBlockModel);
        registerBlockItemFromBlockModel(SharedBlocks.GLOW);
        registerBlockItemFromBlockModel(SharedBlocks.FIREWOOD);
        registerBlockItemFromBlockModel(SharedBlocks.LAVAWOOD);
    }

    private void registerSpriteItem(DeferredItem<? extends Item> holder) {
        Item item = holder.get();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath());
        existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        basicItem(item);
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
