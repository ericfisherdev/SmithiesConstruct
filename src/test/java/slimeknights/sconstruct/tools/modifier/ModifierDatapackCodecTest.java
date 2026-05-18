package slimeknights.sconstruct.tools.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

/**
 * Datapack-registry round-trip tests for {@link Modifier} (SMTCON-173). Where
 * {@link ModifierCodecTest} pins the one-way parse (JSON → concrete subclass) and the field
 * validators, this class proves the full {@code DIRECT_CODEC} round-trip — encode a fixture,
 * decode it, and assert value equality — for every {@link ModifierType} dispatch shape, plus
 * an encoded-shape assertion against a reference datapack fixture.
 *
 * <p>All five {@code ModifierType} permits currently share the same three-field instance
 * record {@code (id, max_level, slot_cost)}; one fixture per permit therefore exercises every
 * branch of the {@link Modifier#DIRECT_CODEC} dispatch and every permit's {@code instanceCodec}.
 */
class ModifierDatapackCodecTest {

    @Test
    void directCodecRoundTripsEveryModifierTypeVariantThroughJson() {
        // Encode → decode → equals for one fixture per ModifierType permit. A record's value
        // equals makes the assertion exact; a regression in any permit's MapCodec surfaces as
        // an inequality rather than a silent field drop.
        for (ModifierType type : ModifierType.All.VALUES) {
            Modifier fixture = fixtureFor(type);
            JsonElement json = Modifier.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, fixture).getOrThrow();
            Modifier decoded = Modifier.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            assertEquals(fixture, decoded, () -> "JSON round-trip must preserve the " + type.id() + " modifier");
        }
    }

    @Test
    void directCodecRoundTripsEveryModifierTypeVariantThroughNbt() {
        // Save-game NBT shares DIRECT_CODEC with datapack JSON; pin the binary ops surface so a
        // codec change that only breaks NBT is still caught.
        for (ModifierType type : ModifierType.All.VALUES) {
            Modifier fixture = fixtureFor(type);
            Tag encoded = Modifier.DIRECT_CODEC.encodeStart(NbtOps.INSTANCE, fixture).getOrThrow();
            Modifier decoded = Modifier.DIRECT_CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
            assertEquals(fixture, decoded, () -> "NBT round-trip must preserve the " + type.id() + " modifier");
        }
    }

    @Test
    void encodedJsonMatchesTheReferenceDatapackShape() {
        // Acceptance: generated JSON matches a reference fixture. The dispatch codec writes the
        // type discriminator alongside the instance fields — pin that flat shape so a future
        // dispatch-key rename breaks the test rather than every shipped modifier JSON.
        Modifier sharpness = new SimpleStatBoostType.Instance(ResourceLocation.fromNamespaceAndPath("sconstruct", "sharpness"), 5, 1);
        JsonElement encoded = Modifier.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, sharpness).getOrThrow();
        JsonElement expected = JsonParser.parseString("""
                {
                  "type": "simple_stat_boost",
                  "id": "sconstruct:sharpness",
                  "max_level": 5,
                  "slot_cost": 1
                }
                """);
        assertEquals(expected, encoded, "encoded modifier JSON must match the reference datapack shape");
    }

    @Test
    void directCodecDecodesEachReferenceFixtureToItsConcreteInstance() {
        // Round out the per-permit coverage from the decode side: a canonical JSON for each
        // type must instantiate that type's Instance record, confirming the dispatch table and
        // the type ids stay in lock-step.
        assertInstanceOf(SimpleStatBoostType.Instance.class, decode("simple_stat_boost"));
        assertInstanceOf(AttackTriggerType.Instance.class, decode("attack_trigger"));
        assertInstanceOf(MiningTriggerType.Instance.class, decode("mining_trigger"));
        assertInstanceOf(RightClickType.Instance.class, decode("right_click"));
        assertInstanceOf(OnBuildType.Instance.class, decode("on_build"));
    }

    /** Builds a three-field instance fixture for the given dispatch type. */
    private static Modifier fixtureFor(ModifierType type) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("sconstruct", "fixture_" + type.id());
        return switch (type) {
        case SimpleStatBoostType ignored -> new SimpleStatBoostType.Instance(id, 5, 1);
        case AttackTriggerType ignored -> new AttackTriggerType.Instance(id, 3, 2);
        case MiningTriggerType ignored -> new MiningTriggerType.Instance(id, 1, 0);
        case RightClickType ignored -> new RightClickType.Instance(id, 4, 1);
        case OnBuildType ignored -> new OnBuildType.Instance(id, 2, 3);
        };
    }

    /** Parses a minimal valid modifier JSON for the given dispatch type id. */
    private static Modifier decode(String typeId) {
        JsonElement json = JsonParser.parseString("""
                { "type": "%s", "id": "sconstruct:ref_%s", "max_level": 1, "slot_cost": 1 }
                """.formatted(typeId, typeId));
        return Modifier.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }
}
