package slimeknights.tconstruct.port1211.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import slimeknights.tconstruct.port1211.TConstruct;

/**
 * Phase-1 stub for tconstruct block tags. Empty body; Phase 2+ pulses append their tag entries
 * inside {@link #addTags}. Registering the stub now means the data generator's provider chain
 * is fixed before content arrives — pulses don't change the chain shape, just its contents.
 */
public final class TinkerBlockTagsProvider extends BlockTagsProvider {

    public TinkerBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, TConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // No block tags in Phase 1 — content arrives in Phase 2+.
    }
}
