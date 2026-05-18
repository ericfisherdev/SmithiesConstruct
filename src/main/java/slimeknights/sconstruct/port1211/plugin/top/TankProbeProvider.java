package slimeknights.sconstruct.port1211.plugin.top;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SearedTankBE;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;

/**
 * The One Probe info provider for a standalone seared tank (SMTCON-165). Adds the tank's fluid
 * contents to the probe overlay — the same data the Jade {@code SearedTankProvider} shows.
 *
 * <p>The probe runs this server-side, so the live tank block entity is read directly from
 * {@code level} at the hit position.
 */
public final class TankProbeProvider implements IProbeInfoProvider {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "seared_tank");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level level, BlockState blockState, IProbeHitData data) {
        BlockEntity blockEntity = level.getBlockEntity(data.getPos());
        if (blockEntity instanceof SearedTankBE tank) {
            probeInfo.text(ProbeTooltips.tankLine(tank.getFluidHandler()));
        }
    }
}
