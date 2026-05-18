package slimeknights.sconstruct.tools.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import slimeknights.sconstruct.tools.PartBuilderRegistry;
import slimeknights.sconstruct.tools.inventory.client.PartBuilderScreen;

/**
 * Client-side wiring for the Part Builder (SMTCON-92). Subscribes a
 * {@link RegisterMenuScreensEvent} listener that pairs
 * {@link PartBuilderRegistry#PART_BUILDER_MENU} with {@link PartBuilderScreen}.
 *
 * <p>{@link #register(IEventBus)} is the seam invoked from
 * {@link slimeknights.sconstruct.SConstruct} under a {@code Dist.CLIENT} guard so
 * this class never resolves on a dedicated server. Same shape as {@link StencilTableClient}.
 */
public final class PartBuilderClient {

    private PartBuilderClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PartBuilderClient::onRegisterMenuScreens);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(PartBuilderRegistry.PART_BUILDER_MENU.get(), PartBuilderScreen::new);
    }
}
