package slimeknights.sconstruct.port1211.plugin.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryControllerBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade tooltip provider for the smeltery controller (SMTCON-164). Shows the smeltery's current
 * internal temperature and the contents of its fluid tank when the player looks at the
 * controller block.
 *
 * <p>{@link #appendServerData} runs on the logical server and snapshots the temperature and
 * tank into the synced {@link CompoundTag}; {@link #appendTooltip} runs on the client and reads
 * that snapshot back, so no client-side block-entity access is needed.
 */
public final class SmelteryProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    /** Shared singleton — Jade registers one provider instance for both hooks. */
    public static final SmelteryProvider INSTANCE = new SmelteryProvider();

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "smeltery");
    private static final String KEY_TEMPERATURE = "Temperature";

    private SmelteryProvider() {
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof SmelteryControllerBlockEntity controller) {
            data.putInt(KEY_TEMPERATURE, controller.getCurrentTemperature());
            JadeTankData.writeTank(data, controller.getFluidHandler());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(KEY_TEMPERATURE)) {
            tooltip.add(Component.translatable("gui." + SConstruct.MOD_ID + ".jade.temperature", data.getInt(KEY_TEMPERATURE)));
        }
        tooltip.add(JadeTankData.tankLine(data));
    }
}
