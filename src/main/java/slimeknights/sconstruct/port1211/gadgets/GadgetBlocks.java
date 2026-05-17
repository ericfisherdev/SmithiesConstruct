package slimeknights.sconstruct.port1211.gadgets;

import java.util.List;
import java.util.Objects;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.gadgets.block.DryingRackBlock;
import slimeknights.sconstruct.port1211.gadgets.block.entity.DryingRackBlockEntity;
import slimeknights.sconstruct.port1211.shared.SharedTabs;

/**
 * Registration hub for the Phase-6 gadget blocks. SMTCON-137 registers the first: the
 * {@link DryingRackBlock}, with its block-item and block-entity type.
 *
 * <p>Mirrors the {@code CastingBlocks} pattern — typed {@code public static final} holders, a
 * frozen {@link #ALL} list as the iteration surface for the future tag / loot / model / lang
 * providers, and an {@link #init()} no-op that forces the static initialisers to run during mod
 * construction. Wired inline from {@code SConstruct} for now; it relocates into the gadgets
 * pulse when that pulse is wired.
 */
public final class GadgetBlocks {

    /** The drying rack — holds an item and dries it into another over time. */
    public static final DeferredBlock<DryingRackBlock> DRYING_RACK = TinkerRegistries.BLOCKS.register("drying_rack", () -> new DryingRackBlock(dryingRackProperties()));

    /** Block-item for the drying rack. */
    public static final DeferredItem<BlockItem> DRYING_RACK_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(DRYING_RACK);

    /** Block-entity type for the drying rack. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DryingRackBlockEntity>> DRYING_RACK_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register("drying_rack",
            () -> BlockEntityType.Builder.of(DryingRackBlockEntity::new, DRYING_RACK.get()).build(null));

    /** Immutable view over every gadget block — the iteration surface for data providers. */
    public static final List<DeferredBlock<? extends Block>> ALL = List.of(DRYING_RACK);

    private GadgetBlocks() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        Objects.requireNonNull(DRYING_RACK);
    }

    /**
     * Subscribes a {@link BuildCreativeModeTabContentsEvent} listener that appends every gadget
     * block-item to {@link SharedTabs#GENERAL}. Called from {@code SConstruct} during mod
     * construction.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(GadgetBlocks::populateCreativeTab);
    }

    private static void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!SharedTabs.GENERAL.getKey().equals(event.getTabKey())) {
            return;
        }
        event.accept(DRYING_RACK_ITEM.get());
    }

    /** Properties for the drying rack — a light wooden block, axe-mined, hand-breakable. */
    private static BlockBehaviour.Properties dryingRackProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0F).noOcclusion();
    }
}
