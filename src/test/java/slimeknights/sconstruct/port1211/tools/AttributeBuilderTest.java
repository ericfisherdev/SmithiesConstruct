package slimeknights.sconstruct.port1211.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.common.data.ToolStats;

/**
 * Pinned-behaviour tests for {@link AttributeBuilder}. Verifies the pure function emits one
 * attack-damage and one attack-speed modifier with the documented amount formula, both bound
 * to {@link EquipmentSlotGroup#MAINHAND} (SMTCON-76).
 */
class AttributeBuilderTest {

    @Test
    void buildEmitsExactlyTwoModifiersForAnyStats() {
        ItemAttributeModifiers modifiers = AttributeBuilder.build(ToolStats.zero());
        assertEquals(2, modifiers.modifiers().size(), "AttributeBuilder must emit one attack-damage and one attack-speed entry");
    }

    @Test
    void attackDamageModifierMatchesStatsValueAndMainhandSlot() {
        ToolStats stats = new ToolStats(100, 6.5F, 0.4F, 1.0F, 2, 3, 0F, 0F, 0F);
        ItemAttributeModifiers modifiers = AttributeBuilder.build(stats);
        ItemAttributeModifiers.Entry damageEntry = findEntry(modifiers, AttributeBuilder.ATTACK_DAMAGE_ID);
        assertNotNull(damageEntry, "must include an entry whose modifier id is ATTACK_DAMAGE_ID");
        assertEquals(EquipmentSlotGroup.MAINHAND, damageEntry.slot(), "attack-damage modifier must target the main hand only");
        assertTrue(damageEntry.attribute().is(Attributes.ATTACK_DAMAGE.unwrapKey().orElseThrow()), "must target the vanilla ATTACK_DAMAGE attribute");
        assertEquals(6.5D, damageEntry.modifier().amount(), 0.0001D, "attack-damage amount must equal ToolStats.attackDamage()");
        assertEquals(AttributeModifier.Operation.ADD_VALUE, damageEntry.modifier().operation());
    }

    @Test
    void attackSpeedModifierFormulaAppliesVanillaBaseline() {
        // Verifies the addition formula `VANILLA_ATTACK_SPEED_BASELINE + stats.attackSpeed()`
        // with a non-zero attackSpeed input (0.4F here → expected -2.0 amount). The pure-zero
        // case is covered separately by zeroStatsEmitVanillaBaselineSpeedAndZeroDamageDelta
        // below.
        ToolStats stats = new ToolStats(100, 0F, 0.4F, 0F, 0, 0, 0F, 0F, 0F);
        ItemAttributeModifiers modifiers = AttributeBuilder.build(stats);
        ItemAttributeModifiers.Entry speedEntry = findEntry(modifiers, AttributeBuilder.ATTACK_SPEED_ID);
        assertNotNull(speedEntry);
        assertEquals(EquipmentSlotGroup.MAINHAND, speedEntry.slot());
        assertTrue(speedEntry.attribute().is(Attributes.ATTACK_SPEED.unwrapKey().orElseThrow()), "must target the vanilla ATTACK_SPEED attribute");
        assertEquals(AttributeBuilder.VANILLA_ATTACK_SPEED_BASELINE + 0.4D, speedEntry.modifier().amount(), 0.0001D, "attack-speed amount must equal -2.4 + ToolStats.attackSpeed()");
        assertEquals(AttributeModifier.Operation.ADD_VALUE, speedEntry.modifier().operation(), "speed delta must apply additively — Operation.ADD_VALUE matches vanilla DiggerItem");
    }

    @Test
    void zeroStatsEmitVanillaBaselineSpeedAndZeroDamageDelta() {
        ItemAttributeModifiers modifiers = AttributeBuilder.build(ToolStats.zero());
        ItemAttributeModifiers.Entry damageEntry = findEntry(modifiers, AttributeBuilder.ATTACK_DAMAGE_ID);
        ItemAttributeModifiers.Entry speedEntry = findEntry(modifiers, AttributeBuilder.ATTACK_SPEED_ID);
        assertNotNull(damageEntry);
        assertNotNull(speedEntry);
        assertEquals(0.0D, damageEntry.modifier().amount(), 0.0001D);
        assertEquals(AttributeBuilder.VANILLA_ATTACK_SPEED_BASELINE, speedEntry.modifier().amount(), 0.0001D);
    }

    @Test
    void buildRejectsNullStatsRatherThanLeakingNpe() {
        // Pin the null-guard contract: callers that hand in a null ToolStats must get a clear
        // NullPointerException whose message names the parameter, not a surprise NPE deeper
        // inside the builder. The message anchor lets log readers locate the caller fast.
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> AttributeBuilder.build(null));
        assertTrue(thrown.getMessage() != null && thrown.getMessage().contains("stats"), "NPE message must name the 'stats' parameter; got: " + thrown.getMessage());
    }

    @Test
    void modifierIdsLiveUnderTheLegacyTconstructNamespace() {
        // Cross-mod compat anchor: any addon that read tools' attribute modifiers by id under
        // 1.12's tconstruct:tool_* paths keeps resolving without an addon-side rename. Pin both
        // namespace and path so a future rename in this class fails this test loudly.
        assertEquals("tconstruct", AttributeBuilder.ATTACK_DAMAGE_ID.getNamespace());
        assertEquals("tool_attack_damage", AttributeBuilder.ATTACK_DAMAGE_ID.getPath());
        assertEquals("tconstruct", AttributeBuilder.ATTACK_SPEED_ID.getNamespace());
        assertEquals("tool_attack_speed", AttributeBuilder.ATTACK_SPEED_ID.getPath());
    }

    private static ItemAttributeModifiers.Entry findEntry(ItemAttributeModifiers modifiers, net.minecraft.resources.ResourceLocation id) {
        List<ItemAttributeModifiers.Entry> entries = modifiers.modifiers();
        for (ItemAttributeModifiers.Entry entry : entries) {
            if (entry.modifier().id().equals(id)) {
                return entry;
            }
        }
        return null;
    }
}
