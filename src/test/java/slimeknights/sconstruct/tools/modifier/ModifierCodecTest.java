package slimeknights.sconstruct.tools.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

/**
 * Pinned-behaviour tests for the {@link Modifier} / {@link ModifierType} dispatch codec.
 * Covers SMTCON-82's acceptance criteria: a modifier JSON parses into the concrete subclass
 * picked by the {@code type} discriminator, every {@link ModifierType} permit is reachable
 * via {@link ModifierType#CODEC}, and an unknown {@code type} fails the parse with a
 * {@link DataResult#error} rather than throwing.
 */
class ModifierCodecTest {

    @Test
    void registryKeyLandsInSconstructNamespace() {
        // The datapack registry path is derived from the registry key — JSON entries land at
        // data/<entry-namespace>/sconstruct/modifier/<entry-path>.json. Pin the registry side
        // of that contract.
        assertEquals("sconstruct", Modifier.REGISTRY_KEY.location().getNamespace());
        assertEquals("modifier", Modifier.REGISTRY_KEY.location().getPath());
    }

    @Test
    void modifierTypeCodecRoundTripsEveryPermit() {
        // Every singleton in ModifierType.All.VALUES round-trips through CODEC by its id —
        // protects against a future rename of a permit's id() that would silently break
        // datapacks referencing the old name.
        for (ModifierType type : ModifierType.All.VALUES) {
            DataResult<ModifierType> decoded = ModifierType.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(type.id()));
            assertTrue(decoded.result().isPresent(), () -> "type " + type.id() + " must round-trip");
            assertSame(type, decoded.result().get(), () -> "decoded singleton must equal source for " + type.id());
        }
    }

    @Test
    void modifierDirectCodecRejectsNonPositiveMaxLevel() {
        // The shared MAX_LEVEL_CODEC field validator rejects max_level <= 0 at parse time,
        // surfacing the bad input as a DataResult.error rather than letting the record
        // constructor throw an unhandled IllegalArgumentException.
        JsonObject json = new JsonObject();
        json.addProperty("type", "simple_stat_boost");
        json.addProperty("id", "sconstruct:bad_max_level");
        json.addProperty("max_level", 0);
        json.addProperty("slot_cost", 1);
        DataResult<Modifier> decoded = Modifier.DIRECT_CODEC.parse(JsonOps.INSTANCE, json);
        assertTrue(decoded.error().isPresent(), "max_level=0 must surface as DataResult.error");
    }

    @Test
    void modifierDirectCodecRejectsNegativeSlotCost() {
        // SLOT_COST_CODEC rejects slot_cost < 0 — same DataResult.error surface as the
        // max_level rejection. Pin so both validators stay codec-side rather than relying on
        // the record constructor to be the failure point.
        JsonObject json = new JsonObject();
        json.addProperty("type", "simple_stat_boost");
        json.addProperty("id", "sconstruct:bad_slot_cost");
        json.addProperty("max_level", 1);
        json.addProperty("slot_cost", -1);
        DataResult<Modifier> decoded = Modifier.DIRECT_CODEC.parse(JsonOps.INSTANCE, json);
        assertTrue(decoded.error().isPresent(), "slot_cost=-1 must surface as DataResult.error");
    }

    @Test
    void modifierTypeCodecRejectsUnknownId() {
        // Unknown ids must surface as DataResult.error rather than throwing — datapack JSON
        // that ships a type the mod doesn't know fails gracefully.
        DataResult<ModifierType> decoded = ModifierType.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("totally_not_a_real_type"));
        assertTrue(decoded.error().isPresent());
    }

    @Test
    void modifierDirectCodecRoutesJsonThroughTheDispatchedType() {
        // Acceptance: loading a modifier JSON instantiates the concrete subclass selected by
        // the type discriminator. Build a canonical SimpleStatBoost JSON and parse it through
        // the direct codec; the result must be a SimpleStatBoostType.Instance carrying the
        // declared fields.
        JsonObject json = JsonParser.parseString("""
                {
                    "type": "simple_stat_boost",
                    "id": "sconstruct:sharpness",
                    "max_level": 5,
                    "slot_cost": 1
                }
                """).getAsJsonObject();

        DataResult<Modifier> decoded = Modifier.DIRECT_CODEC.parse(JsonOps.INSTANCE, json);
        assertTrue(decoded.result().isPresent(), () -> "parse must succeed: " + decoded.error().map(err -> err.message()).orElse(""));
        Modifier modifier = decoded.result().get();
        assertInstanceOf(SimpleStatBoostType.Instance.class, modifier, "dispatch must pick SimpleStatBoostType.Instance for type=simple_stat_boost");
        assertEquals(ResourceLocation.fromNamespaceAndPath("sconstruct", "sharpness"), modifier.id());
        assertEquals(5, modifier.maxLevel());
        assertEquals(1, modifier.slotCost());
        assertSame(SimpleStatBoostType.INSTANCE, modifier.type(), "concrete instance must report its singleton type");
    }

    @Test
    void modifierDirectCodecRoutesEveryDispatchShape() {
        // Every ModifierType permit must be reachable from a JSON payload — pin the routing
        // for the other four shapes so a future change to dispatch doesn't silently break
        // any single permit.
        Modifier attack = parse("attack_trigger");
        assertInstanceOf(AttackTriggerType.Instance.class, attack, "attack_trigger routes to AttackTriggerType");
        assertSame(AttackTriggerType.INSTANCE, attack.type(), "attack_trigger instance reports its singleton type");

        Modifier mining = parse("mining_trigger");
        assertInstanceOf(MiningTriggerType.Instance.class, mining, "mining_trigger routes to MiningTriggerType");
        assertSame(MiningTriggerType.INSTANCE, mining.type(), "mining_trigger instance reports its singleton type");

        Modifier rightClick = parse("right_click");
        assertInstanceOf(RightClickType.Instance.class, rightClick, "right_click routes to RightClickType");
        assertSame(RightClickType.INSTANCE, rightClick.type(), "right_click instance reports its singleton type");

        Modifier onBuild = parse("on_build");
        assertInstanceOf(OnBuildType.Instance.class, onBuild, "on_build routes to OnBuildType");
        assertSame(OnBuildType.INSTANCE, onBuild.type(), "on_build instance reports its singleton type");
    }

    private static Modifier parse(String typeId) {
        JsonObject json = new JsonObject();
        json.addProperty("type", typeId);
        json.addProperty("id", "sconstruct:test_" + typeId);
        json.addProperty("max_level", 1);
        json.addProperty("slot_cost", 1);
        DataResult<Modifier> decoded = Modifier.DIRECT_CODEC.parse(JsonOps.INSTANCE, json);
        return decoded.result().orElseThrow(() -> new AssertionError("parse failed for type " + typeId + ": " + decoded.error().map(err -> err.message()).orElse("")));
    }
}
