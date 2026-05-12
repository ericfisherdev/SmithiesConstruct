package slimeknights.tconstruct.port1211.data;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Mod-bus listener for {@link GatherDataEvent}. Per-pulse providers register themselves
 * here as later phases come online; the empty body proves wiring without producing files.
 */
public final class DataGenerators {
    private DataGenerators() {
    }

    public static void onGather(GatherDataEvent event) {
        // Providers added per phase via event.createProvider / addProvider / createDatapackRegistryObjects.
    }
}
