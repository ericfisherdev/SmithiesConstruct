package slimeknights.sconstruct.data;

import java.util.Collections;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.sconstruct.gadgets.GadgetBlocks;

/**
 * Block loot-table sub-provider for the Phase-6 gadgets pulse (SMTCON-143). Every gadget block —
 * drying rack, wooden hopper, stone ladder, dried clay, dried clay brick — drops itself on
 * break, the plain {@code dropSelf} behaviour.
 *
 * <p>{@code dropSelf} is the right helper: no gadget block has a fortune-aware or silk-touch-only
 * drop. The drying rack's held item is dropped separately by
 * {@link slimeknights.sconstruct.gadgets.block.DryingRackBlock#onRemove} via
 * {@code Block.popResource}; the wooden hopper's container contents are dropped by the inherited
 * {@code HopperBlock} machinery. So the loot table only owns the placed block itself — a
 * "preserve inventory" loot variant would double-drop those contents.
 *
 * <p>Mirrors {@link SharedBlockLoot} / {@link ToolBlockLoot} / {@link WorldBlockLoot} exactly:
 * a single {@link #ownedBlocks} list feeds both {@link #generate()} and {@link #getKnownBlocks()}
 * so the parent's validation pass cannot drift. Wired into {@link TinkerLootProvider} via a
 * {@code LootTableProvider.SubProviderEntry} row.
 */
public final class GadgetBlockLoot extends BlockLootSubProvider {

    /**
     * The blocks this sub-provider owns — every gadget block in {@link GadgetBlocks#ALL},
     * resolved once at construction. Iterated by both {@link #generate} (to call
     * {@code dropSelf}) and {@link #getKnownBlocks} (the parent's validation pass), so the two
     * paths share one list and cannot drift.
     */
    private final List<Block> ownedBlocks;

    public GadgetBlockLoot(HolderLookup.Provider registries) {
        // Empty explosion-resistant set + REGISTRY.allFlags() — same baseline as the sibling
        // sub-providers: no gadget block is explosion-resistant in a way that changes its drop,
        // and every drop table is unconditionally emitted.
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(), registries);
        this.ownedBlocks = GadgetBlocks.ALL.stream().map(DeferredBlock::get).map(Block.class::cast).toList();
    }

    @Override
    protected void generate() {
        for (Block block : ownedBlocks) {
            dropSelf(block);
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ownedBlocks;
    }
}
