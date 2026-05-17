package slimeknights.sconstruct.port1211.gadgets;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.gadgets.item.SlimeSlingItem;
import slimeknights.sconstruct.port1211.shared.SharedTabs;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * Registration hub for the Phase-6 gadget items. SMTCON-132 ships the first four: one
 * {@link SlimeSlingItem} per {@link SlimeColor}, each registered under
 * {@link TinkerRegistries#ITEMS}.
 *
 * <p>Every slimesling shares the {@link #SLING_DURABILITY} budget and a max stack size of one
 * (a charged tool, like a bow). The per-colour launch profile and on-release side effect live
 * in {@link SlimeSlingItem}; this hub only wires the registrations and exposes the
 * {@link #ALL} roster for the creative-tab listener and downstream data providers.
 *
 * <p>{@link #init()} forces the {@code DeferredItem} field initialisers to run before the item
 * registry event fires; {@link #registerCreativeTabContents(IEventBus)} subscribes the
 * {@link BuildCreativeModeTabContentsEvent} listener that appends every slimesling to
 * {@link SharedTabs#GENERAL}. Both are invoked from {@code SConstruct} for now; they relocate
 * into the gadgets pulse's {@code register()} when that pulse is wired.
 */
public final class GadgetItems {

    /** Uses a slimesling lasts before breaking — one durability point is spent per launch. */
    private static final int SLING_DURABILITY = 250;

    /** Blue slimesling — the baseline bounce launch. */
    public static final DeferredItem<SlimeSlingItem> SLING_BLUE = registerSling("blue", SlimeColor.BLUE);

    /** Purple slimesling — a long-range launch that throws the player farther per charge. */
    public static final DeferredItem<SlimeSlingItem> SLING_PURPLE = registerSling("purple", SlimeColor.PURPLE);

    /** Magma slimesling — the baseline launch, and sets the player on fire on release. */
    public static final DeferredItem<SlimeSlingItem> SLING_MAGMA = registerSling("magma", SlimeColor.MAGMA);

    /** Blood slimesling — the baseline launch, and heals the player on release. */
    public static final DeferredItem<SlimeSlingItem> SLING_BLOOD = registerSling("blood", SlimeColor.BLOOD);

    /** Insertion-ordered roster of every registered slimesling, for the creative tab and providers. */
    public static final List<DeferredItem<SlimeSlingItem>> ALL = List.of(SLING_BLUE, SLING_PURPLE, SLING_MAGMA, SLING_BLOOD);

    private GadgetItems() {
    }

    /**
     * No-op that forces this class to load so the {@code DeferredItem} field initialisers above
     * run, registering every slimesling against {@link TinkerRegistries#ITEMS} before the item
     * registry event fires.
     */
    public static void init() {
        // Touching a holder forces the static initialiser chain — same pattern as ToolItems#init.
        SLING_BLUE.getId();
    }

    /**
     * Subscribes a {@link BuildCreativeModeTabContentsEvent} listener that appends every
     * slimesling to {@link SharedTabs#GENERAL}. Called from {@code SConstruct} during mod
     * construction.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(GadgetItems::populateCreativeTab);
    }

    /** Feeds every registered slimesling to {@code accept} in registration order. */
    public static void acceptAll(Consumer<ItemLike> accept) {
        for (DeferredItem<SlimeSlingItem> sling : ALL) {
            accept.accept(sling.get());
        }
    }

    private static void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!SharedTabs.GENERAL.getKey().equals(event.getTabKey())) {
            return;
        }
        acceptAll(event::accept);
    }

    private static DeferredItem<SlimeSlingItem> registerSling(String colorId, SlimeColor color) {
        return TinkerRegistries.ITEMS.registerItem("slimesling_" + colorId, props -> new SlimeSlingItem(props.durability(SLING_DURABILITY).stacksTo(1), color));
    }
}
