package slimeknights.sconstruct.tools.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.tools.PartType;

/**
 * Pinned-behaviour tests for {@link Material} — the data unit of the tools system. Covers the
 * SMTCON-68 acceptance criteria: codec round-trips through JSON and NBT, the registry key is
 * registered against the right id, and the optional fields actually round-trip when absent.
 */
class MaterialTest {

    private static final ResourceLocation WOOD = ResourceLocation.fromNamespaceAndPath("tconstruct", "wood");
    private static final TagKey<Item> PLANKS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "planks"));

    @Test
    void registryKeyLivesUnderTheModNamespaceMaterialPath() {
        // The datapack registry id drives the data/<modid>/material/<file>.json path. Pinning
        // the namespace + path catches a future rename that would silently break every
        // shipped material JSON.
        ResourceKey<net.minecraft.core.Registry<Material>> key = Material.REGISTRY_KEY;
        assertEquals(SConstruct.MOD_ID, key.location().getNamespace());
        assertEquals("material", key.location().getPath());
    }

    @Test
    void directCodecRoundTripsAFullMaterialThroughJson() {
        Material wood = new Material(WOOD, 1, Optional.of(PLANKS_TAG),
                Map.of(PartType.HANDLE, new HandleStats(1.0f, 1.0f, 1.0f), PartType.PICKHEAD, new HeadStats(60, 0, 2.0f, 2.0f), PartType.BOWLIMB, new BowStats(20, 1.0f, 0.5f)),
                List.of(new MaterialTrait(ResourceLocation.fromNamespaceAndPath("tconstruct", "ecological"), PartType.PICKHEAD)), 0xFF8B5A2B);
        JsonElement json = Material.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, wood).getOrThrow();
        Material decoded = Material.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(wood, decoded);
    }

    @Test
    void directCodecRoundTripsAMinimalMaterialThroughNbt() {
        // Optional fields absent: no repair_tag, empty stats map, empty traits list, default
        // color. Round-trip must preserve the absence of repair_tag and not synthesise a value.
        Material minimal = new Material(WOOD, 0, Optional.empty(), Map.of(), List.of(), 0xFFFFFFFF);
        Tag encoded = Material.DIRECT_CODEC.encodeStart(NbtOps.INSTANCE, minimal).getOrThrow();
        Material decoded = Material.DIRECT_CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(minimal, decoded);
        assertTrue(decoded.repairTag().isEmpty(), "absent repair_tag must round-trip as empty");
    }

    @Test
    void compactConstructorDefensivelyCopiesStatsAndTraits() {
        // The record carries a Map / List — without the copyOf defensive copy the caller could
        // mutate the live state after construction. Locking the copy in stops a subtle alias
        // bug.
        java.util.Map<PartType, MaterialStats> mutableStats = new java.util.EnumMap<>(PartType.class);
        mutableStats.put(PartType.HANDLE, new HandleStats(1.0f, 1.0f, 1.0f));
        java.util.List<MaterialTrait> mutableTraits = new java.util.ArrayList<>();
        mutableTraits.add(new MaterialTrait(WOOD, PartType.HANDLE));

        Material material = new Material(WOOD, 1, Optional.empty(), mutableStats, mutableTraits, 0);

        mutableStats.clear();
        mutableTraits.clear();

        assertEquals(1, material.stats().size(), "stats copy must not see post-construction mutation");
        assertEquals(1, material.traits().size(), "traits copy must not see post-construction mutation");
        assertThrows(UnsupportedOperationException.class, () -> material.stats().put(PartType.PICKHEAD, new HeadStats(1, 0, 1.0f, 1.0f)), "stats must be unmodifiable");
        assertThrows(UnsupportedOperationException.class, () -> material.traits().add(new MaterialTrait(WOOD, PartType.HANDLE)), "traits must be unmodifiable");
    }

}
