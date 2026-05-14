package slimeknights.sconstruct.port1211.tools.material;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Code-side façade over the {@code sconstruct:material} datapack registry. Owns two
 * lifecycle hooks:
 *
 * <ul>
 *   <li>{@link #onNewRegistry(DataPackRegistryEvent.NewRegistry)} — registers the registry on
 *       the mod bus during {@code DataPackRegistryEvent.NewRegistry}. Same {@link
 *       Material#DIRECT_CODEC} is used for both the persistence and the network codec slots —
 *       materials are server-authoritative content, and the network codec is only consulted
 *       when the registry is sent to a connecting client.</li>
 *   <li>{@link #onDatapackSync(OnDatapackSyncEvent)} — populates a server-side cache from
 *       {@link OnDatapackSyncEvent#getPlayerList} every time {@code /reload} runs (event also
 *       fires per-player on login; the per-server snapshot is rebuilt on the first sync of
 *       each reload). The cache is the fast path for code-side material lookups that don't
 *       want to round-trip through the {@code RegistryAccess} dispatch.</li>
 * </ul>
 *
 * <p>The cache is {@link Collections#unmodifiableMap}-wrapped and intentionally not exposed —
 * callers go through {@link #get(ResourceLocation)} or {@link #lookup(ResourceLocation)} so a
 * later reload swap can replace the underlying map atomically without leaking the stale
 * reference.
 */
public final class MaterialRegistry {

    /** Server-side material cache. Replaced atomically on each {@link OnDatapackSyncEvent}. */
    private static volatile Map<ResourceLocation, Holder<Material>> cache = Map.of();

    private MaterialRegistry() {
    }

    /**
     * Attach both lifecycle listeners. The {@link DataPackRegistryEvent.NewRegistry} listener
     * goes on the supplied mod bus; the {@link OnDatapackSyncEvent} listener goes on the
     * NeoForge event bus directly because that's where data-pack sync fires.
     */
    public static void register(IEventBus modBus, IEventBus neoForgeBus) {
        modBus.addListener(MaterialRegistry::onNewRegistry);
        neoForgeBus.addListener(MaterialRegistry::onDatapackSync);
    }

    @SubscribeEvent
    private static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(Material.REGISTRY_KEY, Material.DIRECT_CODEC, Material.DIRECT_CODEC);
    }

    @SubscribeEvent
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        // Build the cache once per /reload by reading from the server's RegistryAccess. The
        // event fires before every client sync (per player on login + on /reload for every
        // connected player), but the underlying server registry only changes on /reload — so
        // we snapshot whenever the event fires and let later same-tick fires write the same
        // snapshot. Cheap, idempotent, no synchronisation needed beyond the volatile field.
        MinecraftServer server = event.getPlayerList().getServer();
        HolderLookup.RegistryLookup<Material> lookup = server.registryAccess().lookupOrThrow(Material.REGISTRY_KEY);
        Map<ResourceLocation, Holder<Material>> snapshot = new HashMap<>();
        lookup.listElements().forEach(holder -> snapshot.put(holder.key().location(), holder));
        cache = Collections.unmodifiableMap(snapshot);
    }

    /**
     * Returns the cached {@link Holder} for the supplied material id, or {@link Optional#empty}
     * if no material with that id is currently registered. Cache reflects the most recent
     * {@code /reload}.
     */
    public static Optional<Holder<Material>> get(ResourceLocation id) {
        return Optional.ofNullable(cache.get(id));
    }

    /**
     * Convenience: unwrap the {@link Holder} and return the {@link Material} value directly,
     * or empty if the id is missing. Equivalent to {@code get(id).map(Holder::value)}.
     */
    public static Optional<Material> lookup(ResourceLocation id) {
        return get(id).map(Holder::value);
    }

    /** Test seam: replace the cache atomically. Visible-for-test only. */
    static void overwriteCacheForTest(Map<ResourceLocation, Holder<Material>> values) {
        cache = Collections.unmodifiableMap(new HashMap<>(values));
    }

    /** Test seam: current size of the cache. */
    static int cacheSize() {
        return cache.size();
    }

    /** Test seam: clear the cache (simulates a server shutdown). */
    static void clearCacheForTest() {
        cache = Map.of();
    }
}
