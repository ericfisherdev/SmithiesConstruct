package slimeknights.sconstruct.port1211.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.TinkerTags;

/**
 * Phase-3 biome-tag data provider. Owns {@link TinkerTags.Biomes#SLIME_ISLANDS} — the gate tag
 * that SMTCON-59's slime-island {@link net.minecraft.world.level.levelgen.structure.Structure}
 * and {@link net.neoforged.neoforge.common.world.BiomeModifiers.AddSpawnsBiomeModifier} are
 * staged to migrate onto once a curated biome list lands (per the original ticket's open
 * question 12). Today the tag aliases the vanilla {@link BiomeTags#IS_OVERWORLD} surface set
 * via {@code addTag} so the membership is identical to the broad targeting both providers
 * already use — letting consumers reference {@code sconstruct:slime_islands} now and have the
 * scope tighten under their feet without an interface change later.
 *
 * <p>Named {@code WorldBiomeTagsProvider} (no {@code Tinker} prefix) to match the new
 * convention in CLAUDE.md — existing {@code TinkerBlockTagsProvider} / {@code …Item…} /
 * {@code …Fluid…} keep their names under the carve-out for already-shipped identifiers, but
 * new providers added here forward.
 */
public final class WorldBiomeTagsProvider extends TagsProvider<Biome> {

    public WorldBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, net.minecraft.core.registries.Registries.BIOME, lookupProvider, SConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(TinkerTags.Biomes.SLIME_ISLANDS).addTag(BiomeTags.IS_OVERWORLD);
    }

    @Override
    public String getName() {
        return "Biome Tags: " + SConstruct.MOD_ID;
    }
}
