package slimeknights.sconstruct.port1211.plugin.jade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import slimeknights.sconstruct.port1211.SConstruct;

/**
 * Shared NBT bridge for the Jade smeltery and seared-tank tooltip providers. The server-side
 * {@code appendServerData} pass writes a single tank's contents into the synced
 * {@link CompoundTag}; the client-side {@code appendTooltip} pass reads it back into a tooltip
 * line.
 *
 * <p>The fluid is carried as its registry id rather than a serialised {@link FluidStack} so the
 * payload stays tiny and decodes without a {@code RegistryAccess}; an empty or unknown id
 * renders the {@code tank_empty} line.
 */
final class JadeTankData {

    private static final String KEY_FLUID = "Fluid";
    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_CAPACITY = "Capacity";

    private JadeTankData() {
    }

    /** Writes tank {@code 0} of {@code handler} into {@code data} for the client to read back. */
    static void writeTank(CompoundTag data, IFluidHandler handler) {
        if (handler.getTanks() == 0) {
            return;
        }
        FluidStack fluid = handler.getFluidInTank(0);
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        data.putString(KEY_FLUID, fluid.isEmpty() ? "" : fluidId.toString());
        data.putInt(KEY_AMOUNT, fluid.getAmount());
        data.putInt(KEY_CAPACITY, handler.getTankCapacity(0));
    }

    /**
     * The {@code "Tank: …"} tooltip line for the tank data previously written by
     * {@link #writeTank} — {@code "Tank: empty"} when the tank holds no fluid.
     */
    static Component tankLine(CompoundTag data) {
        int capacity = data.getInt(KEY_CAPACITY);
        int amount = data.getInt(KEY_AMOUNT);
        String fluidId = data.getString(KEY_FLUID);
        if (fluidId.isEmpty() || amount <= 0 || capacity <= 0) {
            return Component.translatable("gui." + SConstruct.MOD_ID + ".jade.tank_empty");
        }
        Fluid fluid = resolveFluid(fluidId);
        Component fluidName = fluid == Fluids.EMPTY ? Component.literal(fluidId) : new FluidStack(fluid, amount).getHoverName();
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jade.tank", amount, capacity, fluidName);
    }

    private static Fluid resolveFluid(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getOptional(key).orElse(Fluids.EMPTY);
    }
}
