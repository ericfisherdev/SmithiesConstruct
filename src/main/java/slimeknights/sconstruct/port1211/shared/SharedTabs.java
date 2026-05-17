package slimeknights.sconstruct.port1211.shared;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;

/**
 * The mod's single user-facing creative tab. Every Phase-2 content surface
 * ({@link SharedBlocks} metal storage + decoratives, {@link SharedItems} ingots/nuggets/
 * slimeballs/misc/buckets) shows up here; Phase-3+ pulses will subscribe their own
 * {@link BuildCreativeModeTabContentsEvent} listeners against {@link #GENERAL} to add their
 * content to the same tab.
 *
 * <p>The tab is registered with an empty {@code displayItems} lambda — population happens
 * exclusively through {@link BuildCreativeModeTabContentsEvent} listeners. That keeps the
 * tab decoupled from any single content class: a new pulse plugs in by subscribing a listener
 * rather than editing this file's builder call.
 *
 * <p>Title comes from the lang key {@code itemGroup.sconstruct} (provided by the lang
 * provider in a later task) and the icon is a cobalt ingot — the most recognisable Tinkers
 * material in the legacy mod, picked over generic stone/wood so the tab is immediately
 * identifiable in the creative inventory's tab strip.
 */
public final class SharedTabs {

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GENERAL = TinkerRegistries.CREATIVE_TABS.register("general",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.sconstruct")).icon(() -> new ItemStack(SharedItems.INGOT_COBALT.get())).displayItems((params, output) -> {
                // Empty — content is appended by the BuildCreativeModeTabContentsEvent
                // listener at SharedTabs#populateContent. Phase-3+ pulses subscribe their own
                // listeners against this tab's key, so a new pulse adds itself by registering
                // an event handler rather than editing this builder call.
            }).build());

    private SharedTabs() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. Same pattern as {@link SharedBlocks#init()}.
    }

    /**
     * Subscribes the shared-pulse content into {@link #GENERAL}. Called from
     * {@code SConstruct} after every content-owning class has been initialised so its lists
     * (e.g. {@link SharedItems#INGOTS}) are populated by the time the event fires.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(SharedTabs::populateContent);
    }

    @SubscribeEvent
    private static void populateContent(BuildCreativeModeTabContentsEvent event) {
        if (!GENERAL.getKey().equals(event.getTabKey())) {
            return;
        }
        acceptAll(event::accept);
    }

    /**
     * Visits every Phase-2 shared-pulse item with the supplied consumer. Lifted out of the
     * event listener so the population logic can be unit-tested without bootstrapping a real
     * {@link BuildCreativeModeTabContentsEvent}. Iteration order matches declaration order in
     * the content classes, which downstream lang/recipe providers also rely on.
     */
    static void acceptAll(Consumer<ItemLike> accept) {
        // Blocks (metal storage + decoratives) — accepted as ItemLike via the registered
        // Block (which carries an asItem() route to the matching BlockItem).
        SharedBlocks.METAL_BLOCKS.values().forEach(block -> accept.accept(block.get()));
        accept.accept(SharedBlocks.GLOW.get());
        accept.accept(SharedBlocks.FIREWOOD.get());
        accept.accept(SharedBlocks.LAVAWOOD.get());
        // Items.
        SharedItems.INGOTS.forEach(ingot -> accept.accept(ingot.get()));
        SharedItems.NUGGETS.forEach(nugget -> accept.accept(nugget.get()));
        SharedItems.SLIMEBALLS.forEach(slimeball -> accept.accept(slimeball.get()));
        accept.accept(SharedItems.MUDBRICK.get());
        accept.accept(SharedItems.BACON.get());
        accept.accept(SharedItems.BUCKET_BLOOD.get());
        accept.accept(SharedItems.MATERIALS_BOOK.get());
    }
}
