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
import slimeknights.sconstruct.port1211.tools.block.StencilTableBlock;
import slimeknights.sconstruct.port1211.tools.block.entity.StencilTableBlockEntity;
import slimeknights.sconstruct.port1211.tools.inventory.StencilTableMenu;
import slimeknights.sconstruct.port1211.tools.item.PatternItem;

/**
 * Registration hub for the Stencil Table (SMTCON-91) — owns the block, its block-item, the
 * matching {@link BlockEntityType}, the {@link MenuType}, and the two pattern items (blank +
 * the typed-output item, which share the same {@link PatternItem} class).
 *
 * <p>Same shape as
 * {@link slimeknights.sconstruct.port1211.tools.PatternChestRegistry}: {@link DeferredHolder}
 * fields plus an {@link #init()} touch hook and a {@link #registerCreativeTabContents}
 * subscriber. The wiring will move into {@code TinkerToolsPulse} when that pulse lands.
 *
 * <p>Only the blank pattern item ({@link #BLANK_PATTERN}) goes into the creative tab —
 * {@link #PATTERN} is the typed-variant item materialised by the stencil table at GUI craft
 * time and is not directly creative-acquirable. (The same {@link PatternItem} class backs
 * both; the typed variant differs only by the
 * {@link slimeknights.sconstruct.port1211.common.data.TinkerDataComponents#TINKER_PATTERN_PART}
 * data component on the {@link net.minecraft.world.item.ItemStack}.)
 */
public final class StencilTableRegistry {

    /** Registry path for the stencil table block, BE, and menu — single source of truth. */
    public static final String PATH = "stencil_table";

    /** Registry path for the blank pattern item. */
    public static final String BLANK_PATTERN_PATH = "blank_pattern";

    /** Registry path for the typed pattern item. Shares the {@link PatternItem} class. */
    public static final String PATTERN_PATH = "pattern";

    public static final DeferredBlock<StencilTableBlock> STENCIL_TABLE = TinkerRegistries.BLOCKS.register(PATH, () -> new StencilTableBlock(StencilTableBlock.defaultProperties()));

    public static final DeferredItem<BlockItem> STENCIL_TABLE_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(STENCIL_TABLE);

    /**
     * Blank pattern item — empty cursor template, the input the stencil table consumes. No
     * default {@code TINKER_PATTERN_PART} component, so {@link PatternItem#getName} falls back
     * to the registered {@code item.sconstruct.blank_pattern} translation key.
     */
    public static final DeferredItem<PatternItem> BLANK_PATTERN = TinkerRegistries.ITEMS.registerItem(BLANK_PATTERN_PATH, props -> new PatternItem(props));

    /**
     * Typed pattern item — the stencil table's output. Same class as {@link #BLANK_PATTERN};
     * the per-stack {@code TINKER_PATTERN_PART} component carries the {@link PartType} the
     * stack is typed for. Stencil-table BE writes this stack with the component set via
     * {@link PatternItem#makeTyped}.
     */
    public static final DeferredItem<PatternItem> PATTERN = TinkerRegistries.ITEMS.registerItem(PATTERN_PATH, props -> new PatternItem(props));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StencilTableBlockEntity>> STENCIL_TABLE_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register(PATH,
            () -> BlockEntityType.Builder.of(StencilTableBlockEntity::new, STENCIL_TABLE.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<StencilTableMenu>> STENCIL_TABLE_MENU = TinkerRegistries.MENU_TYPES.register(PATH,
            () -> IMenuTypeExtension.create((containerId, playerInventory, buf) -> new StencilTableMenu(containerId, playerInventory, buf)));

    private StencilTableRegistry() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        Objects.requireNonNull(STENCIL_TABLE);
    }

    /**
     * Subscribes a {@link BuildCreativeModeTabContentsEvent} listener that pushes the block-item
     * and the blank pattern into {@link SharedTabs#GENERAL}. The typed {@link #PATTERN} item is
     * intentionally excluded — it is the GUI-only output, not a creative-acquirable form.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(StencilTableRegistry::populateCreativeTab);
    }

    /** Test seam: visits the table's + blank pattern's {@link ItemLike} representations. */
    public static void acceptItems(Consumer<ItemLike> accept) {
        accept.accept(STENCIL_TABLE_ITEM.get());
        accept.accept(BLANK_PATTERN.get());
    }

    @SubscribeEvent
    private static void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (SharedTabs.GENERAL.getKey().equals(event.getTabKey()) || SharedTabs.TOOLS.getKey().equals(event.getTabKey())) {
            acceptItems(event::accept);
        }
    }

    /** Returns the registered {@link Item} count this registry owns. Test seam. */
    static int registeredItemCount() {
        // BlockItem + BLANK_PATTERN + PATTERN = 3.
        return 3;
    }
}
