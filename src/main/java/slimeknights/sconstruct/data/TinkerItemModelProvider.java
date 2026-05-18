package slimeknights.sconstruct.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.gadgets.GadgetBlocks;
import slimeknights.sconstruct.gadgets.GadgetItems;
import slimeknights.sconstruct.shared.SharedBlocks;
import slimeknights.sconstruct.shared.SharedItems;
import slimeknights.sconstruct.smeltery.CastingBlocks;
import slimeknights.sconstruct.smeltery.MoltenFluidSet;
import slimeknights.sconstruct.smeltery.SearedBlocks;
import slimeknights.sconstruct.smeltery.SmelteryComponents;
import slimeknights.sconstruct.smeltery.SmelteryFluids;
import slimeknights.sconstruct.tools.PartBuilderRegistry;
import slimeknights.sconstruct.tools.PatternChestRegistry;
import slimeknights.sconstruct.tools.StencilTableRegistry;
import slimeknights.sconstruct.tools.ToolStationRegistry;
import slimeknights.sconstruct.tools.item.ToolParts;
import slimeknights.sconstruct.world.SlimeFluidSet;
import slimeknights.sconstruct.world.WorldBlocks;
import slimeknights.sconstruct.world.WorldFluids;
import slimeknights.sconstruct.world.block.SlimePlantSet;

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

    /** Empty-bucket sprite used as the {@code base} layer of every molten-metal fluid container. */
    private static final ResourceLocation BUCKET_BASE_TEXTURE = ResourceLocation.withDefaultNamespace("item/bucket");

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
        registerSpriteItem(SharedItems.MATERIALS_BOOK);

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

        // SMTCON-92 part builder: inventory icon parents the cube_all block model emitted
        // by TinkerBlockStateProvider above.
        registerBlockItemFromBlockModel(PartBuilderRegistry.PART_BUILDER);

        // SMTCON-93 tool station + tool forge: inventory icons parent the cube_all block
        // models emitted by TinkerBlockStateProvider above.
        registerBlockItemFromBlockModel(ToolStationRegistry.TOOL_STATION);
        registerBlockItemFromBlockModel(ToolStationRegistry.TOOL_FORGE);

        // SMTCON-129: the smeltery block items (seared blocks, the six components, the two
        // casting blocks) parent their block models.
        SearedBlocks.ALL.forEach(this::registerBlockItemFromBlockModel);
        SmelteryComponents.ALL.forEach(this::registerBlockItemFromBlockModel);
        CastingBlocks.ALL.forEach(this::registerBlockItemFromBlockModel);
        // SMTCON-193: the 20 molten-metal buckets render through NeoForge's neoforge:fluid_container
        // dynamic model — an empty bucket base plus the fluid's own still sprite, tinted per metal
        // by the fluid-type extension. No per-bucket PNG, mirroring the shared-texture-plus-tint
        // approach the molten fluids themselves use.
        for (MoltenFluidSet set : SmelteryFluids.ALL) {
            registerMoltenBucket(set);
        }

        // SMTCON-195: the 18 tool-part items. Each MaterialItem gets a flat item/generated
        // sprite (layer0 → sconstruct:item/<part_id>); the per-material colour is applied at
        // render time by the ToolColorHandlers ItemColor on layer 0.
        ToolParts.PARTS.values().forEach(this::registerSpriteItem);

        // SMTCON-143: Phase-6 gadget items + block-items.
        //   - the 15 gadget items (slings, throwballs, piggyback, glow ball, wither head, armor)
        //     get the flat item/generated sprite treatment, layer0 → sconstruct:item/<id>.
        //   - dried clay + dried clay brick parent their cube_all block models like any cube.
        //   - the drying rack's block model varies per DRYING_STATE, so the block-item parents
        //     the "empty" state model — the resting inventory look.
        //   - the wooden hopper's block-item parents the down-facing hopper model
        //     (block/wooden_hopper) emitted by TinkerBlockStateProvider.
        //   - the stone ladder's block-item gets a flat item/generated sprite (vanilla ladders
        //     are flat sprites in inventory, not 3D models).
        for (DeferredItem<? extends Item> holder : GadgetItems.ALL) {
            registerSpriteItem(holder);
        }
        registerBlockItemFromBlockModel(GadgetBlocks.DRIED_CLAY);
        registerBlockItemFromBlockModel(GadgetBlocks.DRIED_CLAY_BRICK);
        registerBlockItemParentedTo(GadgetBlocks.DRYING_RACK, "block/drying_rack_empty");
        registerBlockItemParentedTo(GadgetBlocks.WOODEN_HOPPER, "block/wooden_hopper");
        registerLadderItem(GadgetBlocks.STONE_LADDER);
    }

    /**
     * Emit an item model for a block-item that parents an explicit block model path rather than
     * the {@code block/<registry_path>} default. Used where the block's model name diverges from
     * its registry id — the drying rack (per-state models) and the wooden hopper (the funnel
     * model is the down-facing variant).
     */
    private void registerBlockItemParentedTo(DeferredBlock<? extends Block> blockHolder, String modelPath) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockHolder.get());
        withExistingParent(blockId.getPath(), ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), modelPath));
    }

    /**
     * Emit a flat {@code item/generated} inventory model for a ladder-style block-item, pointing
     * {@code layer0} at the block-tree sprite. Mirrors vanilla's ladder: the inventory icon is a
     * flat sprite, not the 3D rung model.
     */
    private void registerLadderItem(DeferredBlock<? extends Block> blockHolder) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockHolder.get());
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath());
        existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        singleTexture(blockId.getPath(), ResourceLocation.parse("item/generated"), "layer0", texture);
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

    /**
     * Emit a {@code neoforge:fluid_container} dynamic model for one molten-metal bucket. The
     * model composites the vanilla empty-bucket sprite ({@code minecraft:item/bucket}, the
     * {@code base} layer) with the fluid's own still texture, tinted by the fluid-type
     * extension — so a single shared fluid sprite covers all 20 buckets with no per-bucket PNG.
     */
    private void registerMoltenBucket(MoltenFluidSet set) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(set.bucket().get());
        getBuilder(itemId.getPath()).texture("base", BUCKET_BASE_TEXTURE).customLoader(DynamicFluidContainerModelBuilder::begin).fluid(set.source().get()).applyTint(true).flipGas(false).end();
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
