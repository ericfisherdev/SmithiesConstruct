package slimeknights.sconstruct.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Loot-table data provider for sconstruct content. Wires per-pulse {@link LootTableProvider.SubProviderEntry}
 * rows so each pulse owns its own {@link net.minecraft.data.loot.LootTableSubProvider} class
 * rather than centralising every block-drop here.
 *
 * <p>Phase 2: the shared pulse contributes {@link SharedBlockLoot} for the metal storage
 * blocks and decoratives. Phase 3: {@link WorldBlockLoot} drops the world-pulse blocks,
 * {@link SlimeMobLoot} drops the slime mob entities, and {@link SlimeIslandChestLoot} fills the
 * slime-island treasure chest. Later pulses (Phase 6 smeltery drops, etc.) append their own
 * entries to the constructor's list.
 */
public final class TinkerLootProvider extends LootTableProvider {

    public TinkerLootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(new LootTableProvider.SubProviderEntry(SharedBlockLoot::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(WorldBlockLoot::new, LootContextParamSets.BLOCK),
                // SMTCON-90: pattern chest drop-self loot table. Chest contents are
                // dropped by PatternChestBlock#onRemove via Containers.dropContents,
                // not by the loot table — keeping the responsibilities separate avoids
                // a double-drop on break.
                new LootTableProvider.SubProviderEntry(ToolBlockLoot::new, LootContextParamSets.BLOCK),
                // SMTCON-143: Phase-6 gadget block drop-self loot tables. The drying
                // rack's held item and the wooden hopper's container contents are
                // dropped by their block classes, not the loot table — see GadgetBlockLoot.
                new LootTableProvider.SubProviderEntry(GadgetBlockLoot::new, LootContextParamSets.BLOCK), new LootTableProvider.SubProviderEntry(SlimeMobLoot::new, LootContextParamSets.ENTITY),
                new LootTableProvider.SubProviderEntry(SlimeIslandChestLoot::new, LootContextParamSets.CHEST)), registries);
    }
}
