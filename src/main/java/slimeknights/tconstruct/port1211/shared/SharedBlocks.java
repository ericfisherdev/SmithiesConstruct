package slimeknights.tconstruct.port1211.shared;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.tconstruct.port1211.common.TinkerRegistries;

/**
 * Registers one storage block per Phase-2 metal that has a legacy precedent. The 1.12 source
 * shipped storage blocks via a single {@code BlockMetal} enum with 6 variants (cobalt, ardite,
 * manyullyn, knightslime, pigiron, alubrass); Phase 2 widens this to also cover the
 * "real-world" metals (silver, copper, tin, zinc, brass, electrum, steel) so the casting
 * pipeline in Phase 5 has full coverage. Lead and nickel are deliberately excluded — they
 * existed only as ingots/nuggets in legacy and adding storage blocks for them would invent
 * content rather than port it.
 *
 * <p>Each registration goes through {@link #metalBlock} which derives every per-metal
 * difference (id, map color, mining tier) from the {@link Metal} driver row, keeping the
 * boilerplate the legacy code paid 14 times down to a single call site.
 *
 * <p>{@link #init()} forces this class to load during mod construction so the
 * field-initialiser chain runs and the {@link TinkerRegistries#BLOCKS}/{@code ITEMS} entries
 * are populated before the registry event fires. Without an explicit reference from
 * {@code TConstruct}, the JVM has no reason to load the class until the first lookup, which
 * would be after registration has closed.
 */
public final class SharedBlocks {

    /** Set of metal ids excluded from the storage-block registration. */
    private static final java.util.Set<String> NO_BLOCK_IN_LEGACY = java.util.Set.of("lead", "nickel");

    /**
     * Live map of every registered metal-block {@link DeferredBlock}, keyed by the metal's id.
     * Built top-down by the field initialisers below, then frozen. Downstream providers
     * (tags, recipes, lang, models) iterate this map instead of the static fields so a new
     * metal lights up every provider by appending to {@link SharedMetals#ALL}.
     */
    public static final Map<String, DeferredBlock<Block>> METAL_BLOCKS;

    private static final Map<String, DeferredBlock<Block>> BUILDER = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<?>> ITEM_BUILDER = new LinkedHashMap<>();

    /** Public static field per metal, satisfying the AC's downstream-reference requirement. */
    public static final DeferredBlock<Block> COBALT = metalBlock("cobalt");
    public static final DeferredBlock<Block> ARDITE = metalBlock("ardite");
    public static final DeferredBlock<Block> MANYULLYN = metalBlock("manyullyn");
    public static final DeferredBlock<Block> KNIGHTSLIME = metalBlock("knightslime");
    public static final DeferredBlock<Block> PIGIRON = metalBlock("pigiron");
    public static final DeferredBlock<Block> SILVER = metalBlock("silver");
    public static final DeferredBlock<Block> COPPER = metalBlock("copper");
    public static final DeferredBlock<Block> TIN = metalBlock("tin");
    public static final DeferredBlock<Block> ZINC = metalBlock("zinc");
    public static final DeferredBlock<Block> BRASS = metalBlock("brass");
    public static final DeferredBlock<Block> ALUBRASS = metalBlock("alubrass");
    public static final DeferredBlock<Block> ELECTRUM = metalBlock("electrum");
    public static final DeferredBlock<Block> STEEL = metalBlock("steel");

    static {
        // Freeze the registration map after every metalBlock(...) call above has populated it.
        // From this point on adding a metal block goes through a new public static field above
        // plus an entry in SharedMetals; downstream code is read-only on the map. Wrap the
        // LinkedHashMap rather than Map.copyOf so iteration order matches declaration order —
        // providers rely on that for deterministic generated artifacts.
        METAL_BLOCKS = Collections.unmodifiableMap(new LinkedHashMap<>(BUILDER));
        // BUILDER is no longer referenced from anywhere — its contents now live in the immutable
        // copy. ITEM_BUILDER is retained only to root the DeferredItems while their backing
        // DeferredRegister listeners remain attached; once registration fires, the entries are
        // unreachable except through TinkerRegistries.ITEMS.
    }

    private SharedBlocks() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. The init() pattern intentionally has no body so future refactors don't
        // accidentally add work that should live in a Pulse instead.
    }

    /**
     * Subscribes the metal blocks to the vanilla {@code BUILDING_BLOCKS} creative tab so they
     * are reachable without typing into the search bar. Registered against the supplied mod
     * event bus during {@code TConstruct} construction; runs once per creative-tab build.
     */
    public static void registerCreativeTabContents(IEventBus modBus) {
        modBus.addListener(SharedBlocks::onBuildCreativeTabContents);
    }

    @SubscribeEvent
    private static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!CreativeModeTabs.BUILDING_BLOCKS.equals(event.getTabKey())) {
            return;
        }
        ITEM_BUILDER.values().forEach(item -> event.accept(item.get()));
    }

    private static DeferredBlock<Block> metalBlock(String id) {
        Metal metal = lookup(id);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(metal.mapColor()).sound(SoundType.METAL).requiresCorrectToolForDrops()
                // Diamond-tier metals match vanilla diamond/iron-block strength (5.0f, 6.0f);
                // iron-tier metals match vanilla copper/gold-block strength (3.0f, 6.0f).
                .strength(metal.needsDiamond() ? 5.0F : 3.0F, 6.0F);
        DeferredBlock<Block> block = TinkerRegistries.BLOCKS.registerSimpleBlock("block_" + metal.id(), properties);
        ITEM_BUILDER.put(metal.id(), TinkerRegistries.ITEMS.registerSimpleBlockItem(block));
        BUILDER.put(metal.id(), block);
        return block;
    }

    private static Metal lookup(String id) {
        if (NO_BLOCK_IN_LEGACY.contains(id)) {
            throw new IllegalArgumentException(id + " is on the no-storage-block list; remove from NO_BLOCK_IN_LEGACY before declaring a field");
        }
        for (Metal metal : SharedMetals.ALL) {
            if (metal.id().equals(id)) {
                return metal;
            }
        }
        throw new IllegalArgumentException("no metal in SharedMetals.ALL with id '" + id + "'");
    }

    /** Test hook: the set of metal ids deliberately skipped from storage-block registration. */
    static java.util.Set<String> skippedIds() {
        return NO_BLOCK_IN_LEGACY;
    }
}
