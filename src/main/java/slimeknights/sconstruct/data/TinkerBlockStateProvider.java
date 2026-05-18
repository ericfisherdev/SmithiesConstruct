package slimeknights.sconstruct.data;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.gadgets.GadgetBlocks;
import slimeknights.sconstruct.gadgets.block.DryingRackBlock;
import slimeknights.sconstruct.shared.SharedBlocks;
import slimeknights.sconstruct.smeltery.CastingBlocks;
import slimeknights.sconstruct.smeltery.SearedBlocks;
import slimeknights.sconstruct.smeltery.SmelteryComponents;
import slimeknights.sconstruct.tools.PartBuilderRegistry;
import slimeknights.sconstruct.tools.PatternChestRegistry;
import slimeknights.sconstruct.tools.StencilTableRegistry;
import slimeknights.sconstruct.tools.ToolStationRegistry;
import slimeknights.sconstruct.world.WorldBlocks;
import slimeknights.sconstruct.world.block.SlimePlantSet;
import slimeknights.sconstruct.world.block.SlimeSaplingBlock;

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

        // SMTCON-204: the four wood-clad workstations (stencil table, part builder, tool
        // station, tool forge) render as legged tables — a 4px tabletop plate on four legs with
        // an open space between them — parented to the ported sconstruct:block/table model. Each
        // table keeps its dedicated top sprite and the shared "table_side" plank sprite on the
        // tabletop apron; the leg / legBottom / bottom slots reproduce the legacy 1.12 textures:
        // stencil table on oak planks, part builder on oak logs, tool station on oak planks with
        // table_side legs, and the tool forge clad entirely in iron blocks.
        String tableSide = "block/table_side";
        String oakPlanks = "minecraft:block/oak_planks";
        String oakLog = "minecraft:block/oak_log";
        String ironBlock = "minecraft:block/iron_block";
        registerLeggedTable(StencilTableRegistry.STENCIL_TABLE.get(), "block/stencil_table_top", tableSide, oakPlanks, oakPlanks, oakPlanks);
        registerLeggedTable(PartBuilderRegistry.PART_BUILDER.get(), "block/part_builder_top", tableSide, oakLog, oakLog, oakLog);
        registerLeggedTable(ToolStationRegistry.TOOL_STATION.get(), "block/tool_station_top", tableSide, tableSide, oakPlanks, oakPlanks);
        registerLeggedTable(ToolStationRegistry.TOOL_FORGE.get(), "block/tool_forge_top", ironBlock, ironBlock, ironBlock, ironBlock);

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

        // SMTCON-143: Phase-6 gadget blocks.
        //   - dried clay + dried clay brick are plain full cubes — cube_all.
        //   - the drying rack swaps its model with the DRYING_STATE property (empty/drying/done);
        //     each state gets a cube_all model so every state has a valid variant.
        //   - the stone ladder mirrors the vanilla ladder: a "ladder"-shaped model rotated by
        //     the FACING property.
        //   - the wooden hopper mirrors the vanilla hopper blockstate (FACING down + 4 horizontal).
        registerCubeAll(GadgetBlocks.DRIED_CLAY.get());
        registerCubeAll(GadgetBlocks.DRIED_CLAY_BRICK.get());
        registerDryingRack(GadgetBlocks.DRYING_RACK.get());
        registerStoneLadder(GadgetBlocks.STONE_LADDER.get());
        registerWoodenHopper(GadgetBlocks.WOODEN_HOPPER.get());
    }

    /**
     * Emit the drying-rack blockstate — one {@code cube_all}-style model per {@link DryingState}
     * value so every {@code DRYING_STATE} the block can hold has a valid variant. Each state
     * model points at its own sprite ({@code block/drying_rack_<state>}) so the rack can visibly
     * change as its contents dry; the PNGs land in SMTCON-144.
     */
    private void registerDryingRack(Block block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        getVariantBuilder(block).forAllStates(state -> {
            String stateName = state.getValue(DryingRackBlock.DRYING_STATE).getSerializedName();
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath() + "_" + stateName);
            models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
            return ConfiguredModel.builder().modelFile(models().cubeAll(blockId.getPath() + "_" + stateName, texture)).build();
        });
    }

    /**
     * Emit the stone-ladder blockstate — a single {@code minecraft:block/ladder}-parented model
     * textured with {@code block/stone_ladder}, rotated to face the player's chosen wall by the
     * {@code FACING} property (north 0°, south 180°, west 270°, east 90°). Mirrors the vanilla
     * ladder blockstate exactly.
     */
    private void registerStoneLadder(Block block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath());
        models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        // Parent the vanilla ladder model so the rung geometry + cutout render type come for
        // free; only the "texture" sprite differs from a wooden ladder.
        BlockModelBuilder model = models().withExistingParent(blockId.getPath(), ResourceLocation.parse("block/ladder")).texture("texture", texture).texture("particle", texture);
        horizontalBlock(block, model);
    }

    /**
     * Emit the wooden-hopper blockstate — the vanilla hopper shape mapped over the hopper's
     * {@code FACING} property. Two models parent the vanilla hopper templates:
     * {@code minecraft:block/hopper} for the down-facing default and
     * {@code minecraft:block/hopper_side} for the four horizontal facings, each rotated to match.
     * Both are textured with the three vanilla hopper sprites so the wooden hopper renders as a
     * valid hopper until its own sprites land in SMTCON-144.
     */
    private void registerWoodenHopper(Block block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation top = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath() + "_top");
        ResourceLocation outside = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath() + "_outside");
        ResourceLocation inside = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath() + "_inside");
        models().existingFileHelper.trackGenerated(top, PackType.CLIENT_RESOURCES, ".png", "textures");
        models().existingFileHelper.trackGenerated(outside, PackType.CLIENT_RESOURCES, ".png", "textures");
        models().existingFileHelper.trackGenerated(inside, PackType.CLIENT_RESOURCES, ".png", "textures");
        BlockModelBuilder down = models().withExistingParent(blockId.getPath(), ResourceLocation.parse("block/hopper")).texture("top", top).texture("side", outside).texture("inside", inside)
                .texture("particle", outside);
        BlockModelBuilder side = models().withExistingParent(blockId.getPath() + "_side", ResourceLocation.parse("block/hopper_side")).texture("top", top).texture("side", outside)
                .texture("inside", inside).texture("particle", outside);
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(HopperBlock.FACING);
            if (facing == Direction.DOWN) {
                return ConfiguredModel.builder().modelFile(down).build();
            }
            // hopper_side faces north by default; getOpposite().toYRot() maps each facing to the
            // vanilla hopper's rotation (north=0, east=90, south=180, west=270) — plain toYRot()
            // would spin every side a half-turn the wrong way.
            return ConfiguredModel.builder().modelFile(side).rotationY((int) facing.getOpposite().toYRot()).build();
        });
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
        // or just "block/<name>" for sconstruct), so tracking only fires for our namespace.
        trackIfModTexture(side);
        trackIfModTexture(top);
        trackIfModTexture(bottom);
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

    /**
     * Emit a legged-table blockstate + model for {@code block}, parented to the ported
     * {@code sconstruct:block/table} template (a 4px tabletop plate on four legs). The five
     * texture paths fill the parent's {@code top / side / leg / legBottom / bottom} slots; the
     * {@code particle} slot defaults to the top sprite. Each path may carry an explicit
     * namespace ({@code "minecraft:block/oak_planks"}) or default to the mod namespace
     * ({@code "block/table_side"}); mod-namespace textures are pre-registered with the
     * {@link ExistingFileHelper} so model validation passes before the PNGs land.
     */
    private void registerLeggedTable(Block block, String topPath, String sidePath, String legPath, String legBottomPath, String bottomPath) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation top = textureRef(blockId.getNamespace(), topPath);
        ResourceLocation side = textureRef(blockId.getNamespace(), sidePath);
        ResourceLocation leg = textureRef(blockId.getNamespace(), legPath);
        ResourceLocation legBottom = textureRef(blockId.getNamespace(), legBottomPath);
        ResourceLocation bottom = textureRef(blockId.getNamespace(), bottomPath);
        trackIfModTexture(top);
        trackIfModTexture(side);
        trackIfModTexture(leg);
        trackIfModTexture(legBottom);
        trackIfModTexture(bottom);
        BlockModelBuilder model = models().withExistingParent(blockId.getPath(), ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/table")).texture("particle", top).texture("top", top)
                .texture("side", side).texture("leg", leg).texture("legBottom", legBottom).texture("bottom", bottom);
        simpleBlock(block, model);
    }

    /**
     * Pre-register {@code texture} with the {@link ExistingFileHelper} as a texture PNG that
     * will exist, but only when it lives in the mod's own namespace — vanilla textures are
     * already on the classpath and need no tracking.
     */
    private void trackIfModTexture(ResourceLocation texture) {
        if (SConstruct.MOD_ID.equals(texture.getNamespace())) {
            models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        }
    }

    private void registerCubeAll(Block block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath());
        models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        simpleBlock(block);
    }
}
