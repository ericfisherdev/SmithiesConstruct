package slimeknights.tconstruct.port1211.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import slimeknights.tconstruct.port1211.TConstruct;

/**
 * Phase-1 stub for tconstruct en_us translations. Empty body; Phase 2+ pulses append per-item
 * {@code add(...)} calls inside {@link #addTranslations}. Other locales arrive as
 * upstream-translation PRs land on top of this baseline.
 */
public final class TinkerLanguageProvider extends LanguageProvider {

    public TinkerLanguageProvider(PackOutput output) {
        super(output, TConstruct.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // No translations in Phase 1.
    }
}
