package slimeknights.sconstruct.port1211.data;

import java.util.function.BiConsumer;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import slimeknights.sconstruct.port1211.shared.SharedBlocks;
import slimeknights.sconstruct.port1211.shared.SharedItems;
import slimeknights.sconstruct.port1211.world.WorldBlocks;
import slimeknights.sconstruct.port1211.world.WorldStructures;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;
import slimeknights.sconstruct.port1211.world.block.SlimePlantSet;

/**
 * Loot-table sub-provider for the slime-island treasure chest. Implements
 * {@link LootTableSubProvider} directly (rather than extending a chest-loot helper class —
 * vanilla {@code VanillaChestLoot} is also a {@code record … implements LootTableSubProvider})
 * and emits one entry keyed by {@link WorldStructures#SLIME_ISLAND_CHEST_LOOT}.
 *
 * <p>The chest's content is divided into four pools so multiple categories are represented per
 * roll — saplings (rolled 1×, picks a random colour), slimeballs (rolled 2×, picks any colour),
 * dye (rolled 1× from a small palette), and mod-flavour basics (rolled 1×, picks from bacon /
 * mud brick / glow / firewood / lavawood with an {@link EmptyLootItem} weight so empty rolls
 * are common). Counts are deliberately modest — the chest is "loot you'd cheer for", not a
 * netherite-equivalent jackpot.
 */
public record SlimeIslandChestLoot(HolderLookup.Provider registries) implements LootTableSubProvider {

    /** Min slimeball stack rolled by the slimeball pool. */
    private static final int SLIMEBALL_MIN = 2;

    /** Max slimeball stack rolled by the slimeball pool. */
    private static final int SLIMEBALL_MAX = 8;

    /** Min dye stack rolled by the dye pool. */
    private static final int DYE_MIN = 1;

    /** Max dye stack rolled by the dye pool. */
    private static final int DYE_MAX = 4;

    /** Empty-roll weight on the flavour pool — half the time the chest's flavour slot is empty. */
    private static final int FLAVOUR_EMPTY_WEIGHT = 5;

    /** Weight for each individual flavour item; tuned so the total flavour rolls hit roughly half-empty / half-item. */
    private static final int FLAVOUR_ITEM_WEIGHT = 1;

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        LootTable.Builder builder = LootTable.lootTable();

        // Sapling pool — one sapling from the matching-island colour palette.
        LootPool.Builder saplingPool = LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1));
        for (SlimeColor color : SlimeColor.values()) {
            SlimePlantSet plants = WorldBlocks.PLANT_SETS.get(color);
            saplingPool.add(LootItem.lootTableItem(plants.sapling().get()));
        }
        builder.withPool(saplingPool);

        // Slimeballs pool — 2 rolls picking any coloured slimeball; each roll generates
        // SLIMEBALL_MIN..SLIMEBALL_MAX of the picked colour.
        LootPool.Builder slimeballPool = LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(2))
                .add(LootItem.lootTableItem(SharedItems.SLIMEBALL_BLUE.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(SLIMEBALL_MIN, SLIMEBALL_MAX))))
                .add(LootItem.lootTableItem(SharedItems.SLIMEBALL_PURPLE.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(SLIMEBALL_MIN, SLIMEBALL_MAX))))
                .add(LootItem.lootTableItem(SharedItems.SLIMEBALL_MAGMA.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(SLIMEBALL_MIN, SLIMEBALL_MAX))))
                .add(LootItem.lootTableItem(SharedItems.SLIMEBALL_BLOOD.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(SLIMEBALL_MIN, SLIMEBALL_MAX))));
        builder.withPool(slimeballPool);

        // Dye pool — one stack of a vanilla dye that thematically matches the island palette.
        LootPool.Builder dyePool = LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(Items.LIGHT_BLUE_DYE).apply(SetItemCountFunction.setCount(UniformGenerator.between(DYE_MIN, DYE_MAX))))
                .add(LootItem.lootTableItem(Items.PURPLE_DYE).apply(SetItemCountFunction.setCount(UniformGenerator.between(DYE_MIN, DYE_MAX))))
                .add(LootItem.lootTableItem(Items.ORANGE_DYE).apply(SetItemCountFunction.setCount(UniformGenerator.between(DYE_MIN, DYE_MAX))))
                .add(LootItem.lootTableItem(Items.RED_DYE).apply(SetItemCountFunction.setCount(UniformGenerator.between(DYE_MIN, DYE_MAX))));
        builder.withPool(dyePool);

        // Flavour pool — mod-specific basics, balanced against an empty entry so the chest
        // doesn't always reward something exotic. FLAVOUR_EMPTY_WEIGHT vs. FLAVOUR_ITEM_WEIGHT
        // controls the "interesting roll" rate.
        LootPool.Builder flavourPool = LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                .add(EmptyLootItem.emptyItem().setWeight(FLAVOUR_EMPTY_WEIGHT)).add(LootItem.lootTableItem(SharedItems.BACON.get()).setWeight(FLAVOUR_ITEM_WEIGHT))
                .add(LootItem.lootTableItem(SharedItems.MUDBRICK.get()).setWeight(FLAVOUR_ITEM_WEIGHT)).add(LootItem.lootTableItem(SharedBlocks.GLOW.get()).setWeight(FLAVOUR_ITEM_WEIGHT))
                .add(LootItem.lootTableItem(SharedBlocks.FIREWOOD.get()).setWeight(FLAVOUR_ITEM_WEIGHT)).add(LootItem.lootTableItem(SharedBlocks.LAVAWOOD.get()).setWeight(FLAVOUR_ITEM_WEIGHT));
        builder.withPool(flavourPool);

        output.accept(WorldStructures.SLIME_ISLAND_CHEST_LOOT, builder);
    }
}
