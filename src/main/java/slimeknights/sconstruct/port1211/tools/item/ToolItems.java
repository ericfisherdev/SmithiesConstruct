package slimeknights.sconstruct.port1211.tools.item;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.shared.SharedTabs;

/**
 * Registration hub for the four vanilla-equivalent {@link ToolCore} tools (pickaxe, shovel,
 * axe, sword). Each item is registered against {@link TinkerRegistries#ITEMS} at the resource
 * path matching its short name ({@code sconstruct:pickaxe}, …) and added to
 * {@link SharedTabs#GENERAL} via a {@link BuildCreativeModeTabContentsEvent} listener.
 *
 * <p>{@link DeferredItem} singletons are held as {@code public static final} fields so
 * downstream pulses can address each tool by its typed handle without going through a map
 * lookup. The {@link #ALL_TOOLS} list is the iteration surface for the creative-tab listener
 * (and any future "give every tool" hook); it's an unmodifiable copy of the four fields.
 *
 * <p>{@link #registerCreativeTabContents(IEventBus)} is the seam the future
 * {@code TinkerToolsPulse} will use to push every tool item into {@link SharedTabs#GENERAL} —
 * invoked from {@code SConstruct} for now alongside the equivalent {@link ToolParts} wiring.
 *
 * <p>Damage / stat snapshot is intentionally zero on a creative-tab pull (no materials
 * applied). Players who {@code /give} an unbuilt tool see {@code DataComponents.MAX_DAMAGE=0}
 * which renders as the broken sentinel — applying materials via the part-builder rebuilds the
 * stats and unbreaks the tool. The {@code ToolCore.damageItem} hook short-circuits on a
 * zero-max stack so the broken bit doesn't latch on before the durability budget exists.
 */
public final class ToolItems {

    /** Tinker pickaxe — handle + pick-head + binding. */
    public static final DeferredItem<PickaxeItem> PICKAXE = TinkerRegistries.ITEMS.registerItem("pickaxe", PickaxeItem::new);

    /** Tinker shovel — handle + shovel-head + binding. */
    public static final DeferredItem<ShovelItem> SHOVEL = TinkerRegistries.ITEMS.registerItem("shovel", ShovelItem::new);

    /** Tinker hatchet — handle + axe-head + binding. Registered as the {@code axe} item key to
     *  match vanilla tool-slot terminology; the underlying {@link slimeknights.sconstruct.port1211.tools.ToolDefinition#HATCHET}
     *  is the legacy single-handed axe definition. */
    public static final DeferredItem<AxeItem> AXE = TinkerRegistries.ITEMS.registerItem("axe", AxeItem::new);

    /** Tinker broadsword — handle + sword-blade + wide-guard. */
    public static final DeferredItem<SwordItem> SWORD = TinkerRegistries.ITEMS.registerItem("sword", SwordItem::new);

    /** Heavy-pickaxe AOE tool — 3x3 mining swing. */
    public static final DeferredItem<HammerItem> HAMMER = TinkerRegistries.ITEMS.registerItem("hammer", HammerItem::new);

    /** Heavy-shovel AOE tool — 3x3 mining swing. */
    public static final DeferredItem<ExcavatorItem> EXCAVATOR = TinkerRegistries.ITEMS.registerItem("excavator", ExcavatorItem::new);

    /** Two-handed axe — tree-felling pattern. */
    public static final DeferredItem<LumberAxeItem> LUMBER_AXE = TinkerRegistries.ITEMS.registerItem("lumberaxe", LumberAxeItem::new);

    /** Two-handed melee weapon — AOE sweep + 3x3 crop / leaf clear. */
    public static final DeferredItem<ScytheItem> SCYTHE = TinkerRegistries.ITEMS.registerItem("scythe", ScytheItem::new);

    /** Axe / shovel / hoe hybrid — 1x3 vertical dig column. */
    public static final DeferredItem<MattockItem> MATTOCK = TinkerRegistries.ITEMS.registerItem("mattock", MattockItem::new);

    /** Iteration surface for the creative-tab listener and any future "all tools" hook. */
    public static final List<DeferredItem<? extends ToolCore>> ALL_TOOLS = List.of(PICKAXE, SHOVEL, AXE, SWORD, HAMMER, EXCAVATOR, LUMBER_AXE, SCYTHE, MATTOCK);

    private ToolItems() {
    }

    /** Forces class load so the static field initialisers register every tool item. */
    public static void init() {
        // Touch a field so the static initialiser fires. No-op return keeps the call site
        // readable and parallels ToolParts#init.
        Objects.requireNonNull(ALL_TOOLS);
    }

    /**
     * Subscribes a {@link BuildCreativeModeTabContentsEvent} listener that appends every tool
     * item to {@link SharedTabs#GENERAL}. Called from {@code SConstruct} during mod
     * construction. Will move into {@code TinkerToolsPulse#register} once that pulse exists.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(ToolItems::populateCreativeTab);
    }

    /** Visits every registered tool item with the supplied consumer. Test seam. */
    public static void acceptAll(Consumer<ItemLike> accept) {
        for (DeferredItem<? extends ToolCore> tool : ALL_TOOLS) {
            accept.accept(tool.get());
        }
    }

    @SubscribeEvent
    private static void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!SharedTabs.GENERAL.getKey().equals(event.getTabKey())) {
            return;
        }
        acceptAll(event::accept);
    }

    /** Test seam: count of registered tool items. */
    static int registeredCount() {
        return ALL_TOOLS.size();
    }

    /** Test seam: resource path of a tool's registration id. */
    static String registeredPath(DeferredItem<? extends Item> tool) {
        return tool.getId().getPath();
    }
}
