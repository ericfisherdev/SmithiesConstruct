package slimeknights.tconstruct.port1211.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Loot-table data provider for tconstruct content. Wires per-pulse {@link LootTableProvider.SubProviderEntry}
 * rows so each pulse owns its own {@link net.minecraft.data.loot.LootTableSubProvider} class
 * rather than centralising every block-drop here.
 *
 * <p>Phase 2: the shared pulse contributes {@link SharedBlockLoot} for the metal storage
 * blocks and decoratives. Later pulses (Phase 3 slime islands' chest loot, Phase 6 smeltery
 * drops, etc.) append their own entries to the constructor's list.
 */
public final class TinkerLootProvider extends LootTableProvider {

    public TinkerLootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(new LootTableProvider.SubProviderEntry(SharedBlockLoot::new, LootContextParamSets.BLOCK)), registries);
    }
}
