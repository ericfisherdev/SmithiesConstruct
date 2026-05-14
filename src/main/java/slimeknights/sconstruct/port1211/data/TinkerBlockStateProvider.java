package slimeknights.sconstruct.port1211.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.shared.SharedBlocks;
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
    private void registerCubeAll(Block block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath());
        models().existingFileHelper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        simpleBlock(block);
    }
}
