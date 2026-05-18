package slimeknights.sconstruct.smeltery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.smeltery.block.SearedTankIoBlock;
import slimeknights.sconstruct.smeltery.block.entity.SearedTankBE;

/**
 * Unit tests for {@link SearedTankBE} — the standalone seared-tank block entity (SMTCON-118).
 * The block-entity registry is frozen before tests run, so the registered {@link BlockEntityType}
 * and the placed {@link BlockState} are Mockito mocks; the tank's own {@code FluidTank} is real,
 * so capacity and fill/drain behaviour are exercised directly.
 */
class SearedTankBETest {

    private static final BlockPos POS = new BlockPos(0, 64, 0);

    @Test
    void ioTankUsesTheLargerCapacity() {
        IFluidHandler tank = tank(true).getFluidHandler();

        assertEquals(SearedTankBE.CAPACITY_IO, tank.getTankCapacity(0), "a seared tank IO holds the larger IO capacity");
    }

    @Test
    void inTankUsesTheSmallerCapacity() {
        IFluidHandler tank = tank(false).getFluidHandler();

        assertEquals(SearedTankBE.CAPACITY_IN, tank.getTankCapacity(0), "a seared tank in holds the smaller input capacity");
    }

    @Test
    void fillStoresFluidUpToCapacityAndRejectsTheOverflow() {
        IFluidHandler tank = tank(false).getFluidHandler();
        int offered = SearedTankBE.CAPACITY_IN + 500;

        int accepted = tank.fill(new FluidStack(Fluids.WATER, offered), IFluidHandler.FluidAction.EXECUTE);

        assertEquals(SearedTankBE.CAPACITY_IN, accepted, "fill is clamped to the tank capacity");
        assertEquals(SearedTankBE.CAPACITY_IN, tank.getFluidInTank(0).getAmount(), "the tank holds exactly its capacity");
    }

    @Test
    void drainRemovesStoredFluid() {
        IFluidHandler tank = tank(true).getFluidHandler();
        tank.fill(new FluidStack(Fluids.WATER, 3000), IFluidHandler.FluidAction.EXECUTE);

        FluidStack drained = tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);

        assertEquals(1000, drained.getAmount(), "drain returns the requested amount");
        assertEquals(2000, tank.getFluidInTank(0).getAmount(), "the tank keeps the undrained remainder");
    }

    @Test
    void anEmptyTankProvidesNoFuel() {
        SearedTankBE tank = tank(false);

        assertEquals(0, tank.getTemperature(), "an empty tank has no fuel temperature");
        assertFalse(tank.canProvideFuel(), "an empty tank cannot provide fuel");
        assertEquals(0, tank.consumeFuel(10), "an empty tank consumes nothing");
    }

    @Test
    void aLavaTankIsAFuelSourceAtTheLegacyTemperature() {
        SearedTankBE tank = tank(true);
        tank.getFluidHandler().fill(new FluidStack(Fluids.LAVA, 3000), IFluidHandler.FluidAction.EXECUTE);

        assertEquals(1000, SearedTankBE.LAVA_TEMPERATURE, "the legacy lava fuel temperature is pinned at 1000 K");
        assertEquals(SearedTankBE.LAVA_TEMPERATURE, tank.getTemperature(), "lava heats to the legacy temperature");
        assertTrue(tank.canProvideFuel(), "a lava tank can provide fuel");
    }

    @Test
    void consumeFuelDrainsTheRequestedAmountFromAFuelTank() {
        SearedTankBE tank = tank(true);
        tank.getFluidHandler().fill(new FluidStack(Fluids.LAVA, 3000), IFluidHandler.FluidAction.EXECUTE);

        int consumed = tank.consumeFuel(10);

        assertEquals(10, consumed, "consumeFuel drains the requested millibuckets");
        assertEquals(2990, tank.getFluidHandler().getFluidInTank(0).getAmount(), "the tank keeps the unconsumed fuel");
    }

    @Test
    void consumeFuelDrainsOnlyWhatTheTankHoldsWhenAskedForMore() {
        SearedTankBE tank = tank(false);
        tank.getFluidHandler().fill(new FluidStack(Fluids.LAVA, 250), IFluidHandler.FluidAction.EXECUTE);

        int consumed = tank.consumeFuel(1000);

        assertEquals(250, consumed, "consumeFuel returns only the fuel actually drained");
        assertTrue(tank.getFluidHandler().getFluidInTank(0).isEmpty(), "the under-filled tank is emptied");
    }

    @Test
    void consumeFuelIgnoresAZeroOrNegativeRequest() {
        SearedTankBE tank = tank(true);
        tank.getFluidHandler().fill(new FluidStack(Fluids.LAVA, 3000), IFluidHandler.FluidAction.EXECUTE);

        assertEquals(0, tank.consumeFuel(0), "a zero request consumes nothing");
        assertEquals(0, tank.consumeFuel(-100), "a negative request consumes nothing");
        assertEquals(3000, tank.getFluidHandler().getFluidInTank(0).getAmount(), "the tank's fuel is untouched");
    }

    @Test
    void aNonFuelFluidIsNotAFuelSource() {
        SearedTankBE tank = tank(false);
        tank.getFluidHandler().fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);

        assertEquals(0, tank.getTemperature(), "water is not a smeltery fuel");
        assertFalse(tank.canProvideFuel(), "a water tank cannot provide fuel");
        assertEquals(0, tank.consumeFuel(10), "a non-fuel tank consumes nothing");
        // The molten-metal fuel path (SmelteryFluids.moltenTemperature) needs the fluid registry
        // and so is verified in-game, not here — only lava and water are reachable from JUnit.
    }

    /**
     * A {@link SearedTankBE} backed by a mocked registered type. When {@code io} is true the
     * placed state reports a {@link SearedTankIoBlock}, selecting the IO capacity; otherwise the
     * state's block is left unmocked so the BE falls back to the input-tank capacity.
     */
    @SuppressWarnings("unchecked")
    private static SearedTankBE tank(boolean io) {
        BlockEntityType<SearedTankBE> type = mock(BlockEntityType.class);
        when(type.isValid(any(BlockState.class))).thenReturn(true);
        BlockState state = mock(BlockState.class);
        if (io) {
            when(state.getBlock()).thenReturn(mock(SearedTankIoBlock.class));
        }
        return new SearedTankBE(type, POS, state);
    }
}
