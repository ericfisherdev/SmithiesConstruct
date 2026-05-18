package slimeknights.sconstruct.tools.modifier;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Modifier dispatch shape for build-time stamping effects (mossy auto-repair seed,
 * soulbound-owner stamp, diamond-cap durability boost). The {@link Modifier#onBuild} hook is
 * invoked once when the modifier is applied; concrete subclasses stamp persistent state into
 * {@link slimeknights.sconstruct.common.data.ToolPersistentData}.
 */
public final class OnBuildType implements ModifierType {

    public static final OnBuildType INSTANCE = new OnBuildType();

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

    private OnBuildType() {
    }

    @Override
    public String id() {
        return "on_build";
    }

    @Override
    public MapCodec<Instance> instanceCodec() {
        return MAP_CODEC;
    }
}
