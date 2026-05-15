package slimeknights.sconstruct.port1211.data;

import java.util.Collections;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import slimeknights.sconstruct.port1211.tools.PatternChestRegistry;

/**
 * Block loot-table sub-provider for the tools pulse. SMTCON-90 introduces the first
 * tool-pulse block (the 32-slot pattern chest) — it drops itself on break. Chest contents are
 * dropped separately by
 * {@link slimeknights.sconstruct.port1211.tools.block.PatternChestBlock#onRemove} via
 * {@code Containers.dropContents} so the loot table only owns the placed block; the
 * "preserve inventory" loot-function variant would double-drop the contents when paired with
 * the {@code onRemove} path.
 *
 * <p>Wired into {@link TinkerLootProvider} alongside {@link SharedBlockLoot} /
 * {@link WorldBlockLoot}. Pattern mirrors theirs exactly.
 */
public final class ToolBlockLoot extends BlockLootSubProvider {

    public ToolBlockLoot(HolderLookup.Provider registries) {
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
        return ownedBlocks();
    }

    /**
     * Single source of truth for the blocks this sub-provider owns. Currently just the pattern
     * chest; future tools-pulse blocks (tool station, part builder, stencil table) extend this
     * list.
     */
    private List<Block> ownedBlocks() {
        return List.of(PatternChestRegistry.PATTERN_CHEST.get());
    }
}
