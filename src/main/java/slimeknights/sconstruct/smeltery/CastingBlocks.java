package slimeknights.sconstruct.smeltery;

import java.util.List;
import java.util.Objects;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.common.TinkerRegistries;
import slimeknights.sconstruct.smeltery.block.CastingBasinBlock;
import slimeknights.sconstruct.smeltery.block.CastingTableBlock;
import slimeknights.sconstruct.smeltery.block.entity.CastingBasinBlockEntity;
import slimeknights.sconstruct.smeltery.block.entity.CastingTableBlockEntity;

/**
 * Registration hub for the two cast-handling smeltery blocks (SMTCON-113) — the casting table
 * and the casting basin. Owns, for each, a {@link Block}, a block-item, and a
 * {@link BlockEntityType}; six registered objects in all.
 *
 * <p>Mirrors the {@code SmelteryComponents} / {@code PatternChestRegistry} pattern: typed
 * {@code public static final} holders for direct downstream references, a shared property
 * helper, a frozen {@link #ALL} list as the iteration surface for the future tag / loot /
 * model / lang providers, and an {@link #init()} no-op that forces the static initialisers to
 * run during mod construction. The {@code Capabilities.FluidHandler.BLOCK} bindings for the two
 * BEs are registered in {@code SmelteryCapabilities}. The wiring is inline-driven from
 * {@code SConstruct} for now; it relocates into the smeltery pulse at SMTCON-131.
 */
public final class CastingBlocks {

    /** The casting table — casts ingot-sized items (288 mB tank). */
    public static final DeferredBlock<CastingTableBlock> CASTING_TABLE = TinkerRegistries.BLOCKS.register("casting_table", () -> new CastingTableBlock(castingProperties()));
    /** The casting basin — casts block-sized items (2592 mB tank). */
    public static final DeferredBlock<CastingBasinBlock> CASTING_BASIN = TinkerRegistries.BLOCKS.register("casting_basin", () -> new CastingBasinBlock(castingProperties()));

    /** Block-item for the casting table. */
    public static final DeferredItem<BlockItem> CASTING_TABLE_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(CASTING_TABLE);
    /** Block-item for the casting basin. */
    public static final DeferredItem<BlockItem> CASTING_BASIN_ITEM = TinkerRegistries.ITEMS.registerSimpleBlockItem(CASTING_BASIN);

    /** Block-entity type for the casting table. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CastingTableBlockEntity>> CASTING_TABLE_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register("casting_table",
            () -> BlockEntityType.Builder.of(CastingTableBlockEntity::new, CASTING_TABLE.get()).build(null));
    /** Block-entity type for the casting basin. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CastingBasinBlockEntity>> CASTING_BASIN_BE = TinkerRegistries.BLOCK_ENTITY_TYPES.register("casting_basin",
            () -> BlockEntityType.Builder.of(CastingBasinBlockEntity::new, CASTING_BASIN.get()).build(null));

    /**
     * Immutable view over both casting blocks. The single iteration surface for the future
     * tag / loot / blockstate / lang providers.
     */
    public static final List<DeferredBlock<? extends Block>> ALL = List.of(CASTING_TABLE, CASTING_BASIN);

    private CastingBlocks() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // Touch a field so the static initialiser fires. Same touch-to-load pattern as
        // SmelteryComponents#init / SearedBlocks#init.
        Objects.requireNonNull(CASTING_TABLE);
    }

    /**
     * Properties shared by the table and basin — stone-tier seared casting surfaces: stone map
     * colour, stone footstep sound, iron-pickaxe to drop, strength {@code 3.0}/{@code 9}.
     */
    private static BlockBehaviour.Properties castingProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).requiresCorrectToolForDrops().strength(3.0F, 9.0F);
    }
}
