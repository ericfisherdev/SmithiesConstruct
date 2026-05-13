package slimeknights.tconstruct.port1211.shared;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.tconstruct.port1211.common.TinkerRegistries;

/**
 * Registers one ingot item per metal in {@link SharedMetals#ALL}. Unlike {@link SharedBlocks}
 * which restricts to metals with a legacy storage-block precedent, every metal in the driver
 * list gets an ingot — lead and nickel shipped as ingots only in 1.12, and the casting pipeline
 * in Phase 5 needs an output item for every fluid it can mint.
 *
 * <p>Each registration goes through {@link #ingot} which derives the registry path from the
 * metal id ({@code ingot_<id>}), keeping the boilerplate the legacy code paid per metal down
 * to a single call site. Items ship with default {@link Item.Properties} — sprite, lang, and
 * tag wiring arrive in separate Phase-2 tasks.
 *
 * <p>{@link #init()} forces this class to load during mod construction so the field
 * initialisers run and {@link TinkerRegistries#ITEMS} sees every entry before the registry
 * event fires.
 */
public final class SharedItems {

    private static final List<DeferredItem<Item>> BUILDER = new ArrayList<>();

    public static final DeferredItem<Item> INGOT_COBALT = ingot("cobalt");
    public static final DeferredItem<Item> INGOT_ARDITE = ingot("ardite");
    public static final DeferredItem<Item> INGOT_MANYULLYN = ingot("manyullyn");
    public static final DeferredItem<Item> INGOT_KNIGHTSLIME = ingot("knightslime");
    public static final DeferredItem<Item> INGOT_PIGIRON = ingot("pigiron");
    public static final DeferredItem<Item> INGOT_SILVER = ingot("silver");
    public static final DeferredItem<Item> INGOT_COPPER = ingot("copper");
    public static final DeferredItem<Item> INGOT_TIN = ingot("tin");
    public static final DeferredItem<Item> INGOT_ZINC = ingot("zinc");
    public static final DeferredItem<Item> INGOT_BRASS = ingot("brass");
    public static final DeferredItem<Item> INGOT_ALUBRASS = ingot("alubrass");
    public static final DeferredItem<Item> INGOT_ELECTRUM = ingot("electrum");
    public static final DeferredItem<Item> INGOT_STEEL = ingot("steel");
    public static final DeferredItem<Item> INGOT_LEAD = ingot("lead");
    public static final DeferredItem<Item> INGOT_NICKEL = ingot("nickel");

    /**
     * Immutable insertion-ordered view over every registered ingot. Downstream providers
     * (tags, recipes, lang, models) iterate this list instead of the static fields so a new
     * metal lights up every provider by appending to {@link SharedMetals#ALL} and adding one
     * field above — no provider edit required.
     */
    public static final List<DeferredItem<Item>> INGOTS = List.copyOf(BUILDER);

    private SharedItems() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. Same pattern as {@link SharedBlocks#init()}.
    }

    /**
     * Subscribes every ingot to the vanilla {@code INGREDIENTS} creative tab so they're
     * reachable without typing into the search bar. INGREDIENTS is the right vanilla bucket
     * for crafting components — recipe outputs that aren't placeable belong here, not in
     * BUILDING_BLOCKS.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(SharedItems::onBuildCreativeTabContents);
    }

    @SubscribeEvent
    private static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!CreativeModeTabs.INGREDIENTS.equals(event.getTabKey())) {
            return;
        }
        INGOTS.forEach(ingot -> event.accept(ingot.get()));
    }

    private static DeferredItem<Item> ingot(String id) {
        // Validate the metal exists in the driver list so a typo here fails fast at class
        // load rather than producing a tconstruct:ingot_typo item that downstream providers
        // can't tag or recipe-target.
        if (SharedMetals.ALL.stream().noneMatch(metal -> metal.id().equals(id))) {
            throw new IllegalArgumentException("no metal in SharedMetals.ALL with id '" + id + "'");
        }
        DeferredItem<Item> item = TinkerRegistries.ITEMS.registerSimpleItem("ingot_" + id);
        BUILDER.add(item);
        return item;
    }
}
