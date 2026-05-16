package slimeknights.sconstruct.port1211.smeltery.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;

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
        assertTrue(be.getItemHandler().getStackInSlot(0).isEmpty(), "the consumed melting slot is cleared on completion");
    }

    @Test
    void tickMeltsIsANoOpWithNoActiveMelts() {
        SmelteryControllerBlockEntity be = controller();
        be.tickMelts();
        assertTrue(be.getActiveMelts().isEmpty(), "ticking an idle controller leaves the active list empty");
        assertTrue(be.getFluidHandler().getFluidInTank(0).isEmpty(), "ticking an idle controller pours nothing");
    }
}
