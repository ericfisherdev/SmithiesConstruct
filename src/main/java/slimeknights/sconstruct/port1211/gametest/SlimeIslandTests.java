package slimeknights.sconstruct.port1211.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import slimeknights.sconstruct.port1211.shared.SharedItems;
import slimeknights.sconstruct.port1211.world.WorldBlocks;
import slimeknights.sconstruct.port1211.world.WorldEntities;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * Phase-3 in-world acceptance tests. Three {@link GameTest} cases prove the slime island
 * block set, the blueslime entity wiring, and the bouncy slime block physics work end-to-end
 * inside a running NeoForge {@code gameTestServer}.
 *
 * <p>All three tests share the {@code gametest_7x7x7} air template — a hand-built NBT
 * platform large enough to spawn entities and observe a short fall. The shared template
 * keeps the resource cost down (one .nbt instead of three) at the price of running each
 * scenario in a clean air cube rather than a pre-populated island; the worldgen replay AC
 * is covered by the unit-level {@code WorldStructuresTest} in SMTCON-58, and these tests
 * verify the runtime block / entity / physics surface the player actually interacts with.
 */
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SlimeIslandTests {

    private static final String TEMPLATE = "gametest_7x7x7";

    private SlimeIslandTests() {
    }

    /**
     * Test 1: place representative slime-island blocks (blue grass + blue dirt) in-world via
     * {@link GameTestHelper#setBlock} and assert they survive the level set + read-back round
     * trip. Verifies the Phase-3 block registry → blockstate JSON → in-world block round-trip
     * works inside a real server, which the unit-level provider tests cannot exercise.
     */
    @GameTest(template = TEMPLATE)
    public static void placeSlimeIsland(GameTestHelper helper) {
        BlockPos grassPos = new BlockPos(3, 1, 3);
        BlockPos dirtPos = new BlockPos(3, 1, 4);
        helper.setBlock(grassPos, WorldBlocks.PLANT_SETS.get(SlimeColor.BLUE).grass().get());
        helper.setBlock(dirtPos, WorldBlocks.PLANT_SETS.get(SlimeColor.BLUE).dirt().get());
        helper.assertBlockPresent(WorldBlocks.PLANT_SETS.get(SlimeColor.BLUE).grass().get(), grassPos);
        helper.assertBlockPresent(WorldBlocks.PLANT_SETS.get(SlimeColor.BLUE).dirt().get(), dirtPos);
        helper.succeed();
    }

    /**
     * Test 2: spawn a blueslime, kill it, and assert the death drops at least one
     * {@code slimeball_blue} ItemEntity nearby. Pins the entity registration + loot table
     * wiring as a single in-world check.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void blueslimeSpawnsAndDrops(GameTestHelper helper) {
        BlockPos spawnPos = new BlockPos(3, 2, 3);
        Slime slime = helper.spawnWithNoFreeWill(WorldEntities.BLUESLIME.get(), spawnPos);
        // setHealth(0) bypasses death damage logic; hurt with a finite amount instead so
        // vanilla's onDeath -> dropFromLootTable path executes and the slimeball drop fires.
        slime.hurt(helper.getLevel().damageSources().generic(), Float.MAX_VALUE);
        helper.succeedWhen(() -> {
            BlockPos absolute = helper.absolutePos(spawnPos);
            AABB area = new AABB(absolute).inflate(6.0);
            List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
            ItemStack expected = new ItemStack(SharedItems.SLIMEBALL_BLUE.get());
            boolean found = items.stream().anyMatch(item -> ItemStack.isSameItem(item.getItem(), expected));
            if (!found) {
                helper.fail("no slimeball_blue ItemEntity found near blueslime death position");
            }
        });
    }

    /**
     * Test 3: drop a {@link Pig} onto a placed blue slime block and assert the block reverses
     * the entity's Y velocity. Pigs are vanilla, hostile-mob-immune for spawn purposes, and
     * obey the same {@code SlimeBlock#fallOn} hook the player triggers, so a positive Y
     * delta on the first post-impact tick proves the bouncy-block physics surface is wired.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void bouncyBlockBounces(GameTestHelper helper) {
        BlockPos blockPos = new BlockPos(3, 1, 3);
        BlockPos pigPos = new BlockPos(3, 5, 3);
        helper.setBlock(blockPos, WorldBlocks.SLIME_BLOCKS.get(SlimeColor.BLUE).get());
        Pig pig = helper.spawn(EntityType.PIG, pigPos);
        // Leave AI enabled — {@code setNoAi(true)} also freezes the entity tick (NoAI is the
        // vanilla "freeze entirely" flag, not "skip goals only"), which would suppress
        // gravity and prevent any fall from happening.
        // Bouncing produces a transient positive Y velocity for only a few ticks after each
        // impact, so a single-shot check would race the physics. Sample every tick for a
        // generous window and record the peak — any positive sample proves the bounce hook
        // fired, regardless of where the test happens to poll in the impact cycle.
        final double bounceVelocityThreshold = 0.0;
        double[] maxYVelocity = { Double.NEGATIVE_INFINITY };
        helper.startSequence().thenExecuteFor(80, () -> {
            double vy = pig.getDeltaMovement().y;
            if (vy > maxYVelocity[0]) {
                maxYVelocity[0] = vy;
            }
        }).thenExecute(() -> {
            if (maxYVelocity[0] <= bounceVelocityThreshold) {
                helper.fail("expected positive Y velocity (slime-block bounce) at some point in 80 ticks, peak observed=" + maxYVelocity[0]);
            }
        }).thenSucceed();
    }
}
