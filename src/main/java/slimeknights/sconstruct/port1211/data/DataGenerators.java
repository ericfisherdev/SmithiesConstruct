package slimeknights.sconstruct.port1211.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

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
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        boolean server = event.includeServer();
        boolean client = event.includeClient();

        DataProvider.Factory<TinkerBlockTagsProvider> blockTagsFactory = out -> new TinkerBlockTagsProvider(out, registries, existingFileHelper);
        TinkerBlockTagsProvider blockTags = generator.addProvider(server, blockTagsFactory);

        CompletableFuture<net.minecraft.data.tags.TagsProvider.TagLookup<net.minecraft.world.level.block.Block>> blockTagLookup = blockTags.contentsGetter();

        DataProvider.Factory<TinkerItemTagsProvider> itemTagsFactory = out -> new TinkerItemTagsProvider(out, registries, blockTagLookup, existingFileHelper);
        generator.addProvider(server, itemTagsFactory);

        DataProvider.Factory<TinkerFluidTagsProvider> fluidTagsFactory = out -> new TinkerFluidTagsProvider(out, registries, existingFileHelper);
        generator.addProvider(server, fluidTagsFactory);

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
