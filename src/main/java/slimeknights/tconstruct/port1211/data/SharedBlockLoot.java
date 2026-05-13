package slimeknights.tconstruct.port1211.data;

import java.util.Collections;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import slimeknights.tconstruct.port1211.shared.SharedBlocks;

/**
 * Block loot-table sub-provider for the shared pulse. Every block the shared pulse registers
 * drops itself: the 13 metal storage blocks behave like vanilla {@code iron_block} (pickaxe
 * required, drops the placed item) and the three decorative blocks ({@code glow},
 * {@code firewood}, {@code lavawood}) drop themselves via any mining tool.
 *
 * <p>{@code dropSelf} is the right helper because none of these blocks have a fortune-aware
 * drop table or silk-touch-only behaviour — the AC explicitly calls out that silk-touch is
 * unnecessary because the unmodified behaviour already drops the placed item.
 *
 * <p>This sub-provider is wired into {@link TinkerLootProvider} via a
 * {@code LootTableProvider.SubProviderEntry(SharedBlockLoot::new, LootContextParamSets.BLOCK)}
 * row so the data generator runs it during {@code ./gradlew runData}.
 */
public final class SharedBlockLoot extends BlockLootSubProvider {

    public SharedBlockLoot(HolderLookup.Provider registries) {
        // Empty explosion-resistant set: no shared block is explosion-resistant in a way that
        // changes the drop. enabledFeatures = FeatureFlags.REGISTRY.allFlags() so every drop
        // table is unconditionally emitted (the alternative would gate tables on a vanilla
        // feature flag, which we don't use).
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        // Metal storage blocks + decoratives, sourced from the single ownedBlocks() helper so
        // generate() and getKnownBlocks() can't drift. Metal storage drops itself (vanilla
        // iron_block precedent); glow + firewood + lavawood drop themselves too, matching the
        // legacy 1.12 behaviour and vanilla wood-block convention.
        for (Block block : ownedBlocks()) {
            dropSelf(block);
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // BlockLootSubProvider validates that #generate() produced a table for every block it
        // claims to own. Sharing the same iterable as generate() above guarantees no block is
        // dropped from one path without the other — any drift trips a clear runData error
        // instead of silently leaving a block without a loot table.
        return ownedBlocks();
    }

    /**
     * Single source of truth for the blocks this sub-provider owns. Iterated by both
     * {@link #generate} (to call {@code dropSelf}) and {@link #getKnownBlocks} (to satisfy
     * the parent's validation pass).
     */
    private java.util.List<Block> ownedBlocks() {
        java.util.List<Block> blocks = new java.util.ArrayList<>();
        SharedBlocks.METAL_BLOCKS.values().forEach(holder -> blocks.add(holder.get()));
        blocks.add(SharedBlocks.GLOW.get());
        blocks.add(SharedBlocks.FIREWOOD.get());
        blocks.add(SharedBlocks.LAVAWOOD.get());
        return blocks;
    }
}
