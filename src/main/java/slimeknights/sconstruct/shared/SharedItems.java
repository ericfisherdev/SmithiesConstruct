package slimeknights.sconstruct.shared;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.common.TinkerRegistries;

/**
 * Registers the per-metal and slime-variant items the shared pulse owns. Three families are
 * exposed:
 *
 * <ul>
 *   <li>{@link #INGOTS} — one {@code ingot_<metal>} item per metal in {@link SharedMetals#ALL}.
 *       Unlike {@link SharedBlocks}, no metal is skipped: lead and nickel shipped as ingots in
 *       1.12, and the casting pipeline in Phase 5 needs an output item for every fluid.</li>
 *   <li>{@link #NUGGETS} — one {@code nugget_<metal>} item per metal, in 1:1 correspondence
 *       with {@link #INGOTS}. Vanilla treats nuggets as 1/9 of an ingot and recipes/casting
 *       depend on the pair existing for every metal.</li>
 *   <li>{@link #SLIMEBALLS} — four coloured slimeball variants (purple, blood, blue, magma)
 *       used as Phase-3 slime-mob drops and Phase-6 gadget reagents. Vanilla ships the green
 *       variant already; these four extend the palette.</li>
 *   <li>Miscellaneous: {@link #BACON} (a food item with the legacy nutrition/saturation values)
 *       and {@link #MUDBRICK} (a plain crafting item used by Phase-6 gadget recipes). Neither
 *       fits a family and they are exposed as bare static fields rather than a list.</li>
 *   <li>Fluid buckets: {@link #BUCKET_BLOOD}. Each sconstruct fluid (see {@link SharedFluids})
 *       ships a paired filled bucket. Lives here rather than in {@link SharedFluids} so all
 *       per-mod {@link Item} registrations share the same {@link TinkerRegistries#ITEMS}
 *       call-site and creative-tab listener.</li>
 * </ul>
 *
 * <p>Per-metal registrations go through {@link #metalItem} which validates the metal exists in
 * the driver list, derives the registry path from the supplied prefix, and appends the result
 * to the matching family list. Slimeballs use a parallel {@link #slimeball} helper that
 * validates against {@link #SLIMEBALL_VARIANTS} so a typo fails fast at class-load. Items ship
 * with default {@link Item.Properties} — sprite, lang, and tag wiring arrive in separate
 * Phase-2 tasks.
 *
 * <p>{@link #init()} forces this class to load during mod construction so the field
 * initialisers run and {@link TinkerRegistries#ITEMS} sees every entry before the registry
 * event fires.
 */
public final class SharedItems {

    /**
     * Coloured slimeball variants recognised by {@link #slimeball}. Order matches declaration
     * order of the {@code SLIMEBALL_*} fields below; downstream providers iterate
     * {@link #SLIMEBALLS} for deterministic output.
     */
    private static final List<String> SLIMEBALL_VARIANTS = List.of("purple", "blood", "blue", "magma");

    private static final List<DeferredItem<Item>> INGOT_BUILDER = new ArrayList<>();
    private static final List<DeferredItem<Item>> NUGGET_BUILDER = new ArrayList<>();
    private static final List<DeferredItem<Item>> SLIMEBALL_BUILDER = new ArrayList<>();

    public static final DeferredItem<Item> INGOT_COBALT = ingot("cobalt");
    public static final DeferredItem<Item> INGOT_ARDITE = ingot("ardite");
    public static final DeferredItem<Item> INGOT_MANYULLYN = ingot("manyullyn");
    public static final DeferredItem<Item> INGOT_KNIGHTSLIME = ingot("knightslime");
    public static final DeferredItem<Item> INGOT_PIGIRON = ingot("pigiron");
    public static final DeferredItem<Item> INGOT_SILVER = ingot("silver");
    public static final DeferredItem<Item> INGOT_TIN = ingot("tin");
    public static final DeferredItem<Item> INGOT_ZINC = ingot("zinc");
    public static final DeferredItem<Item> INGOT_BRASS = ingot("brass");
    public static final DeferredItem<Item> INGOT_ALUBRASS = ingot("alubrass");
    public static final DeferredItem<Item> INGOT_ELECTRUM = ingot("electrum");
    public static final DeferredItem<Item> INGOT_STEEL = ingot("steel");
    public static final DeferredItem<Item> INGOT_LEAD = ingot("lead");
    public static final DeferredItem<Item> INGOT_NICKEL = ingot("nickel");

    public static final DeferredItem<Item> NUGGET_COBALT = nugget("cobalt");
    public static final DeferredItem<Item> NUGGET_ARDITE = nugget("ardite");
    public static final DeferredItem<Item> NUGGET_MANYULLYN = nugget("manyullyn");
    public static final DeferredItem<Item> NUGGET_KNIGHTSLIME = nugget("knightslime");
    public static final DeferredItem<Item> NUGGET_PIGIRON = nugget("pigiron");
    public static final DeferredItem<Item> NUGGET_SILVER = nugget("silver");
    public static final DeferredItem<Item> NUGGET_TIN = nugget("tin");
    public static final DeferredItem<Item> NUGGET_ZINC = nugget("zinc");
    public static final DeferredItem<Item> NUGGET_BRASS = nugget("brass");
    public static final DeferredItem<Item> NUGGET_ALUBRASS = nugget("alubrass");
    public static final DeferredItem<Item> NUGGET_ELECTRUM = nugget("electrum");
    public static final DeferredItem<Item> NUGGET_STEEL = nugget("steel");
    public static final DeferredItem<Item> NUGGET_LEAD = nugget("lead");
    public static final DeferredItem<Item> NUGGET_NICKEL = nugget("nickel");

    public static final DeferredItem<Item> SLIMEBALL_PURPLE = slimeball("purple");
    public static final DeferredItem<Item> SLIMEBALL_BLOOD = slimeball("blood");
    public static final DeferredItem<Item> SLIMEBALL_BLUE = slimeball("blue");
    public static final DeferredItem<Item> SLIMEBALL_MAGMA = slimeball("magma");

    /**
     * Bacon — the meme food item from legacy. Nutrition 3 + saturation modifier 0.6 matches
     * the 1.12 values verbatim so save-game porters get the same hunger restore.
     */
    public static final DeferredItem<Item> BACON = TinkerRegistries.ITEMS.registerItem("bacon",
            props -> new Item(props.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.6f).build())));

    /**
     * Mud brick — a plain crafting item (not a block); fed into Phase-6 gadget recipes.
     * Sprite and recipe wiring arrive in later tasks per AC.
     */
    public static final DeferredItem<Item> MUDBRICK = TinkerRegistries.ITEMS.registerSimpleItem("mudbrick");

    /**
     * Blood bucket — filled bucket of {@link SharedFluids#BLOOD}. {@code craftRemainder(BUCKET)}
     * gives the empty bucket back when a recipe consumes this one; {@code stacksTo(1)} matches
     * the vanilla bucket convention. The Fluid reference is resolved at lambda-fire time
     * (after FLUIDS registers but before ITEMS), so the cross-class forward reference is safe.
     */
    public static final DeferredItem<BucketItem> BUCKET_BLOOD = TinkerRegistries.ITEMS.registerItem("blood_bucket",
            props -> new BucketItem(SharedFluids.BLOOD.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));

    /**
     * The "Materials and You" guidebook (SMTCON-158) — right-clicking it opens the Patchouli
     * book {@code sconstruct:materialsandyou}. The book id must use the {@code sconstruct}
     * namespace: Patchouli's {@code BookRegistry} scans {@code data/<modid>/patchouli_books/}
     * per mod container, so a book under any namespace other than this mod's id is never
     * registered (SMTCON-209).
     */
    public static final DeferredItem<GuidebookItem> MATERIALS_BOOK = TinkerRegistries.ITEMS.registerItem("book_materials",
            props -> new GuidebookItem(props, ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "materialsandyou")));

    /**
     * Immutable insertion-ordered view over every registered ingot. Downstream providers
     * (tags, recipes, lang, models) iterate this list instead of the static fields so a new
     * metal lights up every provider by appending to {@link SharedMetals#ALL} and adding one
     * field above — no provider edit required.
     */
    public static final List<DeferredItem<Item>> INGOTS = List.copyOf(INGOT_BUILDER);

    /**
     * Immutable insertion-ordered view over every registered nugget. Mirrors {@link #INGOTS}
     * one-to-one; index {@code i} of either list refers to the same metal in
     * {@link SharedMetals#ALL}.
     */
    public static final List<DeferredItem<Item>> NUGGETS = List.copyOf(NUGGET_BUILDER);

    /**
     * Immutable insertion-ordered view over the four coloured slimeballs. Phase-3 slime-mob
     * loot tables and Phase-6 gadget recipes iterate this list rather than reach for the
     * static fields.
     */
    public static final List<DeferredItem<Item>> SLIMEBALLS = List.copyOf(SLIMEBALL_BUILDER);

    private SharedItems() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. Same pattern as {@link SharedBlocks#init()}.
    }

    private static DeferredItem<Item> ingot(String id) {
        return metalItem("ingot_", id, INGOT_BUILDER);
    }

    private static DeferredItem<Item> nugget(String id) {
        return metalItem("nugget_", id, NUGGET_BUILDER);
    }

    private static DeferredItem<Item> metalItem(String pathPrefix, String id, List<DeferredItem<Item>> sink) {
        // Validate the metal exists in the driver list so a typo here fails fast at class
        // load rather than producing a sconstruct:<prefix>_typo item that downstream providers
        // can't tag or recipe-target.
        if (SharedMetals.ALL.stream().noneMatch(metal -> metal.id().equals(id))) {
            throw new IllegalArgumentException("no metal in SharedMetals.ALL with id '" + id + "'");
        }
        DeferredItem<Item> item = TinkerRegistries.ITEMS.registerSimpleItem(pathPrefix + id);
        sink.add(item);
        return item;
    }

    private static DeferredItem<Item> slimeball(String variant) {
        // Pin the variant against SLIMEBALL_VARIANTS so a typo (e.g. "magama") fails fast at
        // class load rather than producing a sconstruct:slimeball_magama item the lang and
        // tag providers will not know about.
        if (!SLIMEBALL_VARIANTS.contains(variant)) {
            throw new IllegalArgumentException("unknown slimeball variant '" + variant + "'; expected one of " + SLIMEBALL_VARIANTS);
        }
        DeferredItem<Item> item = TinkerRegistries.ITEMS.registerSimpleItem("slimeball_" + variant);
        SLIMEBALL_BUILDER.add(item);
        return item;
    }
}
