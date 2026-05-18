package slimeknights.sconstruct.smeltery;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.sconstruct.common.TinkerRegistries;

/**
 * Registers the smeltery's seared construction blocks — the scorched-stone family the player
 * builds the multiblock smeltery and its decorative surrounds from. Twelve base blocks plus
 * stair and slab variants for the two load-bearing bricks, sixteen blocks in all.
 *
 * <p>Every seared block is a stone-tier block: strength {@code 3.5} hardness / {@code 9}
 * blast resistance, a stone footstep sound, and {@code requiresCorrectToolForDrops()} so an
 * iron pickaxe is needed to recover it. Two blocks break that mould — {@link #SEARED_GLASS}
 * and {@link #SEARED_WINDOW} are {@code noOcclusion()} so neighbouring blocks still render the
 * faces behind them; their translucent / cutout render types are declared in the block-model
 * datagen (SMTCON-129).
 *
 * <p>Mirrors the Phase-2 {@code SharedBlocks} pattern: a private helper folds the per-block
 * boilerplate, each block is exposed as a {@code public static final} field for typed
 * downstream references, and the frozen {@link #ALL} list is the iteration surface for the
 * tag / loot / model / lang providers (SMTCON-129) and any future creative-tab listener.
 *
 * <p>{@link #init()} forces this class to load during mod construction so the field
 * initialisers run and the {@link TinkerRegistries#BLOCKS}/{@code ITEMS} entries are populated
 * before the registry events fire. It is invoked from {@code SConstruct} for now; the call
 * relocates into the smeltery pulse when SMTCON-131 wires it.
 */
public final class SearedBlocks {

    /** Hardness shared by every seared block. */
    private static final float HARDNESS = 3.5F;

    /** Blast resistance shared by every seared block. */
    private static final float RESISTANCE = 9.0F;

    private static final List<DeferredBlock<? extends Block>> BUILDER = new ArrayList<>();

    /** Smooth seared stone — the smeltery's baseline block. */
    public static final DeferredBlock<Block> SEARED_STONE = searedBlock("seared_stone");
    /** Rough seared cobblestone. */
    public static final DeferredBlock<Block> SEARED_COBBLE = searedBlock("seared_cobble");
    /** Flat seared paving — the canonical smeltery floor block. */
    public static final DeferredBlock<Block> SEARED_PAVER = searedBlock("seared_paver");
    /** Plain seared brick — the canonical smeltery wall block. */
    public static final DeferredBlock<Block> SEARED_BRICK = searedBlock("seared_brick");
    /** Chiselled seared brick decorative variant. */
    public static final DeferredBlock<Block> SEARED_BRICK_CHISELED = searedBlock("seared_brick_chiseled");
    /** Squared seared brick decorative variant. */
    public static final DeferredBlock<Block> SEARED_BRICK_SQUARED = searedBlock("seared_brick_squared");
    /** Creeper-face seared brick decorative variant. */
    public static final DeferredBlock<Block> SEARED_BRICK_CREEPER = searedBlock("seared_brick_creeper");
    /** Road-pattern seared brick decorative variant. */
    public static final DeferredBlock<Block> SEARED_BRICK_ROAD = searedBlock("seared_brick_road");
    /** Fancy seared brick decorative variant. */
    public static final DeferredBlock<Block> SEARED_BRICK_FANCY = searedBlock("seared_brick_fancy");
    /** Triangle-pattern seared brick decorative variant. */
    public static final DeferredBlock<Block> SEARED_BRICK_TRIANGLE = searedBlock("seared_brick_triangle");
    /** Seared glass — non-occluding, renders translucent (render type set in SMTCON-129 datagen). */
    public static final DeferredBlock<Block> SEARED_GLASS = searedGlass("seared_glass");
    /** Seared window — non-occluding, renders cutout (render type set in SMTCON-129 datagen). */
    public static final DeferredBlock<Block> SEARED_WINDOW = searedGlass("seared_window");

    /** Stairs for the canonical smeltery wall block. */
    public static final DeferredBlock<StairBlock> SEARED_BRICK_STAIRS = searedStairs("seared_brick_stairs", SEARED_BRICK);
    /** Slab for the canonical smeltery wall block. */
    public static final DeferredBlock<SlabBlock> SEARED_BRICK_SLAB = searedSlab("seared_brick_slab");
    /** Stairs for the canonical smeltery floor block. */
    public static final DeferredBlock<StairBlock> SEARED_PAVER_STAIRS = searedStairs("seared_paver_stairs", SEARED_PAVER);
    /** Slab for the canonical smeltery floor block. */
    public static final DeferredBlock<SlabBlock> SEARED_PAVER_SLAB = searedSlab("seared_paver_slab");

    /**
     * Immutable insertion-ordered view over every registered seared block. The single iteration
     * surface for the SMTCON-129 tag / loot / blockstate / lang providers.
     */
    public static final List<DeferredBlock<? extends Block>> ALL = List.copyOf(BUILDER);

    private SearedBlocks() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. Same pattern as {@code SharedBlocks#init()}.
    }

    /** Base properties every seared block shares — stone-tier, iron pickaxe to drop. */
    private static BlockBehaviour.Properties searedProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).requiresCorrectToolForDrops().strength(HARDNESS, RESISTANCE);
    }

    /** Register a solid seared block plus its {@link net.minecraft.world.item.BlockItem}. */
    private static DeferredBlock<Block> searedBlock(String id) {
        DeferredBlock<Block> block = TinkerRegistries.BLOCKS.registerSimpleBlock(id, searedProperties());
        TinkerRegistries.ITEMS.registerSimpleBlockItem(block);
        BUILDER.add(block);
        return block;
    }

    /**
     * Register a non-occluding seared block (glass / window). {@code noOcclusion()} stops the
     * block from culling the faces of whatever sits behind it; the glass sound replaces the
     * stone footstep. The translucent / cutout render type is a model-JSON concern handled by
     * the SMTCON-129 datagen.
     */
    private static DeferredBlock<Block> searedGlass(String id) {
        DeferredBlock<Block> block = TinkerRegistries.BLOCKS.registerSimpleBlock(id,
                BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.GLASS).requiresCorrectToolForDrops().strength(HARDNESS, RESISTANCE).noOcclusion());
        TinkerRegistries.ITEMS.registerSimpleBlockItem(block);
        BUILDER.add(block);
        return block;
    }

    /**
     * Register a seared stair block keyed to {@code base}'s default state. The {@code base}
     * holder resolves through {@code get()} inside the registration lambda — by which point the
     * blocks registry has constructed the base block — so the declaration order (base field
     * above the stair field) is the only constraint.
     */
    private static DeferredBlock<StairBlock> searedStairs(String id, DeferredBlock<Block> base) {
        DeferredBlock<StairBlock> block = TinkerRegistries.BLOCKS.register(id, () -> new StairBlock(base.get().defaultBlockState(), searedProperties()));
        TinkerRegistries.ITEMS.registerSimpleBlockItem(block);
        BUILDER.add(block);
        return block;
    }

    /** Register a seared slab block plus its {@link net.minecraft.world.item.BlockItem}. */
    private static DeferredBlock<SlabBlock> searedSlab(String id) {
        DeferredBlock<SlabBlock> block = TinkerRegistries.BLOCKS.register(id, () -> new SlabBlock(searedProperties()));
        TinkerRegistries.ITEMS.registerSimpleBlockItem(block);
        BUILDER.add(block);
        return block;
    }
}
