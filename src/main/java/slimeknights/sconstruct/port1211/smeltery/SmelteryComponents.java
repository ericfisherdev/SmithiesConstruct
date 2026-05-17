package slimeknights.sconstruct.port1211.smeltery;

import java.util.List;
import java.util.Objects;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.smeltery.block.SearedChuteBlock;
import slimeknights.sconstruct.port1211.smeltery.block.SearedDrainBlock;
import slimeknights.sconstruct.port1211.smeltery.block.SearedTankGaugeBlock;
import slimeknights.sconstruct.port1211.smeltery.block.SearedTankInBlock;
import slimeknights.sconstruct.port1211.smeltery.block.SearedTankIoBlock;
import slimeknights.sconstruct.port1211.smeltery.block.SmelteryControllerBlock;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SearedTankBE;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryComponentBlockEntity;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryControllerBlockEntity;
import slimeknights.sconstruct.port1211.smeltery.inventory.SmelteryControllerMenu;

/**
 * Registration hub for the six functional blocks of the multiblock smeltery (SMTCON-112) — the
 * controller and its five component blocks (tank IO, tank in, gauge, drain, chute). Owns, for
 * each of the six, a {@link Block}, a block-item, and a {@link BlockEntityType}; eighteen
 * registered objects in all.
 *
 * <p>Every component block is backed by the shared {@link SmelteryComponentBlockEntity} — one BE
 * class registered under five distinct {@link BlockEntityType}s so each component keeps its own
 * type identity for the per-component capability / renderer wiring landing in SMTCON-116/118.
 * The five component BE-type factories are lambdas that read their own {@code DeferredHolder}
 * field back via {@code .get()}: this self-reference is legal because the lambda is not invoked
 * at registration time but later, each time a BE is constructed, by which point the holder is
 * fully resolved. The controller's BE is a distinct skeleton type ({@link SmelteryControllerBlockEntity}).
 *
 * <p>This hub mirrors the {@code PatternChestRegistry} / {@code SearedBlocks} pattern: typed
 * {@code public static final} holders for direct downstream references, a private property
 * helper folding the shared block settings, a frozen {@link #ALL} list as the single iteration
 * surface for the future tag / loot / model / lang providers, and an {@link #init()} no-op that
 * forces the static initialisers to run during mod construction. The wiring lives inline-driven
 * from {@code SConstruct} for now; it relocates into the smeltery pulse's {@code register()} at
 * SMTCON-131.
 */
public final class SmelteryComponents {

    /** Hardness shared by every smeltery component block — stone tier. */
    private static final float HARDNESS = 3.5F;

    /** Blast resistance shared by every smeltery component block. */
    private static final float RESISTANCE = 9.0F;

    /** The smeltery controller — the multiblock's brain block, a full-cube directional block. */
    public static final DeferredBlock<SmelteryControllerBlock> SMELTERY_CONTROLLER = TinkerRegistries.BLOCKS.register("smeltery_controller", () -> new SmelteryControllerBlock(componentProperties()));
    /** Seared tank IO — the larger 4000mb in/out storage tank (capacity wired in SMTCON-116). */
    public static final DeferredBlock<SearedTankIoBlock> SEARED_TANK_IO = TinkerRegistries.BLOCKS.register("seared_tank_io", () -> new SearedTankIoBlock(componentProperties()));
    /** Seared tank in — the smaller 2000mb input-only tank (capacity wired in SMTCON-116). */
    public static final DeferredBlock<SearedTankInBlock> SEARED_TANK_IN = TinkerRegistries.BLOCKS.register("seared_tank_in", () -> new SearedTankInBlock(componentProperties()));
    /** Seared tank gauge — the translucent fluid-gauge panel; {@code noOcclusion()} for cutout render. */
    public static final DeferredBlock<SearedTankGaugeBlock> SEARED_TANK_GAUGE = TinkerRegistries.BLOCKS.register("seared_tank_gauge", () -> new SearedTankGaugeBlock(gaugeProperties()));
    /** Seared drain — the output spout exposing an {@code IFluidHandler} (wired in SMTCON-118). */
    public static final DeferredBlock<SearedDrainBlock> SEARED_DRAIN = TinkerRegistries.BLOCKS.register("seared_drain", () -> new SearedDrainBlock(componentProperties()));
    /** Seared chute — the item input forwarding stacks into the controller (wired in SMTCON-118). */
    public static final DeferredBlock<SearedChuteBlock> SEARED_CHUTE = TinkerRegistries.BLOCKS.register("seared_chute", () -> new SearedChuteBlock(componentProperties()));

    /** Block-item for the smeltery controller. */
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> SMELTERY_CONTROLLER_ITEM = TinkerRegistries.ITEMS
            .registerSimpleBlockItem(SMELTERY_CONTROLLER);
    /** Block-item for the seared tank IO. */
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> SEARED_TANK_IO_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(SEARED_TANK_IO);
    /** Block-item for the seared tank in. */
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> SEARED_TANK_IN_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(SEARED_TANK_IN);
    /** Block-item for the seared tank gauge. */
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> SEARED_TANK_GAUGE_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(SEARED_TANK_GAUGE);
    /** Block-item for the seared drain. */
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> SEARED_DRAIN_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(SEARED_DRAIN);
    /** Block-item for the seared chute. */
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> SEARED_CHUTE_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(SEARED_CHUTE);

    /**
     * Controller block-entity type. Skeleton BE for now ({@link SmelteryControllerBlockEntity});
     * the state machine and {@code MenuProvider} arrive in SMTCON-114/125.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmelteryControllerBlockEntity>> SMELTERY_CONTROLLER_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register("smeltery_controller",
            () -> BlockEntityType.Builder.of(SmelteryControllerBlockEntity::new, SMELTERY_CONTROLLER.get()).build(null));
    /**
     * Tank IO block-entity type. The factory captures this very holder so the {@link SearedTankBE}
     * is constructed bound to this distinct type — the lambda runs at BE-construction time, after
     * the holder has resolved. The two tank blocks back a {@link SearedTankBE} (SMTCON-118), not
     * the bare {@link SmelteryComponentBlockEntity}, because a tank owns real fluid storage.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SearedTankBE>> TANK_IO_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register("seared_tank_io",
            () -> BlockEntityType.Builder.of((pos, state) -> new SearedTankBE(SmelteryComponents.TANK_IO_BE.get(), pos, state), SEARED_TANK_IO.get()).build(null));
    /** Tank in block-entity type. See {@link #TANK_IO_BE} for the deferred self-read pattern. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SearedTankBE>> TANK_IN_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register("seared_tank_in",
            () -> BlockEntityType.Builder.of((pos, state) -> new SearedTankBE(SmelteryComponents.TANK_IN_BE.get(), pos, state), SEARED_TANK_IN.get()).build(null));
    /** Tank gauge block-entity type. See {@link #TANK_IO_BE} for the deferred self-read pattern. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmelteryComponentBlockEntity>> TANK_GAUGE_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register("seared_tank_gauge",
            () -> BlockEntityType.Builder.of((pos, state) -> new SmelteryComponentBlockEntity(SmelteryComponents.TANK_GAUGE_BE.get(), pos, state), SEARED_TANK_GAUGE.get()).build(null));
    /** Drain block-entity type. See {@link #TANK_IO_BE} for the deferred self-read pattern. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmelteryComponentBlockEntity>> DRAIN_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register("seared_drain",
            () -> BlockEntityType.Builder.of((pos, state) -> new SmelteryComponentBlockEntity(SmelteryComponents.DRAIN_BE.get(), pos, state), SEARED_DRAIN.get()).build(null));
    /** Chute block-entity type. See {@link #TANK_IO_BE} for the deferred self-read pattern. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmelteryComponentBlockEntity>> CHUTE_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register("seared_chute",
            () -> BlockEntityType.Builder.of((pos, state) -> new SmelteryComponentBlockEntity(SmelteryComponents.CHUTE_BE.get(), pos, state), SEARED_CHUTE.get()).build(null));

    /**
     * Menu type for the smeltery controller GUI (SMTCON-125). The client factory reads the
     * controller's {@link net.minecraft.core.BlockPos} from the open-screen buffer to bind the
     * menu to its block-entity.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<SmelteryControllerMenu>> SMELTERY_CONTROLLER_MENU = TinkerRegistries.MENU_TYPES.register("smeltery_controller",
            () -> IMenuTypeExtension.create(SmelteryControllerMenu::new));

    /**
     * Immutable insertion-ordered view over all six smeltery component blocks. The single
     * iteration surface for the future tag / loot / blockstate / lang providers.
     */
    public static final List<DeferredBlock<? extends Block>> ALL = List.of(SMELTERY_CONTROLLER, SEARED_TANK_IO, SEARED_TANK_IN, SEARED_TANK_GAUGE, SEARED_DRAIN, SEARED_CHUTE);

    private SmelteryComponents() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // Touch a field so the static initialiser fires. Same touch-to-load pattern as
        // PatternChestRegistry#init / SearedBlocks#init.
        Objects.requireNonNull(SMELTERY_CONTROLLER);
    }

    /**
     * Base properties shared by the controller and the solid component blocks — stone-tier:
     * stone map colour, stone footstep sound, iron-pickaxe to drop, strength {@code 3.5}/{@code 9}.
     */
    private static BlockBehaviour.Properties componentProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).requiresCorrectToolForDrops().strength(HARDNESS, RESISTANCE);
    }

    /**
     * Properties for the gauge block — {@link #componentProperties()} plus {@code noOcclusion()}
     * so the translucent gauge panel does not cull the faces of neighbouring blocks; its
     * cutout / translucent render type is declared in the SMTCON-129-style block-model datagen.
     */
    private static BlockBehaviour.Properties gaugeProperties() {
        return componentProperties().noOcclusion();
    }
}
