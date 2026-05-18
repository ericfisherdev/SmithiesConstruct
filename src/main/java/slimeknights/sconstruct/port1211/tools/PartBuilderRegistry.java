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
import slimeknights.sconstruct.port1211.tools.block.PartBuilderBlock;
import slimeknights.sconstruct.port1211.tools.block.entity.PartBuilderBlockEntity;
import slimeknights.sconstruct.port1211.tools.inventory.PartBuilderMenu;

/**
 * Registration hub for the Part Builder (SMTCON-92) — owns the block, its block-item, the
 * matching {@link BlockEntityType}, and the {@link MenuType}.
 *
 * <p>Same shape as {@link StencilTableRegistry}: {@link DeferredHolder} fields plus an
 * {@link #init()} touch hook and a {@link #registerCreativeTabContents} subscriber. The
 * wiring will move into {@code TinkerToolsPulse} when that pulse lands.
 */
public final class PartBuilderRegistry {

    /** Registry path for the part-builder block, BE, and menu — single source of truth. */
    public static final String PATH = "part_builder";

    public static final DeferredBlock<PartBuilderBlock> PART_BUILDER = TinkerRegistries.BLOCKS.register(PATH, () -> new PartBuilderBlock(PartBuilderBlock.defaultProperties()));

    public static final DeferredItem<BlockItem> PART_BUILDER_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(PART_BUILDER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PartBuilderBlockEntity>> PART_BUILDER_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register(PATH,
            () -> BlockEntityType.Builder.of(PartBuilderBlockEntity::new, PART_BUILDER.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<PartBuilderMenu>> PART_BUILDER_MENU = TinkerRegistries.MENU_TYPES.register(PATH,
            () -> IMenuTypeExtension.create((containerId, playerInventory, buf) -> new PartBuilderMenu(containerId, playerInventory, buf)));

    private PartBuilderRegistry() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        Objects.requireNonNull(PART_BUILDER);
    }

    /**
     * Subscribes a {@link BuildCreativeModeTabContentsEvent} listener that pushes the
     * block-item into {@link SharedTabs#GENERAL}.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(PartBuilderRegistry::populateCreativeTab);
    }

    /** Test seam: visits the table's {@link ItemLike} representation. */
    public static void acceptItems(Consumer<ItemLike> accept) {
        accept.accept(PART_BUILDER_ITEM.get());
    }

    @SubscribeEvent
    private static void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (SharedTabs.GENERAL.getKey().equals(event.getTabKey()) || SharedTabs.TOOLS.getKey().equals(event.getTabKey())) {
            acceptItems(event::accept);
        }
    }

    /** Returns the registered {@link Item} count this registry owns. Test seam. */
    static int registeredItemCount() {
        // BlockItem = 1.
        return 1;
    }
}
