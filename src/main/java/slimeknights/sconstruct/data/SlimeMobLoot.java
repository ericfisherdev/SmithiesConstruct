package slimeknights.sconstruct.data;

import java.util.stream.Stream;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import slimeknights.sconstruct.shared.SharedItems;
import slimeknights.sconstruct.world.WorldEntities;

/**
 * Loot table sub-provider for the Phase-3 slime mob entity types. Blueslime drops 1-3 blue
 * slimeballs on kill; huge slime drops 4-9 blue slimeballs plus 1-3 bone meal — the bone meal
 * is the boss reward that lets the player advance the sapling growth they collect from the
 * same island.
 *
 * <p>Drop counts come from the SMTCON-60 implementation plan; pinned as private constants so a
 * future tuning bump flows through one edit. Per-size scaling (vanilla slime uses the size
 * parameter to multiply drop counts) is intentionally not modelled — these are the SMTCON-56
 * fixed-size entity types, not vanilla {@code Slime}, so the size loop in vanilla loot
 * doesn't apply.
 */
public final class SlimeMobLoot extends EntityLootSubProvider {

    /** Lower bound of blue slimeballs dropped by a blueslime kill. */
    private static final int BLUESLIME_BALLS_MIN = 1;

    /** Upper bound of blue slimeballs dropped by a blueslime kill. Inclusive. */
    private static final int BLUESLIME_BALLS_MAX = 3;

    /** Lower bound of blue slimeballs dropped by a huge slime kill — bigger than a blueslime's max so the boss feels worth fighting. */
    private static final int HUGESLIME_BALLS_MIN = 4;

    /** Upper bound of blue slimeballs dropped by a huge slime kill. Inclusive. */
    private static final int HUGESLIME_BALLS_MAX = 9;

    /** Lower bound of bone meal dropped by a huge slime kill — the boss reward stack. */
    private static final int HUGESLIME_BONEMEAL_MIN = 1;

    /** Upper bound of bone meal dropped by a huge slime kill. Inclusive. */
    private static final int HUGESLIME_BONEMEAL_MAX = 3;

    public SlimeMobLoot(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    public void generate() {
        add(WorldEntities.BLUESLIME.get(), LootTable.lootTable().withPool(LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(SharedItems.SLIMEBALL_BLUE.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(BLUESLIME_BALLS_MIN, BLUESLIME_BALLS_MAX))))));

        add(WorldEntities.HUGESLIME.get(),
                LootTable.lootTable()
                        .withPool(LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                                .add(LootItem.lootTableItem(SharedItems.SLIMEBALL_BLUE.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(HUGESLIME_BALLS_MIN, HUGESLIME_BALLS_MAX)))))
                        .withPool(LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                                .add(LootItem.lootTableItem(Items.BONE_MEAL).apply(SetItemCountFunction.setCount(UniformGenerator.between(HUGESLIME_BONEMEAL_MIN, HUGESLIME_BONEMEAL_MAX))))));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        // Restrict the parent's validation pass to just the mod's slime entity types. Without
        // overriding this, EntityLootSubProvider iterates every registered EntityType and
        // expects a loot table for each — vanilla mobs would fail the check.
        return Stream.of(WorldEntities.BLUESLIME.get(), WorldEntities.HUGESLIME.get());
    }
}
