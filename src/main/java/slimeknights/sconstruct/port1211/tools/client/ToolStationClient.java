package slimeknights.sconstruct.port1211.tools.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import slimeknights.sconstruct.port1211.tools.ToolStationRegistry;
import slimeknights.sconstruct.port1211.tools.inventory.client.ToolStationScreen;

/**
 * Client-side wiring for the Tool Station and Tool Forge (SMTCON-93). Subscribes a
 * {@link RegisterMenuScreensEvent} listener that pairs both registry menu types with the
 * shared {@link ToolStationScreen}.
 *
 * <p>{@link #register(IEventBus)} is invoked from {@code SConstruct} under a {@code Dist.CLIENT}
 * guard so this class never resolves on a dedicated server.
 */
public final class ToolStationClient {

    private ToolStationClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ToolStationClient::onRegisterMenuScreens);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ToolStationRegistry.TOOL_STATION_MENU.get(), ToolStationScreen::new);
        event.register(ToolStationRegistry.TOOL_FORGE_MENU.get(), ToolStationScreen::new);
    }
}
