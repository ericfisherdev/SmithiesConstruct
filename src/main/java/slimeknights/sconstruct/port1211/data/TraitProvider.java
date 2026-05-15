package slimeknights.sconstruct.port1211.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonObject;

import slimeknights.sconstruct.port1211.tools.trait.Trait;
import slimeknights.sconstruct.port1211.tools.trait.TraitRegistry;

/**
 * Datagen provider that emits one JSON file per registered {@link Trait} under
 * {@code data/<namespace>/trait/<path>.json}. The JSON shape is intentionally minimal — only
 * the trait id is serialised — since the SMTCON-103 trait registry is still code-side; a
 * future datapack ticket can extend the JSON without changing the file layout.
 *
 * <p>Hooked into the data generator at {@code GatherDataEvent} alongside the other
 * {@link DataProvider}s; the provider runs in parallel and signals completion via the returned
 * {@link CompletableFuture}.
 */
public class TraitProvider implements DataProvider {

    private final PackOutput.PathProvider pathProvider;

    public TraitProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "trait");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(TraitRegistry.all().stream().map(trait -> writeTrait(cache, trait)).toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> writeTrait(CachedOutput cache, Trait trait) {
        ResourceLocation id = trait.id();
        JsonObject json = new JsonObject();
        // Minimal payload — the id round-trips so a datapack rejection of a missing entry is
        // distinguishable from a malformed entry. Future tickets extend this shape.
        json.addProperty("id", id.toString());
        // Surface a static stat-impact descriptor so a datapack consumer (JEI, tooltip cache)
        // can preview the trait's contribution without instantiating the code-side singleton.
        // Emit as a JSON object (not a string) so consumers see a nested node, not a quoted "{}".
        json.add("static_stats", new JsonObject());
        return DataProvider.saveStable(cache, json, pathProvider.json(id));
    }

    @Override
    public String getName() {
        return "Smithies' Construct Traits";
    }
}
