package slimeknights.sconstruct.port1211.data;

import java.util.Map;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.shared.Metal;
import slimeknights.sconstruct.port1211.shared.SharedBlocks;
import slimeknights.sconstruct.port1211.shared.SharedItems;
import slimeknights.sconstruct.port1211.shared.SharedMetals;
import slimeknights.sconstruct.port1211.world.SlimeFluidSet;
import slimeknights.sconstruct.port1211.world.WorldBlocks;
import slimeknights.sconstruct.port1211.world.WorldFluids;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

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
        super(output, SConstruct.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // Creative tab. LanguageProvider has no CreativeModeTab overload, so use the raw key.
        // Must match the lang key SharedTabs#GENERAL builder set via Component.translatable.
        add("itemGroup.sconstruct", "Smithies' Construct");

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
        // a separate key from the BucketItem above ({@code fluid.sconstruct.blood} vs
        // {@code item.sconstruct.blood_bucket}).
        add("fluid.sconstruct.blood", "Blood");

        // World pulse: four coloured slime fluids. Iterate WorldFluids.ALL so a new colour
        // lights up here automatically (matches the SharedMetals iteration pattern above).
        for (SlimeFluidSet fluid : WorldFluids.ALL) {
            String displayColor = slimeColorDisplayName(fluid);
            // LiquidBlock auto-derives "block.sconstruct.<path>" from the registered name.
            add(fluid.block().get(), displayColor + " Slime");
            // BucketItem auto-derives "item.sconstruct.<path>_bucket" from the registered name.
            add(fluid.bucket().get(), "Bucket of " + displayColor + " Slime");
            // Fluid descriptionId is explicit; the FluidType ctor sets it to
            // "fluid.sconstruct.slime_<color>" and there is no add(FluidType, String) helper.
            add("fluid.sconstruct.slime_" + slimeColorId(fluid), "Liquid " + displayColor + " Slime");
        }

        // World pulse: four bouncy slime blocks. Iterate WorldBlocks.SLIME_BLOCKS keyed by
        // SlimeColor so a new colour added in the enum + WorldBlocks lights up here with no
        // edit.
        for (Map.Entry<SlimeColor, ? extends net.neoforged.neoforge.registries.DeferredBlock<?>> entry : WorldBlocks.SLIME_BLOCKS.entrySet()) {
            String colorId = entry.getKey().id();
            String display = Character.toUpperCase(colorId.charAt(0)) + colorId.substring(1);
            add(entry.getValue().get(), display + " Slime Block");
        }

        // World pulse: four plant sets, four entries each (dirt + grass + leaves + sapling).
        // Iterate WorldBlocks.PLANT_SETS so a new colour lights up the lang entries with one
        // line in WorldBlocks plus a single SlimeColor constant.
        for (Map.Entry<SlimeColor, slimeknights.sconstruct.port1211.world.block.SlimePlantSet> entry : WorldBlocks.PLANT_SETS.entrySet()) {
            String colorId = entry.getKey().id();
            String display = Character.toUpperCase(colorId.charAt(0)) + colorId.substring(1);
            slimeknights.sconstruct.port1211.world.block.SlimePlantSet set = entry.getValue();
            add(set.dirt().get(), display + " Slime Dirt");
            add(set.grass().get(), display + " Slime Grass");
            add(set.leaves().get(), display + " Slime Leaves");
            add(set.sapling().get(), display + " Slime Sapling");
        }

        // World pulse: four slime log blocks plus their stripped variants — 8 entries total.
        for (Map.Entry<SlimeColor, net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock>> entry : WorldBlocks.SLIME_LOGS.entrySet()) {
            String colorId = entry.getKey().id();
            String display = Character.toUpperCase(colorId.charAt(0)) + colorId.substring(1);
            add(entry.getValue().get(), display + " Slime Log");
        }
        for (Map.Entry<SlimeColor, net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock>> entry : WorldBlocks.STRIPPED_SLIME_LOGS.entrySet()) {
            String colorId = entry.getKey().id();
            String display = Character.toUpperCase(colorId.charAt(0)) + colorId.substring(1);
            add(entry.getValue().get(), "Stripped " + display + " Slime Log");
        }

        // World pulse: slime mob entities. entity.<modid>.<path> is the conventional auto-key
        // for entity types; LanguageProvider has no add(EntityType, String) overload so the
        // raw key is written explicitly.
        add(slimeknights.sconstruct.port1211.world.WorldEntities.BLUESLIME.get(), "Blueslime");
        add(slimeknights.sconstruct.port1211.world.WorldEntities.HUGESLIME.get(), "Huge Slime");
    }

    private static String slimeColorId(SlimeFluidSet fluid) {
        // Path is "sconstruct:slime_<color>" — strip the "slime_" prefix to recover the colour
        // segment for display-name and lang-key derivation.
        String path = fluid.source().getId().getPath();
        if (!path.startsWith("slime_")) {
            throw new IllegalStateException("expected SlimeFluidSet source path to start with 'slime_', got '" + path + "'");
        }
        return path.substring("slime_".length());
    }

    private static String slimeColorDisplayName(SlimeFluidSet fluid) {
        String id = slimeColorId(fluid);
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
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
        String[] words = id.split("_");
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                words[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
            }
        }
        return String.join(" ", words);
    }
}
