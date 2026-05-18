package slimeknights.sconstruct.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.common.TinkerTags;
import slimeknights.sconstruct.world.WorldEntities;

/**
 * Phase-3 entity-type-tag data provider. Owns {@link TinkerTags.Entities#SLIMES} — the
 * mod-namespaced "any sconstruct slime" tag that bundles the blueslime and huge slime entity
 * types. Lets future tooling (achievements, statistics, ambient sound zones) target both with
 * a single tag instead of enumerating the entity types one by one.
 *
 * <p>Named {@code WorldEntityTagsProvider} (no {@code Tinker} prefix) per CLAUDE.md's
 * forward-looking naming rule — pre-existing {@code Tinker*TagsProvider} classes stay under
 * the carve-out for already-shipped identifiers, but every new provider added here uses the
 * {@code World*} or unbranded form.
 */
public final class WorldEntityTagsProvider extends EntityTypeTagsProvider {

    public WorldEntityTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, SConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(TinkerTags.Entities.SLIMES).add(WorldEntities.BLUESLIME.get(), WorldEntities.HUGESLIME.get());
    }
}
