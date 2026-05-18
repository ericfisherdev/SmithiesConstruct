package slimeknights.sconstruct.plugin.jade;

import slimeknights.sconstruct.smeltery.block.SearedTankBlock;
import slimeknights.sconstruct.smeltery.block.SmelteryControllerBlock;
import slimeknights.sconstruct.smeltery.block.entity.SearedTankBE;
import slimeknights.sconstruct.smeltery.block.entity.SmelteryControllerBlockEntity;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * The mod's Jade integration entry point (SMTCON-164). Jade discovers this class through the
 * {@link WailaPlugin} annotation and calls back into the {@code IWailaPlugin} hooks during its
 * startup.
 *
 * <p>{@link #register} attaches the server-side data providers — they run on the logical server
 * and snapshot block-entity state into the synced tooltip NBT. {@link #registerClient} attaches
 * the same provider instances as client-side tooltip components, keyed by the block class.
 * Registering the abstract {@link SearedTankBlock} covers both the IO and input tank variants.
 */
@WailaPlugin
public final class SmithiesJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(SmelteryProvider.INSTANCE, SmelteryControllerBlockEntity.class);
        registration.registerBlockDataProvider(SearedTankProvider.INSTANCE, SearedTankBE.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(SmelteryProvider.INSTANCE, SmelteryControllerBlock.class);
        registration.registerBlockComponent(SearedTankProvider.INSTANCE, SearedTankBlock.class);
    }
}
