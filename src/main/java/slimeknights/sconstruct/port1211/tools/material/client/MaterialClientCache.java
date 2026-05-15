package slimeknights.sconstruct.port1211.tools.material.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import slimeknights.sconstruct.port1211.tools.material.Material;

/**
 * Client-side mirror of the {@code sconstruct:material} datapack registry's colour field. Holds
 * a {@code Map<ResourceLocation, Integer>} of material id → packed ARGB tint so the per-frame
 * {@code ItemColors} handler can resolve a material's tint without descending through
 * {@link HolderLookup.RegistryLookup} on every lookup. Updated from the live client-side
 * registry when {@link #populateFrom(HolderLookup.RegistryLookup)} is invoked — typically on
 * {@code ClientPlayerNetworkEvent.LoggingIn}, after configuration-phase registry sync.
 *
 * <p>The cache uses an {@link AtomicReference} so the snapshot reference is swapped atomically
 * on each refresh — mirrors the server-side {@code MaterialRegistry} pattern and avoids a
 * {@code synchronized} block on the read path.
 *
 * <p>{@link #getColor(ResourceLocation)} returns {@link #DEFAULT_COLOR} when the cache has no
 * entry for the supplied id; the renderer treats {@code 0xFFFFFFFF} as "no tint" so a missing
 * material entry renders as a plain white sprite rather than crashing the client (acceptance
 * criterion).
 */
public final class MaterialClientCache {

    /** Returned by {@link #getColor(ResourceLocation)} when no entry exists for the id. */
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;

    private static final AtomicReference<Map<ResourceLocation, Integer>> CACHE = new AtomicReference<>(Map.of());

    private MaterialClientCache() {
    }

    /**
     * Look up the cached tint colour for the supplied material id. Returns
     * {@link #DEFAULT_COLOR} when no entry is present — both before the first sync and for
     * material ids that the server-side registry does not contain.
     */
    public static int getColor(ResourceLocation materialId) {
        Integer cached = CACHE.get().get(materialId);
        return cached != null ? cached : DEFAULT_COLOR;
    }

    /**
     * Replace the cache snapshot wholesale. Copies the supplied map so the caller may continue
     * mutating its source without aliasing the cache contents.
     */
    public static void populate(Map<ResourceLocation, Integer> snapshot) {
        CACHE.set(Map.copyOf(snapshot));
    }

    /**
     * Walk the supplied client-side {@link HolderLookup.RegistryLookup} for {@link Material}
     * and rebuild the cache from the {@code id → color} pairs it advertises. Intended for the
     * {@code ClientPlayerNetworkEvent.LoggingIn} entrypoint where the connection's
     * {@code registryAccess()} has just finished syncing the server registry.
     */
    public static void populateFrom(HolderLookup.RegistryLookup<Material> lookup) {
        Map<ResourceLocation, Integer> snapshot = new HashMap<>();
        lookup.listElements().forEach(holder -> snapshot.put(holder.key().location(), holder.value().color()));
        populate(snapshot);
    }

    /** Test seam: current entry count. */
    static int cacheSize() {
        return CACHE.get().size();
    }

    /** Test seam: clear the cache (simulates a fresh client). */
    static void clearForTest() {
        CACHE.set(Map.of());
    }
}
