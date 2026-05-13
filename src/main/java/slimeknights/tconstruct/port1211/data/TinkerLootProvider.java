package slimeknights.tconstruct.port1211.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;

/**
 * Phase-1 stub for tconstruct loot tables. Constructs a {@link LootTableProvider} with an empty
 * set of expected tables and an empty list of sub-provider entries — Phase 2+ pulses (slime
 * islands' chest loot, tool-station drops, etc.) append entries to those collections then.
 */
public final class TinkerLootProvider extends LootTableProvider {

    public TinkerLootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(), registries);
    }
}
