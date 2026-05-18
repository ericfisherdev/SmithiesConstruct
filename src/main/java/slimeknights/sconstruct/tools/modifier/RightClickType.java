package slimeknights.sconstruct.tools.modifier;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Modifier dispatch shape for right-click interaction effects (necrotic siphon, soulbound
 * bind, fortify recipe). The right-click context is delivered through SMTCON-83's hooks
 * dispatcher; this type only pins the dispatch shape.
 */
public final class RightClickType implements ModifierType {

    public static final RightClickType INSTANCE = new RightClickType();

    public record Instance(ResourceLocation id, int maxLevel, int slotCost) implements Modifier {

        /** Same validation contract as {@link SimpleStatBoostType.Instance}. */
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

    private RightClickType() {
    }

    @Override
    public String id() {
        return "right_click";
    }

    @Override
    public MapCodec<Instance> instanceCodec() {
        return MAP_CODEC;
    }
}
