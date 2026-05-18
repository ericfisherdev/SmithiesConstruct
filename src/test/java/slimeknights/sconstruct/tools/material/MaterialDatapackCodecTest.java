package slimeknights.sconstruct.tools.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import slimeknights.sconstruct.tools.PartType;

/**
 * Datapack-registry round-trip tests for {@link Material} (SMTCON-173). Where
 * {@link MaterialTest} pins the codec wiring and the optional-field handling, this class proves
 * a fully-populated datapack JSON survives a {@code DIRECT_CODEC} round-trip with no value loss
 * and that the encoded JSON matches the byte shape a hand-authored datapack file would carry.
 *
 * <p>The headline fixture is a single material that occupies every {@link PartType} slot whose
 * stats need one of the five {@link MaterialStats} permits — so one round-trip exercises
 * {@link HeadStats}, {@link HandleStats}, {@link ExtraStats}, {@link BowStats}, and
 * {@link ArrowStats} together through the {@link MaterialStatsCodecs} dispatch.
 *
 * <p>All floating-point stat values are exactly representable in IEEE-754 binary32 (integers
 * and power-of-two fractions). JSON-shape equality compares numbers by {@code double} value, so
 * a value like {@code 1.1f} — whose nearest float widens to {@code 1.100000023841858d} — would
 * not equal a literal {@code 1.1} parsed from the reference fixture. Exact values keep the
 * shape assertions honest.
 */
class MaterialDatapackCodecTest {

    private static final ResourceLocation OBSIDIAN = ResourceLocation.fromNamespaceAndPath("tconstruct", "obsidian");
    private static final TagKey<Item> PLANKS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "planks"));

    /** Builds the all-five-stat-types fixture used across the round-trip assertions. */
    private static Material everyStatTypeMaterial() {
        return new Material(OBSIDIAN, 5, Optional.of(PLANKS_TAG),
                Map.of(PartType.PICKHEAD, new HeadStats(250, 3, 7.0f, 5.0f), PartType.HANDLE, new HandleStats(1.0f, 1.25f, 0.75f), PartType.BINDING, new ExtraStats(75), PartType.BOWLIMB,
                        new BowStats(30, 1.5f, 0.5f), PartType.ARROWSHAFT, new ArrowStats(0.75f, 12)),
                List.of(new MaterialTrait(ResourceLocation.fromNamespaceAndPath("tconstruct", "reinforced"), PartType.PICKHEAD)), 0x8B5A2B);
    }

    @Test
    void directCodecRoundTripsAMaterialCarryingAllFiveStatRecordTypes() {
        // One material, one round-trip, every MaterialStats permit: a regression in any single
        // stat MapCodec or in the MaterialStatsCodecs dispatch surfaces here.
        Material material = everyStatTypeMaterial();
        JsonElement json = Material.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, material).getOrThrow();
        Material decoded = Material.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(material, decoded, "JSON round-trip must preserve every stat record type");
    }

    @Test
    void directCodecRoundTripsTheAllStatMaterialThroughNbt() {
        // Datapack JSON and save-game NBT both flow through DIRECT_CODEC; pin the NBT pass so a
        // codec change that only breaks the binary ops surface is still caught.
        Material material = everyStatTypeMaterial();
        Tag encoded = Material.DIRECT_CODEC.encodeStart(NbtOps.INSTANCE, material).getOrThrow();
        Material decoded = Material.DIRECT_CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(material, decoded, "NBT round-trip must preserve every stat record type");
    }

    @Test
    void encodedJsonMatchesTheReferenceDatapackShape() {
        // Acceptance: generated JSON matches a reference fixture. Pinning the byte shape — not
        // just round-trip survival — catches a field rename that would still round-trip
        // (encoder and decoder agree) but break every hand-authored datapack file on disk.
        Material material = everyStatTypeMaterial();
        JsonElement encoded = Material.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, material).getOrThrow();
        JsonElement expected = JsonParser.parseString("""
                {
                  "id": "tconstruct:obsidian",
                  "tier": 5,
                  "repair_tag": "minecraft:planks",
                  "stats": {
                    "pickhead":   { "type": "head",   "durability": 250, "harvest_level": 3, "mining_speed": 7.0, "attack_damage": 5.0 },
                    "handle":     { "type": "handle", "durability_modifier": 1.0, "mining_speed_modifier": 1.25, "attack_speed_modifier": 0.75 },
                    "binding":    { "type": "extra",  "extra_durability": 75 },
                    "bowlimb":    { "type": "bow",    "draw_speed": 30, "range_multiplier": 1.5, "damage_bonus": 0.5 },
                    "arrowshaft": { "type": "arrow",  "weight": 0.75, "extra_durability": 12 }
                  },
                  "traits": [ { "trait": "tconstruct:reinforced", "slot": "pickhead" } ],
                  "color": 9132587
                }
                """);
        assertEquals(expected, encoded, "encoded material JSON must match the reference datapack shape");
    }

    @Test
    void tierFiveMaterialWithEveryPartStatRoundTrips() {
        // Edge case from the ticket: a tier-5 material that fills every PartType slot. Each
        // head-class slot carries HeadStats, every other slot carries a stat record valid for
        // that role — the widest stats map a single material can ship.
        Map<PartType, MaterialStats> stats = new EnumMap<>(PartType.class);
        for (PartType part : PartType.values()) {
            stats.put(part, statsFor(part));
        }
        Material maxed = new Material(OBSIDIAN, 5, Optional.of(PLANKS_TAG), stats, List.of(), 0xFFFFFF);
        JsonElement json = Material.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, maxed).getOrThrow();
        Material decoded = Material.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(maxed, decoded, "tier-5 material with every part stat must round-trip");
        assertEquals(PartType.values().length, decoded.stats().size(), "every PartType slot must survive the round-trip");
    }

    /** Picks a representative stat record for the given slot: heads take {@link HeadStats},
     *  bow / arrow slots take their ranged records, everything else takes {@link ExtraStats}. */
    private static MaterialStats statsFor(PartType part) {
        if (part.isHead()) {
            return new HeadStats(250, 3, 7.0f, 5.0f);
        }
        return switch (part) {
        case HANDLE, TOUGHHANDLE -> new HandleStats(1.0f, 1.25f, 0.75f);
        case BOWLIMB -> new BowStats(30, 1.5f, 0.5f);
        case ARROWSHAFT, FLETCHING, ARROW_HEAD -> new ArrowStats(0.75f, 12);
        default -> new ExtraStats(75);
        };
    }
}
