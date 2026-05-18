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
import slimeknights.sconstruct.port1211.tools.block.PatternChestBlock;
import slimeknights.sconstruct.port1211.tools.block.entity.PatternChestBlockEntity;
import slimeknights.sconstruct.port1211.tools.inventory.PatternChestMenu;

/**
 * Registration hub for the 32-slot Pattern Chest (SMTCON-90) — owns the block, its block-item,
 * the matching {@link BlockEntityType}, and the {@link MenuType} that pairs the BE to its
 * screen.
 *
 * <p>The four {@link DeferredHolder} singletons are held as {@code public static final} fields
 * so downstream callers (capabilities wiring, client setup, datagen) reference each registered
 * object by typed handle without map lookups. The {@link #init()} method touches the class to
 * trigger the field initialisers during mod construction — same pattern as
 * {@link slimeknights.sconstruct.port1211.tools.item.ToolItems}.
 *
 * <p>The wiring lives in {@code tools/} (the chest is part of the tools pulse — it stores tool
 * patterns) and will move into {@code TinkerToolsPulse#register} alongside
 * {@link slimeknights.sconstruct.port1211.tools.item.ToolItems} when that pulse lands. Until
 * then, {@link slimeknights.sconstruct.port1211.SConstruct} drives both {@link #init()} and
 * {@link #registerCreativeTabContents(IEventBus)} directly.
 */
public final class PatternChestRegistry {

    /** Registry path for the pattern chest block, BE, and menu — single source of truth. */
    public static final String PATH = "pattern_chest";

    public static final DeferredBlock<PatternChestBlock> PATTERN_CHEST = TinkerRegistries.BLOCKS.register(PATH, () -> new PatternChestBlock(PatternChestBlock.defaultProperties()));

    /**
     * Block-item form of the chest. {@code DeferredRegister.Items#registerSimpleBlockItem}
     * would produce a {@link BlockItem} with default {@link Item.Properties}; spelling it out
     * here keeps the construction explicit and gives a single place to add tab- or stack-size
     * tweaks if needed in a follow-up.
     */
    public static final DeferredItem<BlockItem> PATTERN_CHEST_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(PATTERN_CHEST);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternChestBlockEntity>> PATTERN_CHEST_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register(PATH,
            // Builder.of is the safe ctor surface in 1.21.1 — sidesteps the per-version drift
            // in BlockEntityType's direct ctor signature (with-or-without dataFixerType param).
            () -> BlockEntityType.Builder.of(PatternChestBlockEntity::new, PATTERN_CHEST.get()).build(null));

    /**
     * Menu type for the pattern chest. Uses {@link IMenuTypeExtension#create} so the factory
     * gets the {@code FriendlyByteBuf} extra-data buffer needed by
     * {@link PatternChestMenu}'s client constructor to locate the BE through its position.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<PatternChestMenu>> PATTERN_CHEST_MENU = TinkerRegistries.MENU_TYPES.register(PATH,
            () -> IMenuTypeExtension.create((containerId, playerInventory, buf) -> new PatternChestMenu(containerId, playerInventory, buf)));

    private PatternChestRegistry() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // Touch a field so the static initialiser fires. Same pattern as ToolItems#init.
        Objects.requireNonNull(PATTERN_CHEST);
    }

    /**
     * Subscribes a {@link BuildCreativeModeTabContentsEvent} listener that pushes the chest
     * block-item into {@link SharedTabs#GENERAL}. Mirrors the
     * {@link slimeknights.sconstruct.port1211.tools.item.ToolItems#registerCreativeTabContents}
     * call shape so the future {@code TinkerToolsPulse} can pick up both with one call.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(PatternChestRegistry::populateCreativeTab);
    }

    /** Test seam: visits the chest's {@link ItemLike} representation. */
    public static void acceptItem(Consumer<ItemLike> accept) {
        accept.accept(PATTERN_CHEST_ITEM.get());
    }

    @SubscribeEvent
    private static void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (SharedTabs.GENERAL.getKey().equals(event.getTabKey()) || SharedTabs.TOOLS.getKey().equals(event.getTabKey())) {
            acceptItem(event::accept);
        }
    }
}
