package slimeknights.tconstruct.port1211.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import slimeknights.tconstruct.port1211.TConstruct;

/**
 * Phase-1 stub for tconstruct fluid tags. Empty body; Phase 2+ smeltery work appends molten-metal
 * fluid tags (#molten_metals, #castable_in_smeltery, ...) here.
 */
public final class TinkerFluidTagsProvider extends FluidTagsProvider {

    public TinkerFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, TConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // No fluid tags in Phase 1.
    }
}
