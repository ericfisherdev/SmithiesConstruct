package slimeknights.sconstruct.port1211.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import slimeknights.sconstruct.port1211.world.WorldBlocks;

/**
 * Block loot-table sub-provider for the world pulse. Every coloured slime block drops itself
 * via {@code dropSelf} — vanilla slime-block precedent, and the AC explicitly calls for
 * silk-touch-free self-drop so a magma-block landing doesn't cost the player their bouncy
 * compressed slime in exchange for fire damage.
 *
 * <p>Wired into {@link TinkerLootProvider} via a {@code LootTableProvider.SubProviderEntry}
 * row so the data generator runs it during {@code ./gradlew runData}. Mirrors
 * {@link SharedBlockLoot}'s structure exactly; lifting the {@code ownedBlocks} helper is
 * deliberate so the two providers can't drift in shape.
 */
public final class WorldBlockLoot extends BlockLootSubProvider {

    public WorldBlockLoot(HolderLookup.Provider registries) {
        // Empty explosion-resistant set + every feature flag enabled, matching SharedBlockLoot.
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (Block block : ownedBlocks()) {
            dropSelf(block);
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // Same iterable as generate() — any drift in shape between the two paths would trip
        // BlockLootSubProvider's validation pass with a clear runData error rather than silently
        // omit a loot table.
        return ownedBlocks();
    }

    /**
     * Single source of truth for the blocks this sub-provider owns. Iterated by both
     * {@link #generate} (to call {@code dropSelf}) and {@link #getKnownBlocks} (to satisfy the
     * parent's validation pass). Currently just the four slime blocks; future Phase-3 tasks
     * (slime dirt, leaves, sapling) extend this list.
     */
    private List<Block> ownedBlocks() {
        List<Block> blocks = new ArrayList<>();
        WorldBlocks.ALL.forEach(holder -> blocks.add(holder.get()));
        // Plant set: dirt + grass + leaves + sapling all drop themselves. Vanilla leaves/grass
        // have richer drop tables (sapling chance, dirt-fallback without shears) — those are a
        // follow-up; the AC for SMTCON-54 just requires a loot table exists per block.
        WorldBlocks.ALL_PLANTS.forEach(holder -> blocks.add(holder.get()));
        // Slime logs (normal + stripped): drop self, matching vanilla oak_log loot. Per the
        // SMTCON-55 AC, the stripped/non-stripped distinction is purely an item-id swap when
        // an axe interacts with a placed log — both variants self-drop on break.
        WorldBlocks.ALL_LOGS.forEach(holder -> blocks.add(holder.get()));
        return blocks;
    }
}
