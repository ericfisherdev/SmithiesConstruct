package slimeknights.sconstruct.port1211.tools.modifier;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Modifier dispatch shape for swing-time side effects (knockback, fiery, beheading). The
 * concrete behaviour table for each id is owned by SMTCON-84+'s registration step — this type
 * just pins the dispatch shape and the three JSON fields every modifier carries.
 */
public final class AttackTriggerType implements ModifierType {

    public static final AttackTriggerType INSTANCE = new AttackTriggerType();

    public record Instance(ResourceLocation id, int maxLevel, int slotCost) implements Modifier {

        /** Same validation contract as {@link SimpleStatBoostType.Instance}: maxLevel must be
         *  positive (else un-applyable) and slotCost must be non-negative (else free
         *  modifier slots). */
        public Instance {
            java.util.Objects.requireNonNull(id, "id");
            if (maxLevel <= 0) {
                throw new IllegalArgumentException("modifier '" + id + "' maxLevel must be positive (got " + maxLevel + ")");
            }
            if (slotCost < 0) {
                throw new IllegalArgumentException("modifier '" + id + "' slotCost must be non-negative (got " + slotCost + ")");
            }
        }

        @Override
        public ModifierType type() {
            return INSTANCE;
        }
    }

    private static final MapCodec<Instance> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ResourceLocation.CODEC.fieldOf("id").forGetter(Instance::id),
            Modifier.MAX_LEVEL_CODEC.fieldOf("max_level").forGetter(Instance::maxLevel), Modifier.SLOT_COST_CODEC.fieldOf("slot_cost").forGetter(Instance::slotCost)).apply(instance, Instance::new));

    private AttackTriggerType() {
    }

    @Override
    public String id() {
        return "attack_trigger";
    }

    @Override
    public MapCodec<Instance> instanceCodec() {
        return MAP_CODEC;
    }
}
