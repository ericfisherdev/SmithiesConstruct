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
import slimeknights.sconstruct.port1211.tools.PartType;
import slimeknights.sconstruct.port1211.tools.item.ToolParts;

/**
 * The mod's four user-facing creative tabs. {@link #GENERAL} is the catch-all — every content
 * surface in the mod shows up there. {@link #TOOLS}, {@link #PARTS}, and {@link #MATERIALS} are
 * themed subsets that let players browse a slice of the content without scrolling the whole
 * {@code GENERAL} tab.
 *
 * <p>Every tab is registered with an empty {@code displayItems} lambda — population happens
 * exclusively through {@link BuildCreativeModeTabContentsEvent} listeners. That keeps each tab
 * decoupled from any single content class: a content class plugs into a tab by subscribing a
 * listener that gates on the tab's key, rather than editing this file's builder calls.
 *
 * <p>Routing: each content class adds its items to {@code GENERAL} (so nothing is ever missing
 * from the catch-all) and, when relevant, also to the themed tab whose theme it belongs to.
 * {@code SharedTabs} itself routes its materials-type items (ingots, nuggets, metal storage
 * blocks, slimeballs) into {@link #MATERIALS}; decoratives and misc items stay {@code GENERAL}-only.
 *
 * <p>Titles come from lang keys {@code itemGroup.sconstruct[.tools|.parts|.materials]}. Icons
 * are picked to be immediately identifiable in the creative tab strip: cobalt ingot for
 * {@code GENERAL}, a pickaxe for {@code TOOLS}, a pick-head part for {@code PARTS}, a steel
 * ingot for {@code MATERIALS}.
 */
public final class SharedTabs {

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GENERAL = TinkerRegistries.CREATIVE_TABS.register("general",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.sconstruct")).icon(() -> new ItemStack(SharedItems.INGOT_COBALT.get())).displayItems((params, output) -> {
                // Empty — content is appended by the BuildCreativeModeTabContentsEvent
                // listener at SharedTabs#populateContent and by every content class that
                // gates a listener on this tab's key.
            }).build());

    /** Themed tab: every built tool plus the four workstation block-items. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS = TinkerRegistries.CREATIVE_TABS.register("tools",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.sconstruct.tools")).icon(() -> new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE))
                    .withTabsAfter(GENERAL.getKey()).displayItems((params, output) -> {
                        // Empty — populated by the themed listener in tools.item.ToolItems and
                        // the workstation registries.
                    }).build());

    /** Themed tab: the sixteen {@link slimeknights.sconstruct.port1211.tools.item.MaterialItem} tool parts. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PARTS = TinkerRegistries.CREATIVE_TABS.register("parts",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.sconstruct.parts")).icon(() -> new ItemStack(ToolParts.get(PartType.PICKHEAD).get())).withTabsAfter(TOOLS.getKey())
                    .displayItems((params, output) -> {
                        // Empty — populated by the themed listener in tools.item.ToolParts.
                    }).build());

    /** Themed tab: ingots, nuggets, metal storage blocks, slimeballs, and slime-fluid buckets. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MATERIALS = TinkerRegistries.CREATIVE_TABS.register("materials", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sconstruct.materials")).icon(() -> new ItemStack(SharedItems.INGOT_STEEL.get())).withTabsAfter(PARTS.getKey()).displayItems((params, output) -> {
                // Empty — populated by SharedTabs#populateContent (materials subset)
                // and the world pulse's fluid-bucket listener.
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
        if (GENERAL.getKey().equals(event.getTabKey())) {
            acceptAll(event::accept);
        }
        else if (MATERIALS.getKey().equals(event.getTabKey())) {
            acceptMaterials(event::accept);
        }
    }

    /**
     * Visits the materials-type subset of the shared-pulse content — metal storage blocks,
     * ingots, nuggets, and slimeballs. Routed into {@link #MATERIALS}. Decoratives and misc
     * items are intentionally excluded; they stay {@code GENERAL}-only. The slime-fluid buckets
     * that round out the {@code MATERIALS} theme are added separately by the world pulse's own
     * listener so {@code SharedTabs} need not reference the {@code world} package.
     */
    static void acceptMaterials(Consumer<ItemLike> accept) {
        SharedBlocks.METAL_BLOCKS.values().forEach(block -> accept.accept(block.get()));
        SharedItems.INGOTS.forEach(ingot -> accept.accept(ingot.get()));
        SharedItems.NUGGETS.forEach(nugget -> accept.accept(nugget.get()));
        SharedItems.SLIMEBALLS.forEach(slimeball -> accept.accept(slimeball.get()));
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
