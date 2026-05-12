package slimeknights.tconstruct.port1211.lib.util;

import net.minecraft.resources.ResourceLocation;

import slimeknights.tconstruct.port1211.TConstruct;

/**
 * Bag of cross-pulse static helpers — the {@code port1211/lib/util/} replacement for upstream
 * Mantle's {@code slimeknights.mantle.util.LocUtils} and similar one-line helpers. Mantle has
 * no 1.21.x release; the legacy codebase reached for these constantly so we vendor only the
 * smallest possible subset and let later phases append further helpers as call-sites land.
 */
public final class Util {

    private Util() {
    }

    /**
     * Convenience constructor for a {@link ResourceLocation} in the {@link TConstruct#MOD_ID
     * tconstruct} namespace. Equivalent to
     * {@code ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, path)} but reads as
     * {@code Util.rl("tool_station")} at call sites — the legacy code used a similar one-liner
     * thousands of times.
     */
    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, path);
    }
}
