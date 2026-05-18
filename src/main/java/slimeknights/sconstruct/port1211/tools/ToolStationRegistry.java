package slimeknights.sconstruct.port1211.tools;

import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.shared.SharedTabs;
import slimeknights.sconstruct.port1211.tools.block.ToolForgeBlock;
import slimeknights.sconstruct.port1211.tools.block.ToolStationBlock;
import slimeknights.sconstruct.port1211.tools.block.entity.ToolForgeBlockEntity;
import slimeknights.sconstruct.port1211.tools.block.entity.ToolStationBlockEntity;
import slimeknights.sconstruct.port1211.tools.inventory.ToolStationMenu;

/**
 * Registration hub for the Tool Station + Tool Forge (SMTCON-93). Owns both blocks, both
 * block-items, both {@link BlockEntityType} entries, and both {@link MenuType} entries.
 *
 * <p>The Tool Station and the Tool Forge share menu / screen classes — the only registration
 * difference is the BE type and the menu type, which carry different "accepted tool roster"
 * semantics (basic vs advanced) via the BE override.
 *
 * <p>Same shape as {@link PartBuilderRegistry}: {@link DeferredHolder} fields plus an
 * {@link #init()} touch hook and a {@link #registerCreativeTabContents} subscriber. The wiring
 * will move into {@code TinkerToolsPulse} when that pulse lands.
 */
public final class ToolStationRegistry {

    /** Registry path for the tool-station block, BE, and menu. */
    public static final String STATION_PATH = "tool_station";
    /** Registry path for the tool-forge block, BE, and menu. */
    public static final String FORGE_PATH = "tool_forge";

    public static final DeferredBlock<ToolStationBlock> TOOL_STATION = TinkerRegistries.BLOCKS.register(STATION_PATH, () -> new ToolStationBlock(ToolStationBlock.defaultProperties()));

    public static final DeferredItem<BlockItem> TOOL_STATION_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(TOOL_STATION);

    public static final DeferredBlock<ToolForgeBlock> TOOL_FORGE = TinkerRegistries.BLOCKS.register(FORGE_PATH, () -> new ToolForgeBlock(ToolStationBlock.defaultProperties()));

    public static final DeferredItem<BlockItem> TOOL_FORGE_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(TOOL_FORGE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToolStationBlockEntity>> TOOL_STATION_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register(STATION_PATH,
            () -> BlockEntityType.Builder.of(ToolStationBlockEntity::new, TOOL_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToolForgeBlockEntity>> TOOL_FORGE_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register(FORGE_PATH,
            () -> BlockEntityType.Builder.of(ToolForgeBlockEntity::new, TOOL_FORGE.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ToolStationMenu>> TOOL_STATION_MENU = TinkerRegistries.MENU_TYPES.register(STATION_PATH,
            () -> IMenuTypeExtension.create((containerId, playerInventory, buf) -> ToolStationMenu.fromNetwork(false, containerId, playerInventory, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<ToolStationMenu>> TOOL_FORGE_MENU = TinkerRegistries.MENU_TYPES.register(FORGE_PATH,
            () -> IMenuTypeExtension.create((containerId, playerInventory, buf) -> ToolStationMenu.fromNetwork(true, containerId, playerInventory, buf)));

    private ToolStationRegistry() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        Objects.requireNonNull(TOOL_STATION);
        Objects.requireNonNull(TOOL_FORGE);
    }

    /**
     * Subscribes a {@link BuildCreativeModeTabContentsEvent} listener that pushes both
     * block-items into {@link SharedTabs#GENERAL}.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(ToolStationRegistry::populateCreativeTab);
    }

    /** Test seam: visits both block-items. */
    public static void acceptItems(Consumer<ItemLike> accept) {
        accept.accept(TOOL_STATION_ITEM.get());
        accept.accept(TOOL_FORGE_ITEM.get());
    }

    @SubscribeEvent
    private static void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (SharedTabs.GENERAL.getKey().equals(event.getTabKey()) || SharedTabs.TOOLS.getKey().equals(event.getTabKey())) {
            acceptItems(event::accept);
        }
    }

    /** Returns the registered {@link Item} count this registry owns. Test seam. */
    static int registeredItemCount() {
        // Two BlockItems — station + forge.
        return 2;
    }
}
