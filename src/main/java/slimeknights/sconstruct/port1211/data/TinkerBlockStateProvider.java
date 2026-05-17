package slimeknights.sconstruct.port1211.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.shared.SharedBlocks;
import slimeknights.sconstruct.port1211.smeltery.CastingBlocks;
import slimeknights.sconstruct.port1211.smeltery.SearedBlocks;
import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;
import slimeknights.sconstruct.port1211.tools.PartBuilderRegistry;
import slimeknights.sconstruct.port1211.tools.PatternChestRegistry;
import slimeknights.sconstruct.port1211.tools.StencilTableRegistry;
import slimeknights.sconstruct.port1211.tools.ToolStationRegistry;
import slimeknights.sconstruct.port1211.world.WorldBlocks;
import slimeknights.sconstruct.port1211.world.block.SlimePlantSet;
import slimeknights.sconstruct.port1211.world.block.SlimeSaplingBlock;

/**
 * Blockstate-and-model data provider. Emits one {@code variants}-style blockstate JSON and
 * matching {@code cube_all} model per shared-pulse block. {@link #simpleBlock(Block)} is the
 * right helper for every block this provider owns: metal storage and the wood decoratives
 * are full opaque cubes with a single all-faces texture; the glow block is also a single-
 * texture cube here (the cutout/transparent layer + per-face FACING orientation arrive in a
 * later rendering task per the SMTCON-37 AC).
 *
 * <p>Texture references default to {@code sconstruct:block/<block_path>} — e.g.
 * {@code sconstruct:block/block_cobalt}. The texture PNGs land in a follow-up task; until
 * then, the in-game render uses the missing-texture sprite, but the JSON model side is
 * complete and well-formed so vanilla won't log "missing model" warnings.
 */
public final class TinkerBlockStateProvider extends BlockStateProvider {

    public TinkerBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, SConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Metal storage blocks — iterate the live registry view. A new metal added via
        // SharedBlocks lights up its blockstate + model with no edit here.
        for (DeferredBlock<Block> holder : SharedBlocks.METAL_BLOCKS.values()) {
            registerCubeAll(holder.get());
        }
        // Decoratives. Glow, firewood, and lavawood all use the same cube_all shape — the
        // legacy 1.12 mod's per-face FACING orientation for glow is a Phase-9 rendering task.
        registerCubeAll(SharedBlocks.GLOW.get());
        registerCubeAll(SharedBlocks.FIREWOOD.get());
        registerCubeAll(SharedBlocks.LAVAWOOD.get());

        // Phase-3 world: four coloured slime blocks. Each gets the same cube_all shape pointing
        // at sconstruct:block/slime_<color>_block. A new colour added in WorldBlocks lights up
        // here with no edit.
        WorldBlocks.ALL.forEach(holder -> registerCubeAll(holder.get()));

        // Phase-3 plant sets: dirt, grass, leaves use cubeAll; sapling uses the vanilla
        // "cross" model (two intersecting flat planes) matching the oak sapling shape.
        for (SlimePlantSet set : WorldBlocks.PLANT_SETS.values()) {
            registerCubeAll(set.dirt().get());
            registerCubeAll(set.grass().get());
            registerCubeAll(set.leaves().get());
            registerCross(set.sapling().get());
        }

        // Phase-3 slime logs (normal + stripped): each gets an axis-aware blockstate built
        // from logBlock(), which emits two oriented variants (y, x, z) so logs face the right
        // way under the player's placement gesture. Two textures per log — "side" (matches the
        // registered name) and "_top".
        for (DeferredBlock<RotatedPillarBlock> holder : WorldBlocks.ALL_LOGS) {
            registerLog(holder.get());
        }

        // SMTCON-95: pattern chest renders as a cube_bottom_top with a dedicated top sprite
        // (chest lid) and a side sprite (chest body wood). The legacy 1.12 mod ships a separate
        // "front" texture, but PatternChestBlock has no HORIZONTAL_FACING property in this port
        // — so we collapse the front into the side variant (every face of the body uses
        // pattern_chest_side). Bottom mirrors the top to keep the chest "lidded" on both
        // y-faces, matching the legacy look when placed on a transparent floor.
        registerCubeBottomTop(PatternChestRegistry.PATTERN_CHEST.get(), "block/pattern_chest_side", "block/pattern_chest_top", "block/pattern_chest_top");

        // SMTCON-95: the four wood-clad workstations (stencil table, part builder, tool
        // station, tool forge) share the same shape — a dedicated top sprite paired with a
        // shared "table_side" plank sprite on the four sides and the bottom. The legacy mod
        // ships a single table_side.png used by all four tables; copying it once and referring
        // to it from every table model is consistent with the legacy asset layout.
        String tableSide = "block/table_side";
        registerCubeBottomTop(StencilTableRegistry.STENCIL_TABLE.get(), tableSide, "block/stencil_table_top", tableSide);
        registerCubeBottomTop(PartBuilderRegistry.PART_BUILDER.get(), tableSide, "block/part_builder_top", tableSide);
        registerCubeBottomTop(ToolStationRegistry.TOOL_STATION.get(), tableSide, "block/tool_station_top", tableSide);
        registerCubeBottomTop(ToolStationRegistry.TOOL_FORGE.get(), tableSide, "block/tool_forge_top", tableSide);

        // SMTCON-129: the smeltery blocks. Plain seared blocks are cube_all; the seared brick /
        // paver stair and slab variants get the matching shape pointing at their base brick
        // texture. The six component blocks are horizontally directional, so each gets the four
        // facing variants of a cube_all model. The casting table and basin are cube_all for now.
        for (DeferredBlock<? extends Block> holder : SearedBlocks.ALL) {
            Block block = holder.get();
            if (block instanceof StairBlock stairs) {
                registerSearedStairs(stairs);
            }
            else if (block instanceof SlabBlock slab) {
                registerSearedSlab(slab);
            }
            else {
                registerCubeAll(block);
            }
        }
        for (DeferredBlock<? extends Block> holder : SmelteryComponents.ALL) {
            registerHorizontalCube(holder.get());
        }
        for (DeferredBlock<? extends Block> holder : CastingBlocks.ALL) {
            registerCubeAll(holder.get());
        }
    }

    /** Emit a stairs blockstate + model for a seared stair, textured with its base block. */
    private void registerSearedStairs(StairBlock stairs) {
        ResourceLocation texture = searedBaseTexture(stairs, "_stairs");
        models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        stairsBlock(stairs, texture);
    }

    /** Emit a slab blockstate + model for a seared slab, textured with its base block. */
    private void registerSearedSlab(SlabBlock slab) {
        ResourceLocation texture = searedBaseTexture(slab, "_slab");
        models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        ResourceLocation doubleSlabModel = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/" + baseName(slab, "_slab"));
        slabBlock(slab, doubleSlabModel, texture);
    }

    /** The {@code block/<base>} texture of a seared stair/slab, with {@code suffix} stripped off. */
    private static ResourceLocation searedBaseTexture(Block variant, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/" + baseName(variant, suffix));
    }

    /** The registry path of {@code variant} with the trailing {@code suffix} removed. */
    private static String baseName(Block variant, String suffix) {
        String path = BuiltInRegistries.BLOCK.getKey(variant).getPath();
        return path.endsWith(suffix) ? path.substring(0, path.length() - suffix.length()) : path;
    }

    /**
     * Emit a horizontally-directional blockstate for a smeltery component block — the four
     * facing variants of a single {@code cube_all} model so the placed block has a valid
     * variant for every {@code FACING} state.
     */
    private void registerHorizontalCube(Block block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath());
        models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        horizontalBlock(block, models().cubeAll(blockId.getPath(), texture));
    }

    /**
     * Emit an axis-aware blockstate + cube_column model pair for a slime log. Vanilla
     * {@link BlockStateProvider#logBlock} uses two textures: {@code <name>} for the four sides
     * and {@code <name>_top} for the top/bottom caps. Pre-register both so
     * {@link ExistingFileHelper} doesn't fail validation before the PNGs land.
     */
    private void registerLog(RotatedPillarBlock block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation side = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath());
        ResourceLocation top = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath() + "_top");
        models().existingFileHelper.trackGenerated(side, PackType.CLIENT_RESOURCES, ".png", "textures");
        models().existingFileHelper.trackGenerated(top, PackType.CLIENT_RESOURCES, ".png", "textures");
        logBlock(block);
    }

    /**
     * Emit a {@code cross} blockstate + model for the supplied sapling (two crossed flat
     * planes), pre-registering the referenced sprite so {@link ExistingFileHelper} doesn't
     * reject the model JSON. Vanilla's oak sapling uses {@code minecraft:block/cross} as the
     * parent; we mirror that path so the sapling renders as a flat plant rather than a cube.
     */
    private void registerCross(SlimeSaplingBlock block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath());
        models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        simpleBlock(block, models().cross(blockId.getPath(), texture).renderType("cutout"));
    }

    /**
     * Emit a {@code cube_all} blockstate + model for {@code block}, pre-registering the
     * referenced texture as "will exist" so the {@link ExistingFileHelper} validation pass
     * doesn't reject the model JSON. The texture PNGs land in a follow-up task per the
     * SMTCON-45 AC; until then, in-game render falls back to the missing-texture sprite, but
     * the model side is complete and well-formed.
     */
    /**
     * Emit a {@code cube_bottom_top}-shaped blockstate + model for {@code block}, parented to
     * the vanilla {@code minecraft:block/cube_bottom_top} template. The three texture paths are
     * passed as plain strings ({@code "block/<name>"}) and resolved against the mod namespace —
     * each one is pre-registered with the {@link ExistingFileHelper} so validation doesn't
     * reject the model JSON before the texture PNGs land on the test classpath.
     *
     * <p>The block model's name is the block's registry path; the resulting JSON has the
     * standard {@code textures: { side, top, bottom }} keys plus a {@code particle} entry
     * defaulting to the side sprite (matches vanilla furnace/lectern/etc. shape).
     */
    private void registerCubeBottomTop(Block block, String sidePath, String topPath, String bottomPath) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation side = textureRef(blockId.getNamespace(), sidePath);
        ResourceLocation top = textureRef(blockId.getNamespace(), topPath);
        ResourceLocation bottom = textureRef(blockId.getNamespace(), bottomPath);
        // Vanilla textures live in the minecraft namespace; mod-tree textures live in our own.
        // Either way the path string carries the namespace already (e.g. "minecraft:block/oak_planks"
        // or just "block/<name>" for sconstruct), so trackGenerated only fires for our namespace.
        if (SConstruct.MOD_ID.equals(side.getNamespace())) {
            models().existingFileHelper.trackGenerated(side, PackType.CLIENT_RESOURCES, ".png", "textures");
        }
        if (SConstruct.MOD_ID.equals(top.getNamespace())) {
            models().existingFileHelper.trackGenerated(top, PackType.CLIENT_RESOURCES, ".png", "textures");
        }
        if (SConstruct.MOD_ID.equals(bottom.getNamespace())) {
            models().existingFileHelper.trackGenerated(bottom, PackType.CLIENT_RESOURCES, ".png", "textures");
        }
        simpleBlock(block, models().cubeBottomTop(blockId.getPath(), side, bottom, top));
    }

    /**
     * Parse a texture path that may or may not carry an explicit namespace prefix. Plain paths
     * like {@code "block/table_side"} resolve to the supplied {@code defaultNamespace};
     * fully-qualified paths like {@code "minecraft:block/oak_planks"} keep their declared
     * namespace verbatim.
     */
    private static ResourceLocation textureRef(String defaultNamespace, String path) {
        int colon = path.indexOf(':');
        if (colon >= 0) {
            return ResourceLocation.fromNamespaceAndPath(path.substring(0, colon), path.substring(colon + 1));
        }
        return ResourceLocation.fromNamespaceAndPath(defaultNamespace, path);
    }

    private void registerCubeAll(Block block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath());
        models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        simpleBlock(block);
    }
}
