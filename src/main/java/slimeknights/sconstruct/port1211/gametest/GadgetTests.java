package slimeknights.sconstruct.port1211.gametest;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import slimeknights.sconstruct.port1211.gadgets.GadgetBlocks;
import slimeknights.sconstruct.port1211.gadgets.GadgetItems;
import slimeknights.sconstruct.port1211.gadgets.block.entity.DryingRackBlockEntity;
import slimeknights.sconstruct.port1211.gadgets.entity.GadgetEntities;
import slimeknights.sconstruct.port1211.gadgets.entity.ThrowballEntity;
import slimeknights.sconstruct.port1211.gadgets.recipe.DryingRecipe;
import slimeknights.sconstruct.port1211.gadgets.recipe.GadgetRecipes;

/**
 * End-to-end {@link GameTest}s for the Phase-6 gadgets (SMTCON-145) — three cases proving the
 * throwball impact effect, the drying rack's recipe progression, and the slimesling launch
 * against a live server world.
 *
 * <p>Each case builds its setup programmatically in the shared empty {@code gametest_7x7x7}
 * template and drives the relevant entity / block entity / item synchronously, so every case
 * is deterministic.
 */
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class GadgetTests {

    private static final String TEMPLATE = "gametest_7x7x7";

    /** Tick budget for a throwball / drying loop — larger than any duration these tests need. */
    private static final int TICK_BUDGET = 600;

    private GadgetTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void throwballSlowsAZombie(GameTestHelper helper) {
        // A small stone floor gives the thrown projectile a deterministic surface to strike.
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));

        // Spawn a blue throwball just above the zombie and let it fall onto the floor — the
        // throwball's impact applies an area slowness effect to every living entity in range.
        ThrowballEntity throwball = helper.spawn(GadgetEntities.THROWBALL.get(), new BlockPos(3, 4, 3));
        throwball.setItem(new ItemStack(GadgetItems.THROWBALL_BLUE.get()));
        throwball.setDeltaMovement(0.0D, -0.3D, 0.0D);

        for (int tick = 0; tick < TICK_BUDGET && !throwball.isRemoved(); tick++) {
            throwball.tick();
        }

        helper.assertTrue(throwball.isRemoved(), "the throwball is consumed on impact");
        helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the blue throwball's impact slows a nearby zombie");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dryingRackDriesItsInput(GameTestHelper helper) {
        BlockPos rackPos = new BlockPos(3, 2, 3);
        helper.setBlock(rackPos, GadgetBlocks.DRYING_RACK.get());
        helper.assertTrue(helper.getBlockEntity(rackPos) instanceof DryingRackBlockEntity, "the placed drying rack has a drying-rack block entity");
        DryingRackBlockEntity rack = (DryingRackBlockEntity) helper.getBlockEntity(rackPos);

        // Wet sponge dries into sponge — one of the default SMTCON-143 drying recipes.
        ItemStack wetSponge = new ItemStack(Items.WET_SPONGE);
        rack.getInputHandler().insertItem(0, wetSponge, false);
        DryingRecipe recipe = dryingRecipeFor(helper, wetSponge);

        // The rack dries in dryTime ticks; budget a little extra and stop once the slot changes.
        for (int tick = 0; tick < recipe.dryTime() + 20 && rack.getInputHandler().getStackInSlot(0).is(Items.WET_SPONGE); tick++) {
            DryingRackBlockEntity.serverTick(helper.getLevel(), rack.getBlockPos(), rack.getBlockState(), rack);
        }

        ItemStack dried = rack.getInputHandler().getStackInSlot(0);
        helper.assertTrue(dried.is(Items.SPONGE), "the wet sponge dried into a sponge");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void slimeslingLaunchesThePlayer(GameTestHelper helper) {
        Player player = GameTestHelpers.setupFakePlayer(helper);
        // Aim straight up so a full-charge launch is a purely vertical velocity.
        player.setXRot(-90.0F);
        player.setYRot(0.0F);
        double startY = player.getDeltaMovement().y;

        // releaseUsing with timeLeft 0 means the sling was held for its whole use duration — a
        // full-power charge. The launch sets the player's velocity along their look vector.
        ItemStack sling = new ItemStack(GadgetItems.SLING_BLUE.get());
        GadgetItems.SLING_BLUE.get().releaseUsing(sling, helper.getLevel(), player, 0);

        Vec3 launched = player.getDeltaMovement();
        helper.assertTrue(launched.y > startY, "the slimesling launch increased the player's upward velocity");
        helper.assertTrue(launched.y > 0.5D, "a full-charge upward launch produces a strong upward velocity");
        helper.succeed();
    }

    /** Resolves the drying recipe for {@code stack}, failing the test when none is registered. */
    private static DryingRecipe dryingRecipeFor(GameTestHelper helper, ItemStack stack) {
        Optional<RecipeHolder<DryingRecipe>> recipe = helper.getLevel().getRecipeManager().getRecipeFor(GadgetRecipes.DRYING_TYPE.get(), new SingleRecipeInput(stack), helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "a drying recipe is registered for " + stack.getItem());
        return recipe.get().value();
    }
}
