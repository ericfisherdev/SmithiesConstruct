package slimeknights.sconstruct.port1211.gametest;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import slimeknights.sconstruct.port1211.smeltery.CastingBlocks;
import slimeknights.sconstruct.port1211.smeltery.MoltenMetals;
import slimeknights.sconstruct.port1211.smeltery.SearedBlocks;
import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;
import slimeknights.sconstruct.port1211.smeltery.SmelteryFluids;
import slimeknights.sconstruct.port1211.smeltery.block.SmelteryControllerBlock;
import slimeknights.sconstruct.port1211.smeltery.block.entity.AbstractCastingBlockEntity;
import slimeknights.sconstruct.port1211.smeltery.block.entity.MeltingProgress;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SearedTankBE;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryControllerBlockEntity;
import slimeknights.sconstruct.port1211.smeltery.multiblock.ComponentType;
import slimeknights.sconstruct.port1211.smeltery.multiblock.SmelteryStructure;
import slimeknights.sconstruct.port1211.smeltery.recipe.AlloyRecipe;
import slimeknights.sconstruct.port1211.smeltery.recipe.AlloyRecipeInput;
import slimeknights.sconstruct.port1211.smeltery.recipe.CastingRecipe;
import slimeknights.sconstruct.port1211.smeltery.recipe.CastingRecipeInput;
import slimeknights.sconstruct.port1211.smeltery.recipe.MeltingRecipe;
import slimeknights.sconstruct.port1211.smeltery.recipe.SmelteryRecipes;

/**
 * End-to-end {@link GameTest}s for the smeltery (SMTCON-130) — four cases proving the
 * structure validator, the melting tick, the casting tick, and the alloy recipes work against
 * a live server world.
 *
 * <p><strong>What the smeltery wires today.</strong> SMTCON-114 ships the controller block
 * entity's tick machinery — fuel draw and {@link SmelteryControllerBlockEntity#tickMelts()
 * melt advancement} — and SMTCON-115 the structure validator. SMTCON-123 ships the casting
 * block entity's full recipe-driven tick. The recipe types and the SMTCON-128 recipe datapack
 * are registered. What is <em>not</em> yet wired is the slot-change trigger that turns an item
 * dropped into a melting slot into a {@link MeltingProgress} (the controller exposes
 * {@link SmelteryControllerBlockEntity#addMelt} for that future layer) and any controller-side
 * execution of {@link AlloyRecipe} — the multi-fluid tank an alloy needs does not exist on the
 * controller. These tests therefore drive {@code addMelt} directly and exercise the alloy
 * recipe through the recipe manager, the same way {@link ToolForgeTests} drives the forge's
 * handler directly rather than through a menu. Each gap is called out on the test it affects.
 *
 * <p>Every test builds its smeltery by placing blocks programmatically inside the shared empty
 * {@code gametest_7x7x7} template and advances the relevant block entity's {@code serverTick}
 * synchronously, so each case is deterministic and completes within a single world tick.
 */
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SmelteryTests {

    private static final String TEMPLATE = "gametest_7x7x7";

    /** Floor-slab Y inside the template — the seared block directly under the interior cell. */
    private static final int FLOOR_Y = 1;

    /** Interior / bottom-wall-ring Y — the controller and the bottom ring sit on this layer. */
    private static final int BASE_Y = 2;

    /** The single interior cell of the 1x1x1 smeltery these tests assemble. */
    private static final BlockPos INTERIOR = new BlockPos(3, BASE_Y, 3);

    /**
     * The controller's position — the bottom wall block on the south face of the interior.
     * Its {@link SmelteryControllerBlock#FACING} is set to {@link Direction#SOUTH} so the
     * validator's interior direction ({@code FACING.getOpposite()}) points north into
     * {@link #INTERIOR}.
     */
    private static final BlockPos CONTROLLER = INTERIOR.relative(Direction.SOUTH);

    /** A bottom-ring wall slot swapped for a seared tank so the melt test has a fuel source. */
    private static final BlockPos TANK = new BlockPos(2, BASE_Y, 2);

    /** Brass alloys from 432 mB molten copper — the amount the SMTCON-128 recipe demands. */
    private static final int BRASS_COPPER_MB = 432;

    /** Brass alloys from 144 mB molten zinc — the amount the SMTCON-128 recipe demands. */
    private static final int BRASS_ZINC_MB = 144;

    /** The molten-brass yield of the SMTCON-128 brass alloy recipe — 432 + 144 mB combined. */
    private static final int BRASS_YIELD_MB = 576;

    /** Tick budget for a melt / cast loop — larger than any recipe duration these tests use. */
    private static final int TICK_BUDGET = 400;

    /** Lava loaded into the seared fuel tank — comfortably more than any melt these tests run. */
    private static final int LAVA_FUEL_MB = 2000;

    private SmelteryTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void assembleSmeltery(GameTestHelper helper) {
        buildSmeltery(helper, false);
        SmelteryControllerBlockEntity controller = controllerAt(helper);

        controller.tryAssemble();

        helper.assertTrue(controller.isAssembled(), "controller assembles the 1x1x1 seared bowl");
        SmelteryStructure structure = controller.getStructure().orElseThrow(() -> new AssertionError("assembled controller must expose a structure"));
        helper.assertValueEqual(structure.bowlVolume(), 1, "1x1x1 bowl has volume 1");
        helper.assertValueEqual(structure.walls().size(), 8, "bottom ring is eight wall blocks");
        helper.assertValueEqual(structure.floor().size(), 1, "floor is the single seared slab under the interior");
        helper.assertTrue(structure.components().get(helper.absolutePos(CONTROLLER)) == ComponentType.CONTROLLER, "controller is catalogued as the CONTROLLER component");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void meltIron(GameTestHelper helper) {
        buildSmeltery(helper, true);
        // Lava in the seared tank is the smeltery's fuel — without it drawFuel pauses the melt.
        SearedTankBE tank = GameTestHelpers.blockEntityAt(helper, TANK, SearedTankBE.class, "seared tank");
        int lavaFilled = tank.getFluidHandler().fill(new FluidStack(Fluids.LAVA, LAVA_FUEL_MB), IFluidHandler.FluidAction.EXECUTE);
        helper.assertValueEqual(lavaFilled, LAVA_FUEL_MB, "the seared tank accepts the full lava fuel load");

        SmelteryControllerBlockEntity controller = controllerAt(helper);
        controller.tryAssemble();
        helper.assertTrue(controller.isAssembled(), "controller assembles before melting");

        // Drop an iron ingot into melting slot 0 and resolve its melting recipe. The slot-change
        // trigger that does this automatically is a later ticket, so the test plays that role.
        ItemStack remainder = controller.getItemHandler().insertItem(0, new ItemStack(Items.IRON_INGOT), false);
        helper.assertTrue(remainder.isEmpty(), "melting slot 0 accepts the whole iron ingot");
        MeltingRecipe recipe = meltingRecipeFor(helper, new ItemStack(Items.IRON_INGOT));
        controller.addMelt(new MeltingProgress(0, recipe.time(), recipe.output()));

        BlockState controllerState = controller.getBlockState();
        FluidStack expected = recipe.output();
        for (int tick = 0; tick < TICK_BUDGET && controller.getFluidHandler().getFluidInTank(0).getAmount() < expected.getAmount(); tick++) {
            SmelteryControllerBlockEntity.serverTick(helper.getLevel(), controller.getBlockPos(), controllerState, controller);
        }

        FluidStack tankFluid = controller.getFluidHandler().getFluidInTank(0);
        helper.assertFalse(tankFluid.isEmpty(), "the completed melt poured fluid into the smeltery tank");
        helper.assertTrue(FluidStack.isSameFluid(tankFluid, expected), "the tank holds molten iron");
        helper.assertValueEqual(tankFluid.getAmount(), expected.getAmount(), "the tank holds the recipe's full molten-iron yield");
        helper.assertTrue(controller.getItemHandler().getStackInSlot(0).isEmpty(), "the melted iron ingot was consumed from its slot");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void castIngot(GameTestHelper helper) {
        BlockPos tablePos = new BlockPos(1, BASE_Y, 1);
        helper.setBlock(tablePos, CastingBlocks.CASTING_TABLE.get());
        AbstractCastingBlockEntity table = GameTestHelpers.blockEntityAt(helper, tablePos, AbstractCastingBlockEntity.class, "casting table");

        // Pour molten copper into the empty-cast table — casting_copper_ingot is a no-cast
        // SMTCON-128 recipe, so the bare table casts a copper ingot once the metal cools.
        Fluid moltenCopper = SmelteryFluids.get(MoltenMetals.COPPER).source().get();
        // BuiltInRegistries.ITEM.get returns Items.AIR (not null) for a missing key — assert the
        // key exists so a renamed / removed ingot fails clearly rather than as a recipe miss.
        ResourceLocation copperIngotId = ResourceLocation.fromNamespaceAndPath(SmokeTest.NAMESPACE, "ingot_copper");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(copperIngotId), "the copper ingot item is registered");
        Item copperIngot = BuiltInRegistries.ITEM.get(copperIngotId);
        // Probe with a generous amount so the FluidIngredient's minimum-mB check passes during
        // lookup; the exact pour then comes from the resolved recipe's own fluid amount. The
        // recipe is pinned to the copper-ingot output so a future no-cast copper recipe cannot
        // make this test pass without still exercising the ingot path.
        CastingRecipe recipe = castingRecipeFor(helper, new FluidStack(moltenCopper, FluidType.BUCKET_VOLUME), copperIngot);
        int filled = table.getFluidHandler().fill(new FluidStack(moltenCopper, recipe.fluid().amount()), IFluidHandler.FluidAction.EXECUTE);
        helper.assertValueEqual(filled, recipe.fluid().amount(), "the table accepts a full casting recipe's worth of molten copper");

        for (int tick = 0; tick < TICK_BUDGET && table.getCastHandler().getStackInSlot(0).isEmpty(); tick++) {
            AbstractCastingBlockEntity.serverTick(helper.getLevel(), table.getBlockPos(), table.getBlockState(), table);
        }

        ItemStack cast = table.getCastHandler().getStackInSlot(0);
        helper.assertFalse(cast.isEmpty(), "the cooled cast produced an item");
        helper.assertTrue(ItemStack.isSameItem(cast, recipe.output()), "the cast item is the recipe's copper ingot");
        helper.assertValueEqual(cast.getCount(), recipe.output().getCount(), "the cast stack size matches the recipe output");
        helper.assertTrue(table.getFluidHandler().getFluidInTank(0).isEmpty(), "the table tank is emptied once the cast completes");
        helper.succeed();
    }

    // ServerLevel is AutoCloseable in the type system, but a GameTest borrows the world from the
    // test server's lifecycle and never owns it — PMD's CloseResource heuristic cannot model that.
    @SuppressWarnings("PMD.CloseResource")
    @GameTest(template = TEMPLATE)
    public static void alloyBrass(GameTestHelper helper) {
        // The controller has no multi-fluid tank and runs no alloy step yet, so this case
        // verifies the alloy recipe end-to-end through the recipe manager: the registered brass
        // recipe must match a copper + zinc tank and yield molten brass. An AlloyRecipeInput
        // carries no items, so RecipeManager#getRecipeFor would short-circuit it away — the
        // recipes are matched directly here instead. Controller-side alloy execution is a
        // follow-up ticket.
        ServerLevel level = helper.getLevel();
        Fluid moltenCopper = SmelteryFluids.get(MoltenMetals.COPPER).source().get();
        Fluid moltenZinc = SmelteryFluids.get(MoltenMetals.ZINC).source().get();
        Fluid moltenBrass = SmelteryFluids.get(MoltenMetals.BRASS).source().get();

        AlloyRecipeInput input = new AlloyRecipeInput(List.of(new FluidStack(moltenCopper, BRASS_COPPER_MB), new FluidStack(moltenZinc, BRASS_ZINC_MB)), MoltenMetals.BRASS.temperature());
        AlloyRecipe recipe = null;
        for (RecipeHolder<AlloyRecipe> holder : level.getRecipeManager().getAllRecipesFor(SmelteryRecipes.ALLOY_TYPE.get())) {
            if (holder.value().matches(input, level)) {
                recipe = holder.value();
                break;
            }
        }
        helper.assertTrue(recipe != null, "an alloy recipe matches a copper + zinc tank at brass temperature");

        FluidStack output = recipe.output();
        helper.assertTrue(output.getFluid().isSame(moltenBrass), "the matched alloy recipe yields molten brass");
        helper.assertValueEqual(output.getAmount(), BRASS_YIELD_MB, "the alloy recipe yields the SMTCON-128 brass amount");
        helper.succeed();
    }

    /**
     * Builds a minimal 1x1x1 seared bowl around {@link #INTERIOR}: one seared floor slab, the
     * eight-block bottom wall ring (one slot being the controller, optionally one being a seared
     * tank), and air above so the validator reads an open-topped, height-1 smeltery.
     */
    private static void buildSmeltery(GameTestHelper helper, boolean withTank) {
        helper.setBlock(new BlockPos(INTERIOR.getX(), FLOOR_Y, INTERIOR.getZ()), SearedBlocks.SEARED_BRICK.get());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos ring = new BlockPos(INTERIOR.getX() + dx, BASE_Y, INTERIOR.getZ() + dz);
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (ring.equals(CONTROLLER)) {
                    helper.setBlock(ring, SmelteryComponents.SMELTERY_CONTROLLER.get().defaultBlockState().setValue(SmelteryControllerBlock.FACING, Direction.SOUTH));
                }
                else if (withTank && ring.equals(TANK)) {
                    helper.setBlock(ring, SmelteryComponents.SEARED_TANK_IO.get());
                }
                else {
                    helper.setBlock(ring, SearedBlocks.SEARED_BRICK.get());
                }
                // Clear the layer above the ring so the validator stops the wall at height 1.
                helper.setBlock(ring.above(), Blocks.AIR);
            }
        }
        // The interior column must be clear so the bowl is open-topped.
        helper.setBlock(INTERIOR, Blocks.AIR);
        helper.setBlock(INTERIOR.above(), Blocks.AIR);
    }

    /** Fetches the controller block entity, failing the test if it did not attach. */
    private static SmelteryControllerBlockEntity controllerAt(GameTestHelper helper) {
        return GameTestHelpers.blockEntityAt(helper, CONTROLLER, SmelteryControllerBlockEntity.class, "smeltery controller");
    }

    /** Resolves the melting recipe for {@code stack}, failing the test when none is registered. */
    // See alloyBrass — the borrowed ServerLevel is not this code's to close.
    @SuppressWarnings("PMD.CloseResource")
    private static MeltingRecipe meltingRecipeFor(GameTestHelper helper, ItemStack stack) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<MeltingRecipe>> recipe = level.getRecipeManager().getRecipeFor(SmelteryRecipes.MELTING_TYPE.get(), new SingleRecipeInput(stack), level);
        helper.assertTrue(recipe.isPresent(), "a melting recipe is registered for " + stack.getItem());
        return recipe.get().value();
    }

    /**
     * Resolves the no-cast casting-table recipe that pours {@code fluid} into {@code expectedOutput},
     * failing the test when none is registered. The recipes are matched directly: a no-cast
     * {@link CastingRecipeInput} carries an empty cast slot, so {@code RecipeManager#getRecipeFor}
     * would short-circuit it away. Filtering on the output item keeps the test pinned to the
     * specific recipe under test rather than whichever no-cast recipe happens to match first.
     */
    // See alloyBrass — the borrowed ServerLevel is not this code's to close.
    @SuppressWarnings("PMD.CloseResource")
    private static CastingRecipe castingRecipeFor(GameTestHelper helper, FluidStack fluid, Item expectedOutput) {
        ServerLevel level = helper.getLevel();
        CastingRecipeInput input = new CastingRecipeInput(fluid, ItemStack.EMPTY, false);
        // Pick the greatest-fluid match, mirroring AbstractCastingBlockEntity#findCastingRecipe,
        // so the test resolves the same recipe the casting tick would run.
        CastingRecipe match = null;
        for (RecipeHolder<CastingRecipe> holder : level.getRecipeManager().getAllRecipesFor(SmelteryRecipes.CASTING_TYPE.get())) {
            CastingRecipe candidate = holder.value();
            if (candidate.matches(input, level) && candidate.output().is(expectedOutput) && (match == null || candidate.fluid().amount() > match.fluid().amount())) {
                match = candidate;
            }
        }
        helper.assertTrue(match != null, "a casting-table recipe for " + expectedOutput + " is registered for " + fluid.getFluid());
        return match;
    }
}
