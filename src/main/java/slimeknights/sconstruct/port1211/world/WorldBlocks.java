package slimeknights.sconstruct.port1211.world;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;
import slimeknights.sconstruct.port1211.world.block.TinkerSlimeBlock;

/**
 * Phase-3 world blocks: the four coloured slime blocks. Each variant ships with the same
 * physical-property bundle (friction {@code 0.8}, slime-block sound, speed factor {@code 0.4},
 * jump factor {@code 0.5}) — what differs between colours is {@link SlimeColor#applyFallEffect}
 * (magma damages on landing, blood heals on landing, blue/purple are vanilla-equivalent). The
 * cross-colour helper {@link #slimeBlock} centralises the registration boilerplate so adding a
 * fifth colour is a single line plus one new {@link SlimeColor} constant.
 *
 * <p>Physical properties mirror vanilla {@code slime_block} where the legacy 1.12 mod did
 * (friction {@code 0.8} and {@link SoundType#SLIME_BLOCK}). {@code speedFactor 0.4} matches
 * vanilla too — the player walks at 40% speed across a slime surface. {@code jumpFactor 0.5}
 * deliberately deviates from vanilla (which leaves jump unchanged) so the legacy 1.12
 * compressed-slime "trampoline" feel — a shorter manual jump traded for a much higher
 * <em>bounce</em>-driven launch — survives in the port. The numbers come from the ticket
 * implementation plan.
 *
 * <p>Each block also registers a matching {@code BlockItem} via
 * {@link slimeknights.sconstruct.port1211.common.TinkerRegistries#ITEMS}. {@link #SLIME_BLOCKS}
 * exposes the registrations keyed by {@link SlimeColor}; {@link #ALL} flattens to a list in
 * declaration order. Downstream providers (lang, blockstate, item model, creative tab,
 * loot) iterate the list rather than reach for the named fields so a new colour lights up
 * every consumer with no provider edit.
 *
 * <p>{@link #init()} forces this class to load during mod construction so the field
 * initialisers run and the {@link TinkerRegistries#BLOCKS}/{@code ITEMS} registers see every
 * entry before their registry events fire. Same pattern as {@link WorldFluids#init()} and
 * {@link slimeknights.sconstruct.port1211.shared.SharedBlocks#init()}.
 */
public final class WorldBlocks {

    private static final Map<SlimeColor, DeferredBlock<TinkerSlimeBlock>> SLIME_BUILDER = new LinkedHashMap<>();

    public static final DeferredBlock<TinkerSlimeBlock> SLIMEBLUE = slimeBlock(SlimeColor.BLUE);
    public static final DeferredBlock<TinkerSlimeBlock> SLIMEPURPLE = slimeBlock(SlimeColor.PURPLE);
    public static final DeferredBlock<TinkerSlimeBlock> SLIMEMAGMA = slimeBlock(SlimeColor.MAGMA);
    public static final DeferredBlock<TinkerSlimeBlock> SLIMEBLOOD = slimeBlock(SlimeColor.BLOOD);

    /**
     * Immutable view over the four slime-block holders keyed by {@link SlimeColor}, in
     * declaration order. Downstream providers iterate this map instead of the named fields so
     * a new colour lights up every consumer (lang, blockstate, item model, loot, creative
     * tab) without a per-provider edit.
     */
    public static final Map<SlimeColor, DeferredBlock<TinkerSlimeBlock>> SLIME_BLOCKS;

    /** Insertion-ordered list view of the slime-block holders — matches {@link #SLIME_BLOCKS} order. */
    public static final List<DeferredBlock<TinkerSlimeBlock>> ALL;

    static {
        // Freeze the maps after every slimeBlock(...) call above has populated the builder.
        // Wrap LinkedHashMap so iteration order matches declaration order — downstream
        // providers rely on that for deterministic visual grouping.
        SLIME_BLOCKS = Collections.unmodifiableMap(new LinkedHashMap<>(SLIME_BUILDER));
        ALL = List.copyOf(SLIME_BUILDER.values());
    }

    private WorldBlocks() {
    }

    /** Forces class load so the static field initialisers run during mod construction. */
    public static void init() {
        // No-op — invoking this method touches the class, which triggers the field-initialiser
        // chain above. Same pattern as {@link WorldFluids#init()}.
    }

    /**
     * Visits each slime block's {@link ItemLike} in declaration order. Used by the world
     * pulse's creative-tab listener so the slime blocks land in the same tab as the rest of
     * the mod's content without coupling the pulse to a specific tab key lookup.
     */
    public static void acceptBlockItems(Consumer<ItemLike> accept) {
        for (DeferredBlock<TinkerSlimeBlock> block : ALL) {
            accept.accept(block.get());
        }
    }

    private static DeferredBlock<TinkerSlimeBlock> slimeBlock(SlimeColor color) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(color.mapColor()).sound(SoundType.SLIME_BLOCK).friction(0.8F).speedFactor(0.4F).jumpFactor(0.5F).noOcclusion();
        DeferredBlock<TinkerSlimeBlock> block = TinkerRegistries.BLOCKS.register("slime_" + color.id() + "_block", () -> new TinkerSlimeBlock(color, properties));
        TinkerRegistries.ITEMS.registerSimpleBlockItem(block);
        SLIME_BUILDER.put(color, block);
        return block;
    }
}
