package slimeknights.sconstruct.port1211.tools.item;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
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
import slimeknights.sconstruct.port1211.tools.PartType;

/**
 * Registration hub for the sixteen {@link MaterialItem} tool-part items — one per
 * {@link PartType}. Each item is registered against {@link TinkerRegistries#ITEMS} at the
 * resource path matching {@link PartType#id()} ({@code sconstruct:pickhead},
 * {@code sconstruct:handle}, …) so the item key round-trips byte-for-byte with the part-type
 * codec form.
 *
 * <p>The {@link DeferredItem} for each slot is held in an unmodifiable {@link EnumMap}
 * exposed via {@link #get(PartType)}. {@link EnumMap} gives downstream lookups O(1) without
 * the boxing overhead of {@link java.util.HashMap}; the unmodifiable wrapper keeps the map
 * read-only at the API boundary so later pulses can't accidentally mutate the registration
 * state.
 *
 * <p>{@link #registerCreativeTabContents(IEventBus)} is the seam future {@code TinkerToolsPulse}
 * will use to push every part item into {@link SharedTabs#GENERAL} — invoked from
 * {@code SConstruct} now (no tools pulse yet) so the items satisfy the acceptance criterion
 * "Each visible in creative inventory" today.
 */
public final class ToolParts {

    private static final Map<PartType, DeferredItem<MaterialItem>> BUILDER = new EnumMap<>(PartType.class);

    static {
        for (PartType part : PartType.values()) {
            BUILDER.put(part, TinkerRegistries.ITEMS.registerItem(part.id(), props -> new MaterialItem(props, part)));
        }
    }

    /** Immutable view of the {@link PartType} → {@link DeferredItem} map. */
    public static final Map<PartType, DeferredItem<MaterialItem>> PARTS = Collections.unmodifiableMap(BUILDER);

    private ToolParts() {
    }

    /** Forces class load so the static initialiser registers every part item. */
    public static void init() {
        // Touch a field so the static block fires. No-op return keeps the call site readable.
        Objects.requireNonNull(PARTS);
    }

    /**
     * The registered {@link DeferredItem} for the supplied {@link PartType}. Never returns
     * {@code null} — every {@link PartType} has a corresponding entry by construction; an
     * absent value would mean the static initialiser was skipped, which is a JVM-level bug
     * worth surfacing rather than silently masking.
     */
    public static DeferredItem<MaterialItem> get(PartType part) {
        DeferredItem<MaterialItem> item = PARTS.get(Objects.requireNonNull(part, "part"));
        if (item == null) {
            throw new IllegalStateException("ToolParts.PARTS is missing an entry for " + part + " — class loading order regressed");
        }
        return item;
    }

    /**
     * Subscribes a {@link BuildCreativeModeTabContentsEvent} listener that appends every part
     * item to {@link SharedTabs#GENERAL}. Called from {@code SConstruct} during mod
     * construction. Will move into {@code TinkerToolsPulse#register} once that pulse exists.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(ToolParts::populateCreativeTab);
    }

    /** Visits every registered part item with the supplied consumer. Test seam. */
    public static void acceptAll(Consumer<ItemLike> accept) {
        for (PartType part : PartType.values()) {
            accept.accept(get(part).get());
        }
    }

    @SubscribeEvent
    private static void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (SharedTabs.GENERAL.getKey().equals(event.getTabKey()) || SharedTabs.PARTS.getKey().equals(event.getTabKey())) {
            acceptAll(event::accept);
        }
    }

    /** Test seam: number of registered part items. */
    static int registeredCount() {
        return PARTS.size();
    }

    /** Test seam: resource path prefix-less view of a part's registration id. */
    static String registeredPath(PartType part) {
        return get(part).getId().getPath();
    }

    /** Test hook ensuring the {@link Item} type bound to {@link MaterialItem} round-trips. */
    static boolean isMaterialItem(PartType part) {
        Item item = get(part).get();
        return item instanceof MaterialItem;
    }
}
