package slimeknights.sconstruct.tools.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import slimeknights.sconstruct.tools.PatternChestRegistry;
import slimeknights.sconstruct.tools.inventory.client.PatternChestScreen;

/**
 * Client-side wiring for the Pattern Chest (SMTCON-90). Subscribes a
 * {@link RegisterMenuScreensEvent} listener that pairs
 * {@link PatternChestRegistry#PATTERN_CHEST_MENU} with {@link PatternChestScreen} so the
 * client knows which screen class to instantiate when the server opens the menu.
 *
 * <p>{@link #register(IEventBus)} is the seam invoked from
 * {@link slimeknights.sconstruct.SConstruct} under a {@code Dist.CLIENT} guard so
 * this class never resolves on a dedicated server. Same shape as
 * {@link ToolColorHandlers#register(IEventBus)}.
 */
public final class PatternChestClient {

    private PatternChestClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PatternChestClient::onRegisterMenuScreens);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        // Vanilla resolves the screen factory on the client when the server sends a
        // ClientboundOpenScreenPacket carrying the menu type. The factory's three-arg shape
        // matches the AbstractContainerScreen constructor (menu, inventory, title).
        event.register(PatternChestRegistry.PATTERN_CHEST_MENU.get(), PatternChestScreen::new);
    }
}
