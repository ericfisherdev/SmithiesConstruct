package slimeknights.sconstruct.port1211.tools.modifier;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Pure-stat modifier type — the dispatch shape that holds no per-instance behaviour beyond a
 * cached level-scaled stat contribution. Sharpness (+0.5 attack damage / level) and Redstone
 * (+0.08 mining speed / level) are the canonical examples; the actual stat contribution lives
 * in {@link slimeknights.sconstruct.port1211.tools.StatsBuilder} keyed on the modifier id.
 * Instances here are the JSON registry entries that pin the id, cap, and slot cost.
 */
public final class SimpleStatBoostType implements ModifierType {

    /** Singleton instance — the dispatch codec routes {@code "simple_stat_boost"} JSON entries
     *  through this type. */
    public static final SimpleStatBoostType INSTANCE = new SimpleStatBoostType();

    /** Concrete record carrying the four JSON-defined fields. Hook methods inherit the no-op
     *  defaults from {@link Modifier} — stat math runs through {@code StatsBuilder} keyed on
     *  the id, so this type has no runtime hook side effects to register. */
    public record Instance(ResourceLocation id, int maxLevel, int slotCost) implements Modifier {

        /** Compact constructor: a non-positive maxLevel makes the modifier un-applyable, and a
         *  negative slot cost would let a tool gain free modifiers — neither is a meaningful
         *  configuration. Fail at the JSON-load site rather than letting the bad value reach
         *  SMTCON-83's hooks dispatcher. */
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

    private SimpleStatBoostType() {
    }

    @Override
    public String id() {
        return "simple_stat_boost";
    }

    @Override
    public MapCodec<Instance> instanceCodec() {
        return MAP_CODEC;
    }
}
