package slimeknights.tconstruct.port1211.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import slimeknights.tconstruct.port1211.TConstruct;

/**
 * Phase-1 stub for tconstruct item tags. Item tags can reference block-tag membership via the
 * {@code copy(BlockTag, ItemTag)} helper, so this provider receives the
 * {@link TagsProvider.TagLookup} from {@link TinkerBlockTagsProvider} at construction time.
 * Empty body; Phase 2+ pulses append entries inside {@link #addTags}.
 */
public final class TinkerItemTagsProvider extends ItemTagsProvider {

    public TinkerItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
            ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, TConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // No item tags in Phase 1 — content arrives in Phase 2+.
    }
}
