package slimeknights.sconstruct.port1211.tools.client;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.tools.item.MaterialItem;
import slimeknights.sconstruct.port1211.tools.item.ToolParts;
import slimeknights.sconstruct.port1211.tools.material.Material;
import slimeknights.sconstruct.port1211.tools.material.client.MaterialClientCache;

/**
 * Client-side wiring for material-tinted {@link MaterialItem} part items (SMTCON-72).
 *
 * <ul>
 *   <li>{@link RegisterColorHandlersEvent.Item} (mod bus) — register an {@code ItemColor}
 *       against every part item registered by {@link ToolParts}. The handler returns the
 *       material's cached colour on layer 0 and {@code -1} (no tint) on every other layer so
 *       multi-layer part models can still ship un-tinted overlays.</li>
 *   <li>{@link ClientPlayerNetworkEvent.LoggingIn} (NeoForge game bus) — refresh
 *       {@link MaterialClientCache} from the connection's just-synced
 *       {@link HolderLookup.RegistryLookup} for {@link Material#REGISTRY_KEY}. Subsequent
 *       {@code /reload} cycles re-sync the registry through the same channel; the cache
 *       refreshes opportunistically when the player rejoins.</li>
 * </ul>
 *
 * <p>Loaded only on the client side — {@link SConstruct} guards the {@link #register(IEventBus)}
 * call with a {@code Dist.CLIENT} check so this class never resolves on a dedicated server.
 */
public final class ToolColorHandlers {

    private ToolColorHandlers() {
    }

    /**
     * Pure tint computation extracted as a static seam so it can be unit-tested without a live
     * {@code ItemColors} dispatcher. Layer-0 returns the per-material cached tint; every other
     * layer returns {@code -1} (the renderer's "no tint" sentinel).
     */
    public static int resolveColor(ItemStack stack, int tintLayer) {
        if (tintLayer != 0) {
            return -1;
        }
        ResourceLocation materialId = stack.get(TinkerDataComponents.PART_MATERIAL.get());
        if (materialId == null) {
            materialId = MaterialItem.DEFAULT_MATERIAL;
        }
        return MaterialClientCache.getColor(materialId);
    }

    /**
     * Subscribe both the {@link RegisterColorHandlersEvent.Item} handler (mod bus) and the
     * {@link ClientPlayerNetworkEvent.LoggingIn} cache refresh (NeoForge game bus). Invoke once
     * from the mod entrypoint behind a {@code Dist.CLIENT} guard.
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(ToolColorHandlers::onRegisterColorHandlers);
        NeoForge.EVENT_BUS.addListener(ToolColorHandlers::onClientLoggingIn);
    }

    private static void onRegisterColorHandlers(RegisterColorHandlersEvent.Item event) {
        for (DeferredItem<MaterialItem> part : ToolParts.PARTS.values()) {
            event.register(ToolColorHandlers::resolveColor, part.get());
        }
    }

    private static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // ClientPlayerNetworkEvent#getConnection returns the underlying Netty Connection rather
        // than the ClientPacketListener — the listener (with its synced registryAccess) hangs off
        // the local player. Read it from there; bail out if the player isn't available yet (a
        // crash during configuration phase can fire LoggingIn with a half-built player ref).
        if (event.getPlayer() == null) {
            return;
        }
        event.getPlayer().connection.registryAccess().lookup(Material.REGISTRY_KEY).ifPresent(MaterialClientCache::populateFrom);
    }
}
