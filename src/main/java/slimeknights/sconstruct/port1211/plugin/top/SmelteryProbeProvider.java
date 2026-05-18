package slimeknights.sconstruct.port1211.plugin.top;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryControllerBlockEntity;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;

/**
 * The One Probe info provider for the smeltery controller (SMTCON-165). Adds the smeltery's
 * current temperature and tank contents to the probe overlay — the same data the Jade
 * {@code SmelteryProvider} shows.
 *
 * <p>The probe runs this server-side and hands over the server {@link Level}, so the live
 * controller block entity is read directly from {@code level} at the hit position.
 */
public final class SmelteryProbeProvider implements IProbeInfoProvider {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "smeltery");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level level, BlockState blockState, IProbeHitData data) {
        BlockEntity blockEntity = level.getBlockEntity(data.getPos());
        if (blockEntity instanceof SmelteryControllerBlockEntity controller) {
            probeInfo.text(ProbeTooltips.temperatureLine(controller.getCurrentTemperature()));
            probeInfo.text(ProbeTooltips.tankLine(controller.getFluidHandler()));
        }
    }
}
