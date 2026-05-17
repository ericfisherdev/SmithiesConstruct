package slimeknights.sconstruct.port1211.gadgets;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.gadgets.item.GlowBallItem;
import slimeknights.sconstruct.port1211.gadgets.item.PiggybackItem;
import slimeknights.sconstruct.port1211.gadgets.item.SlimeSlingItem;
import slimeknights.sconstruct.port1211.gadgets.item.ThrowballItem;
import slimeknights.sconstruct.port1211.shared.SharedTabs;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * Registration hub for the Phase-6 gadget items, all registered under
 * {@link TinkerRegistries#ITEMS}:
 *
 * <ul>
 *   <li>{@link #SLINGS} — one {@link SlimeSlingItem} per {@link SlimeColor} (SMTCON-132). Each
 *       shares the {@link #SLING_DURABILITY} budget and a max stack size of one.</li>
 *   <li>{@link #THROWBALLS} — one {@link ThrowballItem} per {@link SlimeColor} (SMTCON-133), a
 *       thrown projectile that applies a colour-specific area effect on impact.</li>
 *   <li>{@link #PIGGYBACK} — the single {@link PiggybackItem} (SMTCON-134) that lets a player
 *       carry another on their shoulders.</li>
 *   <li>{@link #GLOW_BALL} — the single {@link GlowBallItem} (SMTCON-135), a thrown projectile
 *       that places a glow block where it lands.</li>
 * </ul>
 *
 * <p>The per-colour behaviour lives in the item classes; this hub only wires the registrations
 * and exposes the rosters for the creative-tab listener and downstream data providers.
 *
 * <p>{@link #init()} forces the {@code DeferredItem} field initialisers to run before the item
 * registry event fires; {@link #registerCreativeTabContents(IEventBus)} subscribes the
 * {@link BuildCreativeModeTabContentsEvent} listener that appends every gadget item to
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
    public static final List<DeferredItem<SlimeSlingItem>> SLINGS = List.of(SLING_BLUE, SLING_PURPLE, SLING_MAGMA, SLING_BLOOD);

    /** Blue throwball — slows every living entity caught in the impact radius. */
    public static final DeferredItem<ThrowballItem> THROWBALL_BLUE = registerThrowball("blue", SlimeColor.BLUE);

    /** Purple throwball — a purely cosmetic explosion on impact, no block or entity damage. */
    public static final DeferredItem<ThrowballItem> THROWBALL_PURPLE = registerThrowball("purple", SlimeColor.PURPLE);

    /** Magma throwball — sets every living entity caught in the impact radius on fire. */
    public static final DeferredItem<ThrowballItem> THROWBALL_MAGMA = registerThrowball("magma", SlimeColor.MAGMA);

    /** Blood throwball — weakens every living entity caught in the impact radius. */
    public static final DeferredItem<ThrowballItem> THROWBALL_BLOOD = registerThrowball("blood", SlimeColor.BLOOD);

    /** Insertion-ordered roster of every registered throwball, for the creative tab and providers. */
    public static final List<DeferredItem<ThrowballItem>> THROWBALLS = List.of(THROWBALL_BLUE, THROWBALL_PURPLE, THROWBALL_MAGMA, THROWBALL_BLOOD);

    /** Piggyback item — right-click another player to carry them on your shoulders. */
    public static final DeferredItem<PiggybackItem> PIGGYBACK = TinkerRegistries.ITEMS.registerItem("piggyback", props -> new PiggybackItem(props.stacksTo(1)));

    /** Glow-ball item — a thrown projectile that places a glow block where it lands. */
    public static final DeferredItem<GlowBallItem> GLOW_BALL = TinkerRegistries.ITEMS.registerItem("glow_ball", props -> new GlowBallItem(props.stacksTo(GlowBallItem.STACK_SIZE)));

    /** Immutable insertion-ordered roster of every registered gadget item — slings, throwballs, piggyback, glow ball. */
    public static final List<DeferredItem<? extends net.minecraft.world.item.Item>> ALL = Stream.of(SLINGS, THROWBALLS, List.of(PIGGYBACK, GLOW_BALL)).flatMap(List::stream)
            .collect(Collectors.toUnmodifiableList());

    private GadgetItems() {
    }

    /**
     * No-op that forces this class to load so the {@code DeferredItem} field initialisers above
     * run, registering every gadget item against {@link TinkerRegistries#ITEMS} before the item
     * registry event fires.
     */
    public static void init() {
        // Touching a holder forces the static initialiser chain — same pattern as ToolItems#init.
        SLING_BLUE.getId();
    }

    /**
     * Subscribes a {@link BuildCreativeModeTabContentsEvent} listener that appends every gadget
     * item to {@link SharedTabs#GENERAL}. Called from {@code SConstruct} during mod construction.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(GadgetItems::populateCreativeTab);
    }

    /** Feeds every registered gadget item — slimeslings, throwballs, piggyback, glow ball — to {@code accept}. */
    public static void acceptAll(Consumer<ItemLike> accept) {
        for (DeferredItem<? extends net.minecraft.world.item.Item> gadget : ALL) {
            accept.accept(gadget.get());
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

    private static DeferredItem<ThrowballItem> registerThrowball(String colorId, SlimeColor color) {
        return TinkerRegistries.ITEMS.registerItem("throwball_" + colorId, props -> new ThrowballItem(props.stacksTo(ThrowballItem.STACK_SIZE), color));
    }
}
