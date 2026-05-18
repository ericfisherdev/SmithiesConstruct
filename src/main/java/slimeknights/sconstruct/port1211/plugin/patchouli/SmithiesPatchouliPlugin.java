package slimeknights.sconstruct.port1211.plugin.patchouli;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.plugin.patchouli.page.PageAlloy;
import slimeknights.sconstruct.port1211.plugin.patchouli.page.PageCasting;
import slimeknights.sconstruct.port1211.plugin.patchouli.page.PageMelting;

import vazkii.patchouli.client.book.ClientBookRegistry;

/**
 * Client-side registration hub for the mod's custom Patchouli page types (SMTCON-162).
 * Patchouli has no formal plugin API for page types — a page type is a {@code resource-location
 * -> BookPage subclass} entry in the public {@link ClientBookRegistry#pageTypes} map — so this
 * class adds the three smeltery page types to that map during client setup.
 *
 * <p>Patchouli's own {@code addPageTypes} only ever {@code put}s into the map (it never clears
 * it), so registering here is order-independent of Patchouli's startup; the entries survive
 * every book reload.
 */
@EventBusSubscriber(modid = SConstruct.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SmithiesPatchouliPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Legacy {@code tconstruct} namespace — book content addresses these page types by it. */
    private static final String BOOK_NAMESPACE = "tconstruct";

    private SmithiesPatchouliPlugin() {
    }

    /**
     * Mod-bus client-setup handler. Registration touches the shared
     * {@link ClientBookRegistry#pageTypes} map, so it runs inside
     * {@link FMLClientSetupEvent#enqueueWork} on the main thread rather than the parallel
     * setup thread.
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SmithiesPatchouliPlugin::registerPageTypes);
    }

    private static void registerPageTypes() {
        var pageTypes = ClientBookRegistry.INSTANCE.pageTypes;
        pageTypes.put(ResourceLocation.fromNamespaceAndPath(BOOK_NAMESPACE, "melting"), PageMelting.class);
        pageTypes.put(ResourceLocation.fromNamespaceAndPath(BOOK_NAMESPACE, "casting"), PageCasting.class);
        pageTypes.put(ResourceLocation.fromNamespaceAndPath(BOOK_NAMESPACE, "alloy"), PageAlloy.class);
        LOGGER.info("Registered 3 Smithies' Construct Patchouli page types (melting, casting, alloy)");
    }
}
