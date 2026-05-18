package slimeknights.sconstruct.plugin.top;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import slimeknights.sconstruct.SConstruct;

/**
 * Shared tooltip-line builders for the The One Probe providers. The probe runs server-side and
 * reads the live block entity directly, so — unlike the Jade providers, which bridge through a
 * synced NBT tag — these helpers turn an {@link IFluidHandler} straight into a probe line.
 *
 * <p>The lines reuse the {@code gui.sconstruct.jade.*} keys: the probe shows exactly the same
 * temperature and tank text Jade does, so a second set of identical strings would only be
 * translation-file noise.
 */
final class ProbeTooltips {

    private ProbeTooltips() {
    }

    /** The {@code "Temperature: N K"} probe line. */
    static Component temperatureLine(int temperatureKelvin) {
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jade.temperature", temperatureKelvin);
    }

    /**
     * The {@code "Tank: …"} probe line for tank {@code 0} of {@code handler} — {@code "Tank:
     * empty"} when the tank holds no fluid or exposes no tanks at all.
     */
    static Component tankLine(IFluidHandler handler) {
        if (handler.getTanks() == 0) {
            return emptyTank();
        }
        FluidStack fluid = handler.getFluidInTank(0);
        int capacity = handler.getTankCapacity(0);
        if (fluid.isEmpty() || capacity <= 0) {
            return emptyTank();
        }
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jade.tank", fluid.getAmount(), capacity, fluid.getHoverName());
    }

    private static Component emptyTank() {
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jade.tank_empty");
    }
}
