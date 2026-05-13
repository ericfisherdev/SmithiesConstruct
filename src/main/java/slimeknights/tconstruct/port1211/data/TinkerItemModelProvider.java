package slimeknights.tconstruct.port1211.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import slimeknights.tconstruct.port1211.TConstruct;

/**
 * Phase-1 stub for tconstruct item-model JSON generation. Empty body; Phase 2+ pulses append
 * per-item calls inside {@link #registerModels}.
 */
public final class TinkerItemModelProvider extends ItemModelProvider {

    public TinkerItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, TConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // No item models in Phase 1.
    }
}
