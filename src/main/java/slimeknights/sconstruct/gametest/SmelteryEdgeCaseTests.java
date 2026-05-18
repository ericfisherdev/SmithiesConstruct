package slimeknights.sconstruct.gametest;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import slimeknights.sconstruct.smeltery.MoltenMetals;
import slimeknights.sconstruct.smeltery.SearedBlocks;
import slimeknights.sconstruct.smeltery.SmelteryComponents;
import slimeknights.sconstruct.smeltery.SmelteryFluids;
import slimeknights.sconstruct.smeltery.block.SmelteryControllerBlock;
import slimeknights.sconstruct.smeltery.block.entity.MeltingProgress;
import slimeknights.sconstruct.smeltery.block.entity.SearedTankBE;
import slimeknights.sconstruct.smeltery.block.entity.SmelteryControllerBlockEntity;
import slimeknights.sconstruct.smeltery.recipe.AlloyRecipe;
import slimeknights.sconstruct.smeltery.recipe.AlloyRecipeInput;
import slimeknights.sconstruct.smeltery.recipe.MeltingRecipe;
import slimeknights.sconstruct.smeltery.recipe.SmelteryRecipes;

/**
 * Smeltery edge-case {@link GameTest}s (SMTCON-176) — the structure / fuel / disassembly
 * behaviours a per-phase happy-path test does not reach. Where {@link SmelteryTests} pins the
 * assemble / melt / cast / alloy happy paths, this class drives the transitions: a wall broken
 * and replaced, a permanent break while the tank holds metal, alloy resolution at the
 * temperature boundary, and a melt that stalls without fuel and completes with it.
 *
 * <p>Each test assembles a minimal 1x1x1 seared bowl inside the shared {@code gametest_7x7x7}
 * template and advances the controller's {@code serverTick} synchronously, mirroring the
 * deterministic single-tick style of {@link SmelteryTests}.
 */
// The ServerLevel and MinecraftServer are borrowed from the gameTestServer harness — the test
// never owns their lifecycle. PMD's CloseResource heuristic cannot model that.
@SuppressWarnings("PMD.CloseResource")
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SmelteryEdgeCaseTests {

    private static final String TEMPLATE = "gametest_7x7x7";

    /** Floor-slab Y — the seared block directly under the interior cell. */
    private static final int FLOOR_Y = 1;

    /** Interior / bottom-wall-ring Y — the controller and the bottom ring sit on this layer. */
    private static final int BASE_Y = 2;

    /** The single interior cell of the 1x1x1 smeltery these tests assemble. */
    private static final BlockPos INTERIOR = new BlockPos(3, BASE_Y, 3);

    /** Controller position — the bottom wall block on the south face of the interior. */
    private static final BlockPos CONTROLLER = INTERIOR.relative(Direction.SOUTH);

    /** A bottom-ring corner swapped for a seared tank so the fuel test has a heat source. */
    private static final BlockPos TANK = new BlockPos(2, BASE_Y, 2);

    /** The seared floor slab under the interior — broken to force a permanent disassembly. */
    private static final BlockPos FLOOR = new BlockPos(INTERIOR.getX(), FLOOR_Y, INTERIOR.getZ());

    /** A non-controller, non-tank bottom-ring wall block — broken and replaced by test 1. */
    private static final BlockPos WALL = new BlockPos(4, BASE_Y, 4);

    /** Brass alloys from 432 mB molten copper — the amount the SMTCON-128 recipe demands. */
    private static final int BRASS_COPPER_MB = 432;

    /** Brass alloys from 144 mB molten zinc — the amount the SMTCON-128 recipe demands. */
    private static final int BRASS_ZINC_MB = 144;

    /** The molten-brass yield of the SMTCON-128 brass alloy recipe — 432 + 144 mB combined. */
    private static final int BRASS_YIELD_MB = 576;

    /** A copper amount large enough to overflow the 1x1x1 interior on a disassembly release. */
    private static final int COPPER_FILL_MB = 3000;

    /** Lava loaded into the seared fuel tank — comfortably more than any melt these tests run. */
    private static final int LAVA_FUEL_MB = 2000;

    /** Tick budget for a melt loop — larger than any recipe duration these tests use. */
    private static final int TICK_BUDGET = 400;

    /** Tick count for the no-fuel stall check — long enough to prove the melt never advances. */
    private static final int NO_FUEL_TICKS = 40;

    private SmelteryEdgeCaseTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void smelteryWallBreakReassembles(GameTestHelper helper) {
        buildSmeltery(helper, false);
        SmelteryControllerBlockEntity controller = controllerAt(helper);
        controller.tryAssemble();
        helper.assertTrue(controller.isAssembled(), "the freshly built smeltery assembles");

        // Knock out one wall block — the multiblock is now incomplete and must fail validation.
        helper.setBlock(WALL, Blocks.AIR);
        controller.tryAssemble();
        helper.assertFalse(controller.isAssembled(), "a smeltery with a missing wall block fails validation");

        // Replace the wall block — validation must succeed again, with the interior cleared so
        // the validator reads an open-topped height-1 bowl.
        helper.setBlock(WALL, SearedBlocks.SEARED_BRICK.get());
        helper.setBlock(WALL.above(), Blocks.AIR);
        controller.tryAssemble();
        helper.assertTrue(controller.isAssembled(), "replacing the broken wall block lets the smeltery reassemble");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void smelteryFluidDropsOnPermBreak(GameTestHelper helper) {
        buildSmeltery(helper, false);
        SmelteryControllerBlockEntity controller = controllerAt(helper);
        controller.tryAssemble();
        helper.assertTrue(controller.isAssembled(), "the smeltery assembles before the break");

        // Half-fill the tank with molten copper — more than the single interior cell can hold,
        // so the disassembly release must place what it can and leave the rest in the tank.
        Fluid moltenCopper = SmelteryFluids.get(MoltenMetals.COPPER).source().get();
        int filled = controller.getFluidHandler().fill(new FluidStack(moltenCopper, COPPER_FILL_MB), IFluidHandler.FluidAction.EXECUTE);
        helper.assertValueEqual(filled, COPPER_FILL_MB, "the controller tank accepts the molten copper");

        // Break the seared floor — a permanent disassembly. The controller must release the
        // tank's metal into the world as fluid blocks rather than deleting it.
        helper.setBlock(FLOOR, Blocks.AIR);
        controller.tryAssemble();
        helper.assertFalse(controller.isAssembled(), "breaking the floor disassembles the smeltery");

        BlockState released = helper.getLevel().getBlockState(helper.absolutePos(INTERIOR));
        helper.assertFalse(released.getFluidState().isEmpty(), "the disassembly poured molten copper into the interior cell");

        // Conservation: the metal that left the tank must equal the metal now in the world. The
        // 1x1x1 interior holds one bucket; the remaining two buckets stay in the tank.
        int remaining = controller.getFluidHandler().getFluidInTank(0).getAmount();
        int placedBlocks = countInteriorFluidBlocks(helper);
        helper.assertValueEqual(remaining + placedBlocks * 1000, COPPER_FILL_MB, "disassembly conserves every drop of stored metal");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void alloyResolvesAfterPour(GameTestHelper helper) {
        Fluid moltenCopper = SmelteryFluids.get(MoltenMetals.COPPER).source().get();
        Fluid moltenZinc = SmelteryFluids.get(MoltenMetals.ZINC).source().get();
        Fluid moltenBrass = SmelteryFluids.get(MoltenMetals.BRASS).source().get();
        List<FluidStack> poured = List.of(new FluidStack(moltenCopper, BRASS_COPPER_MB), new FluidStack(moltenZinc, BRASS_ZINC_MB));

        // At brass temperature the 3:1 copper:zinc pour resolves to molten brass.
        AlloyRecipe matched = findAlloyRecipe(helper, new AlloyRecipeInput(poured, MoltenMetals.BRASS.temperature()));
        helper.assertTrue(matched != null, "a copper + zinc pour at brass temperature resolves an alloy recipe");
        helper.assertTrue(matched.output().getFluid().isSame(moltenBrass), "the resolved alloy yields molten brass");
        helper.assertValueEqual(matched.output().getAmount(), BRASS_YIELD_MB, "the alloy yields the SMTCON-128 brass amount");

        // Edge case: the exact same pour below the alloy's temperature floor resolves nothing —
        // a cold smeltery must not alloy.
        AlloyRecipe cold = findAlloyRecipe(helper, new AlloyRecipeInput(poured, 0));
        helper.assertTrue(cold == null, "the same pour below the temperature floor resolves no alloy recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void meltCompletesWithLavaFuel(GameTestHelper helper) {
        buildSmeltery(helper, true);
        SmelteryControllerBlockEntity controller = controllerAt(helper);
        controller.tryAssemble();
        helper.assertTrue(controller.isAssembled(), "the smeltery assembles before melting");

        // Queue an iron melt. With no fuel in the tank the fuel system delivers no heat, so the
        // melt must not advance and the temperature must stay at zero.
        ItemStack remainder = controller.getItemHandler().insertItem(0, new ItemStack(Items.IRON_INGOT), false);
        helper.assertTrue(remainder.isEmpty(), "melting slot 0 accepts the iron ingot");
        MeltingRecipe recipe = meltingRecipeFor(helper, new ItemStack(Items.IRON_INGOT));
        controller.addMelt(new MeltingProgress(0, recipe.time(), recipe.output()));

        BlockState controllerState = controller.getBlockState();
        for (int tick = 0; tick < NO_FUEL_TICKS; tick++) {
            SmelteryControllerBlockEntity.serverTick(helper.getLevel(), controller.getBlockPos(), controllerState, controller);
        }
        helper.assertTrue(controller.getFluidHandler().getFluidInTank(0).isEmpty(), "with no fuel the melt produces nothing");
        helper.assertValueEqual(controller.getCurrentTemperature(), 0, "with no fuel the smeltery stays cold");

        // Load lava into the seared tank — the fuel system must now deliver heat and complete
        // the melt.
        SearedTankBE tank = blockEntityAt(helper, TANK, SearedTankBE.class, "seared tank");
        int lavaFilled = tank.getFluidHandler().fill(new FluidStack(Fluids.LAVA, LAVA_FUEL_MB), IFluidHandler.FluidAction.EXECUTE);
        helper.assertValueEqual(lavaFilled, LAVA_FUEL_MB, "the seared tank accepts the lava fuel");

        FluidStack expected = recipe.output();
        for (int tick = 0; tick < TICK_BUDGET && controller.getFluidHandler().getFluidInTank(0).getAmount() < expected.getAmount(); tick++) {
            SmelteryControllerBlockEntity.serverTick(helper.getLevel(), controller.getBlockPos(), controllerState, controller);
        }
        helper.assertTrue(controller.getCurrentTemperature() > 0, "lava fuel raised the smeltery temperature");
        FluidStack tankFluid = controller.getFluidHandler().getFluidInTank(0);
        helper.assertTrue(FluidStack.isSameFluid(tankFluid, expected), "the lava-fuelled melt poured molten iron into the tank");
        helper.assertValueEqual(tankFluid.getAmount(), expected.getAmount(), "the melt completed once fuel delivered heat");
        helper.succeed();
    }

    /**
     * Builds a minimal 1x1x1 seared bowl around {@link #INTERIOR}: one seared floor slab, the
     * eight-block bottom wall ring (one slot the controller, optionally one a seared tank), and
     * air above so the validator reads an open-topped height-1 smeltery.
     */
    private static void buildSmeltery(GameTestHelper helper, boolean withTank) {
        helper.setBlock(FLOOR, SearedBlocks.SEARED_BRICK.get());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos ring = new BlockPos(INTERIOR.getX() + dx, BASE_Y, INTERIOR.getZ() + dz);
                if (ring.equals(CONTROLLER)) {
                    helper.setBlock(ring, SmelteryComponents.SMELTERY_CONTROLLER.get().defaultBlockState().setValue(SmelteryControllerBlock.FACING, Direction.SOUTH));
                }
                else if (withTank && ring.equals(TANK)) {
                    helper.setBlock(ring, SmelteryComponents.SEARED_TANK_IO.get());
                }
                else {
                    helper.setBlock(ring, SearedBlocks.SEARED_BRICK.get());
                }
                helper.setBlock(ring.above(), Blocks.AIR);
            }
        }
        helper.setBlock(INTERIOR, Blocks.AIR);
        helper.setBlock(INTERIOR.above(), Blocks.AIR);
    }

    /** Counts the interior cells now occupied by a fluid block — the disassembly release set. */
    private static int countInteriorFluidBlocks(GameTestHelper helper) {
        return helper.getLevel().getBlockState(helper.absolutePos(INTERIOR)).getFluidState().isEmpty() ? 0 : 1;
    }

    /** Fetches the controller block entity, failing the test if it did not attach. */
    private static SmelteryControllerBlockEntity controllerAt(GameTestHelper helper) {
        return blockEntityAt(helper, CONTROLLER, SmelteryControllerBlockEntity.class, "smeltery controller");
    }

    /** Typed block-entity fetch with a clear failure message — see {@link GameTestHelpers}. */
    private static <T> T blockEntityAt(GameTestHelper helper, BlockPos pos, Class<T> type, String description) {
        net.minecraft.world.level.block.entity.BlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(type.isInstance(be), description + " block entity attaches");
        return type.cast(be);
    }

    /** Resolves the melting recipe for {@code stack}, failing the test when none is registered. */
    private static MeltingRecipe meltingRecipeFor(GameTestHelper helper, ItemStack stack) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<MeltingRecipe>> recipe = level.getRecipeManager().getRecipeFor(SmelteryRecipes.MELTING_TYPE.get(), new SingleRecipeInput(stack), level);
        helper.assertTrue(recipe.isPresent(), "a melting recipe is registered for " + stack.getItem());
        return recipe.get().value();
    }

    /**
     * Resolves the first {@link AlloyRecipe} that matches {@code input}, or {@code null} when
     * none does. An {@link AlloyRecipeInput} carries no items, so {@code RecipeManager#getRecipeFor}
     * would short-circuit it away — the recipes are matched directly instead, mirroring how the
     * controller's future alloy tick will scan them.
     */
    private static AlloyRecipe findAlloyRecipe(GameTestHelper helper, AlloyRecipeInput input) {
        ServerLevel level = helper.getLevel();
        for (RecipeHolder<AlloyRecipe> holder : level.getRecipeManager().getAllRecipesFor(SmelteryRecipes.ALLOY_TYPE.get())) {
            if (holder.value().matches(input, level)) {
                return holder.value();
            }
        }
        return null;
    }
}
