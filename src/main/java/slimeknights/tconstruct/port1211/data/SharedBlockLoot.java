package slimeknights.tconstruct.port1211.data;

import java.util.Collections;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

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
        // Metal storage blocks — iterate the live registry view so a new metal lights up the
        // loot table by appending to SharedMetals.ALL alone, no edit here.
        for (DeferredBlock<Block> blockHolder : SharedBlocks.METAL_BLOCKS.values()) {
            dropSelf(blockHolder.get());
        }
        // Decoratives. Glow drops itself; the two wood variants do too — vanilla wood blocks
        // are the precedent, and the legacy 1.12 mod used the same drop behaviour.
        dropSelf(SharedBlocks.GLOW.get());
        dropSelf(SharedBlocks.FIREWOOD.get());
        dropSelf(SharedBlocks.LAVAWOOD.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // BlockLootSubProvider validates that #generate() produced a table for every block it
        // claims to own. Returning the union of metal storage blocks + the three decoratives
        // matches the dropSelf calls above; any drift trips a clear runData error rather
        // than silently leaving a block without a loot table.
        java.util.List<Block> known = new java.util.ArrayList<>();
        SharedBlocks.METAL_BLOCKS.values().forEach(holder -> known.add(holder.get()));
        known.add(SharedBlocks.GLOW.get());
        known.add(SharedBlocks.FIREWOOD.get());
        known.add(SharedBlocks.LAVAWOOD.get());
        return known;
    }
}
