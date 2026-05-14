package slimeknights.tconstruct.port1211.data;

import java.util.Map;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import slimeknights.tconstruct.port1211.TConstruct;
import slimeknights.tconstruct.port1211.shared.Metal;
import slimeknights.tconstruct.port1211.shared.SharedBlocks;
import slimeknights.tconstruct.port1211.shared.SharedItems;
import slimeknights.tconstruct.port1211.shared.SharedMetals;

/**
 * {@code en_us} translation data provider. Baseline for the other twelve locales — those land
 * as static JSONs in a later migration task; this provider only owns English.
 *
 * <p>Entries are produced per registered object using the {@link LanguageProvider} helpers
 * ({@code add(Block, String)}, {@code add(Item, String)},
 * {@code add(CreativeModeTab, String)}), so the lang key is derived from the registered name
 * automatically and a future rename only requires editing here.
 *
 * <p>Metal display names use vanilla's "Block of X" / "X Ingot" / "X Nugget" templates so the
 * UI feels native. A few metals carry irregular names ({@code pigiron} → "Pig Iron",
 * {@code alubrass} → "Aluminum Brass") — those overrides live in {@link #METAL_DISPLAY_NAMES}
 * so the otherwise-derived title-casing stays the default.
 */
public final class TinkerLanguageProvider extends LanguageProvider {

    /** Display-name overrides for metal ids that don't title-case cleanly. */
    private static final Map<String, String> METAL_DISPLAY_NAMES = Map.of("pigiron", "Pig Iron", "alubrass", "Aluminum Brass");

    public TinkerLanguageProvider(PackOutput output) {
        super(output, TConstruct.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // Creative tab. LanguageProvider has no CreativeModeTab overload, so use the raw key.
        // Must match the lang key SharedTabs#GENERAL builder set via Component.translatable.
        add("itemGroup.tconstruct", "Tinkers' Construct");

        // Metal storage blocks: "Block of Cobalt" (vanilla iron_block convention).
        // Ingots: "<Metal> Ingot"; nuggets: "<Metal> Nugget".
        int metals = SharedMetals.ALL.size();
        if (SharedItems.INGOTS.size() != metals || SharedItems.NUGGETS.size() != metals) {
            throw new IllegalStateException("SharedMetals.ALL / SharedItems.INGOTS / SharedItems.NUGGETS must be parallel in size");
        }
        for (int i = 0; i < metals; i++) {
            Metal metal = SharedMetals.ALL.get(i);
            String name = metalDisplayName(metal.id());
            if (SharedBlocks.METAL_BLOCKS.containsKey(metal.id())) {
                add(SharedBlocks.METAL_BLOCKS.get(metal.id()).get(), "Block of " + name);
            }
            // INGOTS / NUGGETS index matches SharedMetals.ALL ordering — pinned by
            // TinkerItemTagsProvider and ingotsAndNuggetsAreParallelByIndex.
            add(SharedItems.INGOTS.get(i).get(), name + " Ingot");
            add(SharedItems.NUGGETS.get(i).get(), name + " Nugget");
        }

        // Decoratives.
        add(SharedBlocks.GLOW.get(), "Glow");
        add(SharedBlocks.FIREWOOD.get(), "Firewood");
        add(SharedBlocks.LAVAWOOD.get(), "Lavawood");

        // Slimeballs (parity with vanilla "Slime Ball" capitalisation).
        add(SharedItems.SLIMEBALL_BLUE.get(), "Blue Slime Ball");
        add(SharedItems.SLIMEBALL_PURPLE.get(), "Purple Slime Ball");
        add(SharedItems.SLIMEBALL_BLOOD.get(), "Blood Slime Ball");
        add(SharedItems.SLIMEBALL_MAGMA.get(), "Magma Slime Ball");

        // Miscellaneous items.
        add(SharedItems.BACON.get(), "Bacon");
        add(SharedItems.MUDBRICK.get(), "Mud Brick");
        add(SharedItems.BUCKET_BLOOD.get(), "Bucket of Blood");

        // Blood fluid description id. Set explicitly here because Fluid#getDescriptionId is
        // a separate key from the BucketItem above ({@code fluid.tconstruct.blood} vs
        // {@code item.tconstruct.blood_bucket}).
        add("fluid.tconstruct.blood", "Blood");
    }

    /**
     * Display name for a metal id; falls back to a title-cased version of the raw id. The
     * Metal record's canonical constructor rejects blank ids, so the empty-string guard here
     * is defence-in-depth — if a caller ever invokes this directly with an empty string the
     * fall through returns it verbatim instead of throwing StringIndexOutOfBoundsException.
     */
    private static String metalDisplayName(String id) {
        String override = METAL_DISPLAY_NAMES.get(id);
        if (override != null) {
            return override;
        }
        if (id.isEmpty()) {
            return id;
        }
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }
}
