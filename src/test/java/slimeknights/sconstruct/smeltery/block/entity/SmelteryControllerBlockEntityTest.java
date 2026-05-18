package slimeknights.sconstruct.smeltery.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.smeltery.SmelteryComponents;

/**
 * Pinned-behaviour tests for {@link SmelteryControllerBlockEntity}. Covers the initial tank /
 * melting-slot sizing, the temperature accessor, and the {@link SmelteryControllerBlockEntity#tickMelts()}
 * contract — that a melt advances each tick and, on completion, pours its result into the tank,
 * clears the melting slot it consumed, and drops out of the active list.
 */
class SmelteryControllerBlockEntityTest {

    private static SmelteryControllerBlockEntity controller() {
        return new SmelteryControllerBlockEntity(BlockPos.ZERO, SmelteryComponents.SMELTERY_CONTROLLER.get().defaultBlockState());
    }

    @Test
    void tankAndMeltingSlotsStartAtTheirInitialSizes() {
        SmelteryControllerBlockEntity be = controller();
        assertEquals(SmelteryControllerBlockEntity.INITIAL_TANK_CAPACITY, be.getFluidHandler().getTankCapacity(0), "initial tank capacity");
        assertEquals(SmelteryControllerBlockEntity.INITIAL_MELTING_SLOTS, be.getItemHandler().getSlots(), "initial melting-slot count");
    }

    @Test
    void temperatureRoundTripsThroughTheAccessor() {
        SmelteryControllerBlockEntity be = controller();
        assertEquals(0, be.getCurrentTemperature(), "temperature starts at zero");
        be.setCurrentTemperature(1500);
        assertEquals(1500, be.getCurrentTemperature());
    }

    @Test
    void tickMeltsAdvancesAnInProgressMeltWithoutCompletingItEarly() {
        SmelteryControllerBlockEntity be = controller();
        be.addMelt(new MeltingProgress(0, 4, new FluidStack(Fluids.LAVA, 500)));

        be.tickMelts();
        be.tickMelts();

        assertEquals(1, be.getActiveMelts().size(), "a melt short of its duration stays active");
        assertEquals(2, be.getActiveMelts().get(0).elapsedTicks(), "two ticks advanced the melt twice");
        assertTrue(be.getFluidHandler().getFluidInTank(0).isEmpty(), "no fluid poured before the melt completes");
    }

    @Test
    void tickMeltsCompletesAMeltByPouringFluidAndClearingTheSlot() {
        SmelteryControllerBlockEntity be = controller();
        // Seed the consumed melting slot so the completion's slot-clear is observable.
        be.getItemHandler().insertItem(0, new ItemStack(Items.IRON_INGOT), false);
        be.addMelt(new MeltingProgress(0, 3, new FluidStack(Fluids.LAVA, 500)));

        for (int tick = 0; tick < 3; tick++) {
            be.tickMelts();
        }

        assertTrue(be.getActiveMelts().isEmpty(), "the completed melt is removed from the active list");
        assertEquals(500, be.getFluidHandler().getFluidInTank(0).getAmount(), "the melt's result is poured into the tank");
        assertSame(Fluids.LAVA, be.getFluidHandler().getFluidInTank(0).getFluid(), "the poured fluid is the melt's result fluid");
        assertTrue(be.getItemHandler().getStackInSlot(0).isEmpty(), "the consumed melting slot is cleared on completion");
    }

    @Test
    void tickMeltsDefersAFinishedMeltWhenTheTankCannotTakeTheWholePour() {
        SmelteryControllerBlockEntity be = controller();
        // Leave only 100 mB of headroom — less than the melt's 500 mB result.
        be.getFluidHandler().fill(new FluidStack(Fluids.LAVA, SmelteryControllerBlockEntity.INITIAL_TANK_CAPACITY - 100), IFluidHandler.FluidAction.EXECUTE);
        be.addMelt(new MeltingProgress(0, 2, new FluidStack(Fluids.LAVA, 500)));

        for (int tick = 0; tick < 4; tick++) {
            be.tickMelts();
        }

        assertEquals(1, be.getActiveMelts().size(), "a finished melt stays queued while the tank is full");
        assertTrue(be.getActiveMelts().get(0).isComplete(), "the deferred melt is still marked complete");
        assertEquals(SmelteryControllerBlockEntity.INITIAL_TANK_CAPACITY - 100, be.getFluidHandler().getFluidInTank(0).getAmount(), "no partial pour while the tank lacks room");
    }

    @Test
    void aMeltingSlotIsLockedAgainstExtractionWhileItsMeltIsActive() {
        SmelteryControllerBlockEntity be = controller();
        be.getItemHandler().insertItem(0, new ItemStack(Items.IRON_INGOT), false);
        be.addMelt(new MeltingProgress(0, 10, new FluidStack(Fluids.LAVA, 500)));

        // Slot 0 backs an in-flight melt — a hopper or player must not be able to pull it out
        // or swap a different stack onto it.
        assertTrue(be.getItemHandler().extractItem(0, 64, false).isEmpty(), "a reserved slot rejects extraction");
        ItemStack rejected = be.getItemHandler().insertItem(0, new ItemStack(Items.GOLD_INGOT), false);
        assertEquals(1, rejected.getCount(), "a reserved slot rejects insertion — the whole stack bounces back");
        assertSame(Items.IRON_INGOT, be.getItemHandler().getStackInSlot(0).getItem(), "the reserved input stays in place");
        // A slot with no melt still accepts insertion normally.
        assertTrue(be.getItemHandler().insertItem(1, new ItemStack(Items.IRON_INGOT), false).isEmpty(), "an unreserved slot still accepts insertion");
        assertSame(Items.IRON_INGOT, be.getItemHandler().getStackInSlot(1).getItem(), "the unreserved slot holds the inserted item");
    }

    @Test
    void tickMeltsIsANoOpWithNoActiveMelts() {
        SmelteryControllerBlockEntity be = controller();
        be.tickMelts();
        assertTrue(be.getActiveMelts().isEmpty(), "ticking an idle controller leaves the active list empty");
        assertTrue(be.getFluidHandler().getFluidInTank(0).isEmpty(), "ticking an idle controller pours nothing");
    }

    @Test
    void applyFuelUpdateSetsTheCurrentAndTargetTemperatures() {
        SmelteryControllerBlockEntity be = controller();

        be.applyFuelUpdate(1500, 1600);

        assertEquals(1500, be.getCurrentTemperature(), "the fuel update sets the current temperature");
        assertEquals(1600, be.getTargetTemperature(), "the fuel update sets the target temperature");
    }

    @Test
    void applyFluidUpdateReplacesTheTankContents() {
        SmelteryControllerBlockEntity be = controller();

        be.applyFluidUpdate(List.of(new FluidStack(Fluids.LAVA, 500)));
        assertEquals(500, be.getFluidHandler().getFluidInTank(0).getAmount(), "the fluid update fills the tank");

        be.applyFluidUpdate(List.of());
        assertTrue(be.getFluidHandler().getFluidInTank(0).isEmpty(), "an empty fluid update clears the tank");
    }

    @Test
    void applyStructureUpdateSetsAndClearsTheRenderBounds() {
        SmelteryControllerBlockEntity be = controller();
        BoundingBox box = new BoundingBox(0, 0, 0, 2, 2, 2);

        be.applyStructureUpdate(Optional.of(box));
        assertEquals(box, be.getRenderBounds().orElseThrow(), "the structure update sets the render bounds");

        be.applyStructureUpdate(Optional.empty());
        assertTrue(be.getRenderBounds().isEmpty(), "an empty structure update clears the render bounds");
    }
}
