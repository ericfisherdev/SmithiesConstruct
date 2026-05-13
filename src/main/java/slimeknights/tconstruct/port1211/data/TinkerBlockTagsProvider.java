package slimeknights.tconstruct.port1211.data;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.tconstruct.port1211.TConstruct;
import slimeknights.tconstruct.port1211.shared.Metal;
import slimeknights.tconstruct.port1211.shared.SharedBlocks;
import slimeknights.tconstruct.port1211.shared.SharedMetals;

/**
 * Block-tag data provider. Writes the {@code minecraft:mineable/*} and {@code minecraft:needs_*_tool}
 * tag JSONs for every tconstruct block so vanilla's tool-tier system enforces the right pickaxe
 * (iron for most metals, diamond for cobalt/ardite/manyullyn) and the wood decoratives respond
 * to axes.
 *
 * <p>The mapping is driven entirely by {@link SharedMetals#ALL}'s {@code needsDiamond} flag —
 * no per-metal table is duplicated here. A new metal lights up the right tags by appending to
 * the driver and {@link SharedBlocks#METAL_BLOCKS}.
 *
 * <p>{@code BlockTagsProvider#tag} returns the same builder on repeat calls, so tagging a block
 * under both {@code MINEABLE_WITH_PICKAXE} and {@code NEEDS_IRON_TOOL} appends one entry to each
 * tag JSON — the per-tag JSON file lists every block tagged into it.
 */
public final class TinkerBlockTagsProvider extends BlockTagsProvider {

    public TinkerBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, TConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        Map<String, Metal> metalsById = indexMetalsById();
        for (Map.Entry<String, DeferredBlock<Block>> entry : SharedBlocks.METAL_BLOCKS.entrySet()) {
            Block block = entry.getValue().get();
            Metal metal = metalsById.get(entry.getKey());
            if (metal == null) {
                // SharedBlocks#metalBlock validates against SharedMetals.ALL at registration
                // time, so today this is unreachable. Pin it anyway so a future refactor that
                // weakens that guard fails datagen with a precise message instead of NPEing.
                throw new IllegalStateException("Block key '" + entry.getKey() + "' in SharedBlocks.METAL_BLOCKS has no matching Metal in SharedMetals.ALL");
            }
            // Every metal block is pickaxe-mined; the difference is what *tier* of pickaxe.
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
            // Iron-tier for real-world metals; diamond-tier for the Nether trio (cobalt, ardite,
            // manyullyn). Membership is mutually exclusive — vanilla treats NEEDS_DIAMOND_TOOL
            // as strictly stricter than NEEDS_IRON_TOOL, so a block tagged into both would still
            // require diamond, but tagging both pollutes the tag JSON without changing behaviour.
            tag(metal.needsDiamond() ? BlockTags.NEEDS_DIAMOND_TOOL : BlockTags.NEEDS_IRON_TOOL).add(block);
        }
        // Decoratives: glow + the two firewood variants are wood-like and respond to axes. No
        // tier requirement — any axe (or hand) can mine them, matching the legacy hardness
        // settings on BlockGlow and BlockFirewood.
        tag(BlockTags.MINEABLE_WITH_AXE).add(SharedBlocks.GLOW.get(), SharedBlocks.FIREWOOD.get(), SharedBlocks.LAVAWOOD.get());
    }

    private static Map<String, Metal> indexMetalsById() {
        java.util.Map<String, Metal> map = new java.util.HashMap<>();
        for (Metal metal : SharedMetals.ALL) {
            map.put(metal.id(), metal);
        }
        return Map.copyOf(map);
    }
}
