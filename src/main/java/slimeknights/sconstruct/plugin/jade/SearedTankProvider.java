package slimeknights.sconstruct.plugin.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.smeltery.block.entity.SearedTankBE;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade tooltip provider for a standalone seared tank (SMTCON-164). Shows the tank's fluid
 * contents when the player looks at an IO or input tank block, whether or not the tank is part
 * of an assembled smeltery.
 *
 * <p>Like {@link SmelteryProvider}, the tank snapshot is written server-side in
 * {@link #appendServerData} and read back client-side in {@link #appendTooltip}.
 */
public final class SearedTankProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    /** Shared singleton — Jade registers one provider instance for both hooks. */
    public static final SearedTankProvider INSTANCE = new SearedTankProvider();

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "seared_tank");

    private SearedTankProvider() {
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof SearedTankBE tank) {
            JadeTankData.writeTank(data, tank.getFluidHandler());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        tooltip.add(JadeTankData.tankLine(accessor.getServerData()));
    }
}
