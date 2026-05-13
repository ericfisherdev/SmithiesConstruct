package slimeknights.tconstruct.port1211.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.tconstruct.port1211.TConstruct;
import slimeknights.tconstruct.port1211.shared.SharedBlocks;

/**
 * Blockstate-and-model data provider. Emits one {@code variants}-style blockstate JSON and
 * matching {@code cube_all} model per shared-pulse block. {@link #simpleBlock(Block)} is the
 * right helper for every block this provider owns: metal storage and the wood decoratives
 * are full opaque cubes with a single all-faces texture; the glow block is also a single-
 * texture cube here (the cutout/transparent layer + per-face FACING orientation arrive in a
 * later rendering task per the SMTCON-37 AC).
 *
 * <p>Texture references default to {@code tconstruct:block/<block_path>} — e.g.
 * {@code tconstruct:block/block_cobalt}. The texture PNGs land in a follow-up task; until
 * then, the in-game render uses the missing-texture sprite, but the JSON model side is
 * complete and well-formed so vanilla won't log "missing model" warnings.
 */
public final class TinkerBlockStateProvider extends BlockStateProvider {

    public TinkerBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, TConstruct.MOD_ID, existingFileHelper);
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
