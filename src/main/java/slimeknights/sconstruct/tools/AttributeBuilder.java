package slimeknights.sconstruct.tools;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import slimeknights.sconstruct.common.data.ToolStats;

/**
 * Pure function that derives the {@link ItemAttributeModifiers} component for a built tool
 * from its cached {@link ToolStats}. The vanilla {@code DataComponents.ATTRIBUTE_MODIFIERS}
 * slot then drives the tooltip's "When in main hand" lines and the damage / speed numbers fed
 * to {@code Player#getAttribute} during a swing, so the tool stack carries the same per-stack
 * attribute view a {@code DiggerItem}'s default modifiers would (SMTCON-76).
 *
 * <p>Modifier ids stay in the legacy {@code tconstruct} namespace per the ticket so any addon
 * that previously read the tools' attribute modifiers by id keeps resolving. The
 * {@code -2.4} attack-speed baseline matches vanilla {@code DiggerItem.createAttributes} —
 * vanilla applies the {@link AttributeModifier.Operation#ADD_VALUE} delta to the player's
 * default {@link Attributes#ATTACK_SPEED} (4.0), so a sword shipping {@code -2.4} produces the
 * familiar 1.6 attacks/sec cooldown.
 */
public final class AttributeBuilder {

    /** Stable id for the attack-damage modifier — preserved from legacy 1.12 for addon compat. */
    public static final ResourceLocation ATTACK_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "tool_attack_damage");

    /** Stable id for the attack-speed modifier — preserved from legacy 1.12 for addon compat. */
    public static final ResourceLocation ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "tool_attack_speed");

    /**
     * Vanilla {@code DiggerItem} attack-speed baseline. Sums with the stat block's
     * {@link ToolStats#attackSpeed} so a tool with a stats-side {@code +0.4} effectively ships
     * a {@code -2.0} modifier (player default {@code 4.0} → {@code 2.0} attacks/sec).
     */
    public static final double VANILLA_ATTACK_SPEED_BASELINE = -2.4D;

    private AttributeBuilder() {
    }

    /**
     * Build the {@link ItemAttributeModifiers} that match the supplied {@link ToolStats}.
     * Pure — no side effects, no caching; callers store the result on the stack themselves
     * (typically inside {@code StatsBuilder.rebuildStats} once SMTCON-77 lands).
     *
     * <p>Both modifiers attach to {@link EquipmentSlotGroup#MAINHAND} only — off-hand and armor
     * slots see no attack contribution. The ranged tools (bows, crossbow) carry their per-shot
     * damage through {@link ToolStats#projectileBonus}, not through this attribute view; that
     * value feeds {@code AbstractArrow#setBaseDamage} in the bow's release hook rather than the
     * player's swing attribute.
     *
     * @param stats the cached tool stats — must be non-null; pass {@link ToolStats#zero} for
     *     the "stats not yet computed" sentinel.
     * @throws NullPointerException if {@code stats} is null.
     */
    public static ItemAttributeModifiers build(ToolStats stats) {
        Objects.requireNonNull(stats, "stats");
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_ID, stats.attackDamage(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_ID, VANILLA_ATTACK_SPEED_BASELINE + stats.attackSpeed(), AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }
}
