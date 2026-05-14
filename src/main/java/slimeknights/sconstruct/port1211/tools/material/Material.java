package slimeknights.sconstruct.port1211.tools.material;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.tools.PartType;

/**
 * The central data unit of the tools system: a single material identity (wood, iron, slime, …)
 * with the inputs every tool stage reads from — tier, repair-item tag, per-{@link PartType}
 * stat block, trait grants, and a render-time colour. Materials live in a NeoForge datapack
 * registry under {@code data/<modid>/material/} so addons can ship JSON files without
 * registering anything code-side.
 *
 * <ul>
 *   <li>{@code id} — registry id; matches the JSON file path.</li>
 *   <li>{@code tier} — 0..n harvest-tier baseline; tools take the {@code max} across heads.</li>
 *   <li>{@code repairTag} — optional item tag whose contents repair a tool made of this
 *       material at the anvil. Optional because not every material has a repair shortcut
 *       (slime, paper).</li>
 *   <li>{@code stats} — per-part-type stat block; absent entries mean the material is not
 *       valid for that slot (a stone material with no {@code bowlimb} entry cannot be a bow
 *       limb).</li>
 *   <li>{@code traits} — ordered list of trait grants applied at tool-build time.</li>
 *   <li>{@code color} — packed ARGB tint applied to the part item by the {@code ItemColors}
 *       handler (SMTCON-72).</li>
 * </ul>
 *
 * <p>{@link #REGISTRY_KEY} is the {@link ResourceKey} for the datapack registry itself; pass it
 * to {@code BootstrapContext}, {@code RegistryAccess}, or
 * {@code DataPackRegistryEvent.NewRegistry#dataPackRegistry} to address the registry. The
 * registration of the registry happens in {@code MaterialRegistry#onNewRegistry}; this record
 * only defines the value shape and codec.
 */
public record Material(ResourceLocation id, int tier, Optional<TagKey<Item>> repairTag, Map<PartType, MaterialStats> stats, List<MaterialTrait> traits, int color) {

    /**
     * Registry key for the {@code sconstruct:material} datapack registry. The registry holds
     * {@link Material} entries by {@link ResourceLocation}; callers look up materials through
     * {@code RegistryAccess.lookupOrThrow(Material.REGISTRY_KEY).get(rl)} or via the
     * server-side cache in {@code MaterialRegistry}.
     */
    public static final ResourceKey<net.minecraft.core.Registry<Material>> REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "material"));

    /**
     * Datapack-side JSON codec. Field-by-field RecordCodecBuilder; the {@code stats} map keys
     * use {@link PartType#CODEC} so JSON entries read as {@code {"pickhead": {...}}} rather
     * than ordinal indices.
     *
     * <p>{@code repairTag} is optional — materials without a vanilla-item repair shortcut omit
     * the field. The tag codec runs against {@link Registries#ITEM} so the JSON form is
     * {@code "#minecraft:planks"} rather than a raw resource location.
     */
    public static final Codec<Material> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance
            .group(ResourceLocation.CODEC.fieldOf("id").forGetter(Material::id), Codec.INT.fieldOf("tier").forGetter(Material::tier),
                    TagKey.codec(Registries.ITEM).optionalFieldOf("repair_tag").forGetter(Material::repairTag),
                    Codec.unboundedMap(PartType.CODEC, MaterialStats.Placeholder.CODEC).optionalFieldOf("stats", Map.of()).forGetter(Material::stats),
                    MaterialTrait.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(Material::traits), Codec.INT.optionalFieldOf("color", 0xFFFFFFFF).forGetter(Material::color))
            .apply(instance, Material::new));

    /** Defensive-copy compact constructor — the {@code Map} and {@code List} arguments are
     *  copied so the record stays truly immutable even when a caller mutates the source. */
    public Material {
        stats = Map.copyOf(stats);
        traits = List.copyOf(traits);
    }
}
