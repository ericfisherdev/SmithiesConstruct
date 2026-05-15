package slimeknights.sconstruct.port1211.tools.modifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Code-side façade over the {@code sconstruct:modifier} datapack registry. Mirrors
 * {@link slimeknights.sconstruct.port1211.tools.material.MaterialRegistry} — the modifier
 * registry has the same shape as the material registry (datapack-driven, server-authoritative,
 * cached on every reload via {@link OnDatapackSyncEvent}). The split keeps the modifier
 * lifecycle decoupled from materials: a datapack ship can add modifiers without re-syncing
 * materials, and the per-registry cache lets the SMTCON-83 hooks dispatcher resolve modifiers
 * off the same atomic snapshot for every tick.
 */
public final class ModifierRegistry {

    /** Server-side modifier cache. Replaced atomically on each {@link OnDatapackSyncEvent}. */
    private static final AtomicReference<Map<ResourceLocation, Holder<Modifier>>> CACHE = new AtomicReference<>(Map.of());

    private ModifierRegistry() {
    }

    /**
     * Attach both lifecycle listeners. {@link DataPackRegistryEvent.NewRegistry} goes on the
     * supplied mod bus; {@link OnDatapackSyncEvent} goes on the NeoForge event bus.
     */
    public static void register(IEventBus modBus, IEventBus neoForgeBus) {
        modBus.addListener(ModifierRegistry::onNewRegistry);
        neoForgeBus.addListener(ModifierRegistry::onDatapackSync);
    }

    private static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(Modifier.REGISTRY_KEY, Modifier.DIRECT_CODEC, Modifier.DIRECT_CODEC);
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        // Same one-snapshot-per-reload contract as MaterialRegistry — the event fires before
        // every client sync; we snapshot once and let later same-tick fires overwrite with the
        // same payload.
        HolderLookup.RegistryLookup<Modifier> lookup = event.getPlayerList().getServer().registryAccess().lookupOrThrow(Modifier.REGISTRY_KEY);
        Map<ResourceLocation, Holder<Modifier>> snapshot = new HashMap<>();
        lookup.listElements().forEach(holder -> snapshot.put(holder.key().location(), holder));
        CACHE.set(Collections.unmodifiableMap(snapshot));
    }

    /** Cached {@link Holder} for the supplied modifier id, or {@link Optional#empty} if no
     *  modifier with that id is currently registered. Cache reflects the most recent reload. */
    public static Optional<Holder<Modifier>> get(ResourceLocation id) {
        return Optional.ofNullable(CACHE.get().get(id));
    }

    /** Convenience: unwrap the {@link Holder} and return the {@link Modifier} value directly. */
    public static Optional<Modifier> lookup(ResourceLocation id) {
        return get(id).map(Holder::value);
    }

    /** Test seam: replace the cache atomically. */
    static void overwriteCacheForTest(Map<ResourceLocation, Holder<Modifier>> values) {
        CACHE.set(Collections.unmodifiableMap(new HashMap<>(values)));
    }

    /** Test seam: current size of the cache. */
    static int cacheSize() {
        return CACHE.get().size();
    }

    /** Test seam: clear the cache. */
    static void clearCacheForTest() {
        CACHE.set(Map.of());
    }
}
