package slimeknights.sconstruct.port1211.smeltery.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;
import slimeknights.sconstruct.port1211.smeltery.inventory.client.SmelteryControllerScreen;

/**
 * Client-side wiring for the smeltery GUIs (SMTCON-125). Subscribes a
 * {@link RegisterMenuScreensEvent} listener that pairs the smeltery controller menu type with
 * its {@link SmelteryControllerScreen}.
 *
 * <p>{@link #register(IEventBus)} is invoked from {@code SConstruct} under a {@code Dist.CLIENT}
 * guard so this class never resolves on a dedicated server; the call relocates into the
 * smeltery pulse's client setup at SMTCON-131.
 */
public final class SmelteryClientMenus {

    private SmelteryClientMenus() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SmelteryClientMenus::onRegisterMenuScreens);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SmelteryComponents.SMELTERY_CONTROLLER_MENU.get(), SmelteryControllerScreen::new);
    }
}
