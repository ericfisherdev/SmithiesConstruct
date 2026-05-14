package slimeknights.sconstruct.port1211.data;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.data.material.TinkerMaterialBootstrap;
import slimeknights.sconstruct.port1211.tools.material.Material;
import slimeknights.sconstruct.port1211.world.WorldFeatures;
import slimeknights.sconstruct.port1211.world.WorldStructures;

/**
 * Mod-bus listener for {@link GatherDataEvent}. Registers the Phase-1 provider stubs so the
 * datagen chain shape is fixed before content arrives — per-pulse providers in Phase 2+ append
 * entries inside the stubs' {@code addTags} / {@code buildRecipes} / etc. hooks rather than
 * altering this wiring.
 *
 * <p>Each factory is bound to a {@link DataProvider.Factory}-typed local before being handed to
 * {@link DataGenerator#addProvider} to disambiguate from the {@code addProvider(boolean, T)}
 * overload — javac can otherwise consider a lambda applicable to both overloads even though
 * {@link DataProvider} is not a functional interface.
 */
public final class DataGenerators {
    private DataGenerators() {
    }

    public static void onGather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        CompletableFuture<HolderLookup.Provider> baseRegistries = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        boolean server = event.includeServer();
        boolean client = event.includeClient();

        // Worldgen entries (ConfiguredFeature + PlacedFeature). DatapackBuiltinEntriesProvider
        // augments the lookup provider so any later provider that needs the new entries (e.g. a
        // future biome modifier referencing slime trees) can resolve them through the augmented
        // provider returned by getRegistryProvider() — bind that augmented future to a fresh
        // effectively-final local because subsequent factory lambdas capture it.
        RegistrySetBuilder registrySetBuilder = new RegistrySetBuilder().add(Registries.CONFIGURED_FEATURE, WorldFeatures::bootstrapConfigured)
                .add(Registries.PLACED_FEATURE, WorldFeatures::bootstrapPlaced).add(Registries.STRUCTURE, WorldStructures::bootstrapStructures)
                .add(Registries.STRUCTURE_SET, WorldStructures::bootstrapStructureSets)
                .add(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS, WorldStructures::bootstrapBiomeModifiers)
                .add(Material.REGISTRY_KEY, TinkerMaterialBootstrap::bootstrap);
        // The provider scope set lists every namespace whose entries this datagen emits.
        // Phase-4 materials live under the {@code tconstruct} namespace for legacy-addon
        // compatibility (see TinkerMaterialBootstrap class javadoc), so both namespaces are
        // declared here.
        DatapackBuiltinEntriesProvider datapackProvider = new DatapackBuiltinEntriesProvider(generator.getPackOutput(), baseRegistries, registrySetBuilder, Set.of(SConstruct.MOD_ID, "tconstruct"));
        generator.addProvider(server, datapackProvider);
        CompletableFuture<HolderLookup.Provider> registries = datapackProvider.getRegistryProvider();

        DataProvider.Factory<TinkerBlockTagsProvider> blockTagsFactory = out -> new TinkerBlockTagsProvider(out, registries, existingFileHelper);
        TinkerBlockTagsProvider blockTags = generator.addProvider(server, blockTagsFactory);

        CompletableFuture<net.minecraft.data.tags.TagsProvider.TagLookup<net.minecraft.world.level.block.Block>> blockTagLookup = blockTags.contentsGetter();

        DataProvider.Factory<TinkerItemTagsProvider> itemTagsFactory = out -> new TinkerItemTagsProvider(out, registries, blockTagLookup, existingFileHelper);
        generator.addProvider(server, itemTagsFactory);

        DataProvider.Factory<TinkerFluidTagsProvider> fluidTagsFactory = out -> new TinkerFluidTagsProvider(out, registries, existingFileHelper);
        generator.addProvider(server, fluidTagsFactory);

        // Phase-3 biome and entity-type tag providers — World* naming forward (the existing
        // Tinker*TagsProvider classes keep their names per the carve-out in CLAUDE.md).
        DataProvider.Factory<WorldBiomeTagsProvider> biomeTagsFactory = out -> new WorldBiomeTagsProvider(out, registries, existingFileHelper);
        generator.addProvider(server, biomeTagsFactory);

        DataProvider.Factory<WorldEntityTagsProvider> entityTagsFactory = out -> new WorldEntityTagsProvider(out, registries, existingFileHelper);
        generator.addProvider(server, entityTagsFactory);

        DataProvider.Factory<TinkerRecipeProvider> recipeFactory = out -> new TinkerRecipeProvider(out, registries);
        generator.addProvider(server, recipeFactory);

        DataProvider.Factory<TinkerLootProvider> lootFactory = out -> new TinkerLootProvider(out, registries);
        generator.addProvider(server, lootFactory);

        DataProvider.Factory<TinkerBlockStateProvider> blockStateFactory = out -> new TinkerBlockStateProvider(out, existingFileHelper);
        generator.addProvider(client, blockStateFactory);

        DataProvider.Factory<TinkerItemModelProvider> itemModelFactory = out -> new TinkerItemModelProvider(out, existingFileHelper);
        generator.addProvider(client, itemModelFactory);

        DataProvider.Factory<TinkerLanguageProvider> languageFactory = TinkerLanguageProvider::new;
        generator.addProvider(client, languageFactory);
    }
}
