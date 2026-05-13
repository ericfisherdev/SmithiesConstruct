package slimeknights.tconstruct.port1211.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import slimeknights.tconstruct.port1211.TConstruct;

/**
 * Phase-1 stub for tconstruct blockstate JSON + block-model generation. Empty body; Phase 2+
 * pulses append per-block calls inside {@link #registerStatesAndModels}.
 */
public final class TinkerBlockStateProvider extends BlockStateProvider {

    public TinkerBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, TConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // No blockstates or block models in Phase 1.
    }
}
