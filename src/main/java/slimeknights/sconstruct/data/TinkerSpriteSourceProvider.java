package slimeknights.sconstruct.data;

import static slimeknights.sconstruct.lib.util.Util.rl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SpriteSourceProvider;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.tools.PartType;

/**
 * Datagen provider that emits the {@code minecraft:blocks} atlas {@code PalettedPermutations}
 * source which materialises one sprite per (part-type, material) pair at runtime — replacing
 * the legacy 1.12 "ship one PNG per (part × material) combination" approach with a single
 * grayscale base sprite per part, a canonical palette key, and one short palette PNG per
 * material (SMTCON-106).
 *
 * <p>The atlas materialises {@code <namespace>:<base>_<material>} sprites for every entry in
 * the cross-product of {@link #BASE_TEXTURES} and {@link #MATERIAL_PALETTES}. Vanilla 1.21.1's
 * {@link PalettedPermutations#run} walks the sources at resource-reload time and writes the
 * permuted bitmaps into the {@code SpriteSource.Output}, so the resulting sprite paths line up
 * with the texture references emitted by the per-material baked tool model in SMTCON-105.
 *
 * <p>The JSON file written here is structural — it points at PNG paths but does not require
 * them to exist at JSON-generation time. PNG asset shipping (the grayscale base for each part
 * + the palette key + per-material palette strips) lands separately; until then, runtime
 * resolution falls through to the vanilla "missing texture" sentinel, which keeps the client
 * crash-free.
 */
public final class TinkerSpriteSourceProvider extends SpriteSourceProvider {

    /** Texture-folder root for tool-part sprites. Centralised so a future rename is one edit. */
    private static final String PARTS_FOLDER = "item/parts";

    /**
     * Canonical palette key — the 8-step grayscale gradient image that each material palette is
     * mapped against. Stored under {@code item/parts/_palette_key.png} per the standard
     * vanilla convention for paletted-permutations atlases.
     */
    public static final ResourceLocation PALETTE_KEY = rl(PARTS_FOLDER + "/_palette_key");

    /**
     * Ordered list of grayscale base textures, one per {@link PartType}. The runtime permuter
     * applies each material palette against every entry here. Path convention matches the AC
     * in SMTCON-106: {@code <modid>:item/parts/<partid>_grayscale}.
     */
    public static final List<ResourceLocation> BASE_TEXTURES;

    /**
     * Material id → palette PNG. The palette PNG is an 8×1-pixel strip whose colours map
     * one-to-one onto the {@link #PALETTE_KEY}'s grayscale gradient. The map iteration order
     * is preserved (LinkedHashMap) so the generated JSON's permutation block is deterministic
     * across runs — important for reproducible datagen output.
     */
    public static final Map<String, ResourceLocation> MATERIAL_PALETTES;

    static {
        BASE_TEXTURES = buildBaseTextures();
        MATERIAL_PALETTES = buildMaterialPalettes();
    }

    public TinkerSpriteSourceProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper) {
        super(output, registries, SConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void gather() {
        atlas(BLOCKS_ATLAS).addSource(new PalettedPermutations(BASE_TEXTURES, PALETTE_KEY, MATERIAL_PALETTES));
    }

    private static List<ResourceLocation> buildBaseTextures() {
        List<ResourceLocation> list = new java.util.ArrayList<>(PartType.values().length);
        for (PartType part : PartType.values()) {
            list.add(rl(PARTS_FOLDER + "/" + part.id() + "_grayscale"));
        }
        return List.copyOf(list);
    }

    private static Map<String, ResourceLocation> buildMaterialPalettes() {
        // Materials registered by TinkerMaterialBootstrap, ordered to match its declaration so
        // the emitted JSON's permutation block follows the same canonical order. New materials
        // get appended in the bootstrap class and mirrored here in a single edit.
        String[] materials = { "wood", "stone", "iron", "gold", "flint", "bone", "paper", "slime", "blueslime", "cobalt", "ardite", "manyullyn", "copper", "silver", "steel" };
        Map<String, ResourceLocation> palettes = new LinkedHashMap<>();
        for (String material : materials) {
            palettes.put(material, rl(PARTS_FOLDER + "/_palette_" + material));
        }
        return java.util.Collections.unmodifiableMap(palettes);
    }
}
