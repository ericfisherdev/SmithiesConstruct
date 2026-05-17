package slimeknights.sconstruct.port1211.gadgets;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * Gadget capability registrations, delegated into by the central {@code TinkerCapabilities}
 * dispatcher (SMTCON-142). Exposes the gadget block entities' inventories on the standard
 * {@code Capabilities.ItemHandler.BLOCK} channel so hoppers, pipes, and other automation can
 * load and unload them:
 *
 * <ul>
 *   <li>the drying rack's single item slot;</li>
 *   <li>the wooden hopper's container, wrapped as an item handler for modded-pipe interop.</li>
 * </ul>
 *
 * <p>No piggyback entity capability is registered: the ride state lives in the
 * {@code GadgetAttachments#PIGGYBACK} attachment (SMTCON-134), and the attachment is itself the
 * access layer — {@code player.getData(PIGGYBACK)} is the query. An entity capability that
 * merely proxied the attachment would add a second source of truth for no benefit.
 */
public final class GadgetCapabilities {

    private GadgetCapabilities() {
    }

    /** Hook for {@code TinkerCapabilities} to delegate the gadgets' capability registrations into. */
    public static void register(RegisterCapabilitiesEvent event) {
        // The drying rack exposes its single item slot so a hopper above or a pipe can deliver
        // items to dry and collect the finished output.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GadgetBlocks.DRYING_RACK_BE.get(), (rack, side) -> rack.getInputHandler());
        // The wooden hopper exposes its container as an item handler for modded pipe interop.
        // Vanilla hopper-to-container transfer uses the Container interface directly and needs
        // no capability; this binding is purely for automation that queries the cap channel.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GadgetBlocks.WOODEN_HOPPER_BE.get(), (hopper, side) -> new InvWrapper(hopper));
    }
}
