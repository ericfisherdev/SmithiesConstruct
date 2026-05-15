package slimeknights.sconstruct.port1211.tools.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import slimeknights.sconstruct.port1211.tools.StencilTableRegistry;
import slimeknights.sconstruct.port1211.tools.inventory.client.StencilTableScreen;

/**
 * Client-side wiring for the Stencil Table (SMTCON-91). Subscribes a
 * {@link RegisterMenuScreensEvent} listener that pairs
 * {@link StencilTableRegistry#STENCIL_TABLE_MENU} with {@link StencilTableScreen}.
 *
 * <p>{@link #register(IEventBus)} is the seam invoked from
 * {@link slimeknights.sconstruct.port1211.SConstruct} under a {@code Dist.CLIENT} guard so
 * this class never resolves on a dedicated server. Same shape as {@link PatternChestClient}.
 */
public final class StencilTableClient {

    private StencilTableClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(StencilTableClient::onRegisterMenuScreens);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(StencilTableRegistry.STENCIL_TABLE_MENU.get(), StencilTableScreen::new);
    }
}
