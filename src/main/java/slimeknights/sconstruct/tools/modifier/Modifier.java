package slimeknights.sconstruct.tools.modifier;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import slimeknights.sconstruct.SConstruct;

/**
 * The contract every Smithies' Construct modifier implements. Modifiers are datapack-registry
 * entries — JSON files under {@code data/<modid>/modifier/} — each declaring a {@code type}
 * field that selects a concrete {@link ModifierType} and a per-type parameter block consumed by
 * that type's {@link ModifierType#instanceCodec instance codec}. The split between this
 * interface (data-shape) and {@link ModifierType} (code-side dispatch) mirrors the legacy 1.12
 * model where {@code TraitModifier} subclasses pulled their per-instance config from {@code
 * ModifierNBT} — the JSON layer here replaces the per-instance NBT while keeping the type-class
 * hierarchy code-side.
 *
 * <p>Lifecycle hooks: {@link #onAttack}, {@link #onMine}, {@link #onBuild} fire from the four
 * vanilla event paths a tinker tool participates in (player swing, mining tick, build / repair
 * via {@link slimeknights.sconstruct.tools.ToolHelper}). The {@code level} parameter
 * is the player's accumulated count of this modifier on the stack (read off
 * {@link slimeknights.sconstruct.tools.ToolHelper#getModifierLevel}); a {@code 0}
 * level means the modifier is not applied — the dispatcher (SMTCON-83) is responsible for
 * branching on {@code level > 0} before invoking the hook.
 *
 * <p>Cost model: {@link #maxLevel} caps how many times a modifier can stack on a single tool;
 * {@link #slotCost} is the number of {@code freeModifiers} each application consumes. Hard
 * constants here keep cost / cap immutable per-modifier — the cap-tier upgrade flow in
 * SMTCON-88 will introduce per-stack overrides via {@code ToolPersistentData} rather than
 * mutating these values.
 */
public interface Modifier {

    /** Datapack registry key for {@code sconstruct:modifier}. JSON entries land under
     *  {@code data/<namespace>/modifier/<path>.json}; runtime lookup goes through
     *  {@link ModifierRegistry#get} or {@link net.minecraft.core.HolderLookup.Provider}'s
     *  {@code lookupOrThrow(REGISTRY_KEY)}. */
    ResourceKey<Registry<Modifier>> REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "modifier"));

    /** Polymorphic dispatch codec. Decodes a modifier JSON by reading the {@code type} field,
     *  picking the matching {@link ModifierType} from {@link ModifierType#CODEC}'s string
     *  dispatch, then running that type's {@link ModifierType#instanceCodec} against the rest
     *  of the JSON object. Adding a new modifier shape means adding a permit to
     *  {@link ModifierType} — not editing this codec. */
    Codec<Modifier> DIRECT_CODEC = ModifierType.CODEC.dispatch("type", Modifier::type, ModifierType::instanceCodec);

    /** Field codec that rejects {@code maxLevel <= 0} at parse time, surfacing the bad input as
     *  a {@link DataResult#error} rather than letting the record constructor throw an
     *  unhandled {@link IllegalArgumentException}. Shared across every {@link ModifierType}
     *  permit so a single error message format covers every modifier shape. */
    Codec<Integer> MAX_LEVEL_CODEC = Codec.INT.flatXmap(value -> value > 0 ? DataResult.success(value) : DataResult.error(() -> "max_level must be positive (got " + value + ")"), DataResult::success);

    /** Field codec that rejects {@code slotCost < 0} at parse time — same rationale as
     *  {@link #MAX_LEVEL_CODEC}. */
    Codec<Integer> SLOT_COST_CODEC = Codec.INT.flatXmap(value -> value >= 0 ? DataResult.success(value) : DataResult.error(() -> "slot_cost must be non-negative (got " + value + ")"),
            DataResult::success);

    /** The id this modifier was registered under. Used as the cross-cutting key in
     *  {@link slimeknights.sconstruct.common.data.ToolModifiers} and as the stable
     *  reference any tooltip / JEI category resolves against. */
    ResourceLocation id();

    /** Dispatch discriminator — the {@link ModifierType} this modifier instance belongs to.
     *  Drives the {@link #DIRECT_CODEC} dispatch and gives downstream consumers (the SMTCON-83
     *  hooks dispatcher) the concrete type to branch on. */
    ModifierType type();

    /** Maximum stackable level on a single tool. Cap-tier modifiers (SMTCON-88) can lift this
     *  per-stack, but the base value is the floor every fresh tool reads against. */
    int maxLevel();

    /** {@code freeModifiers} slots each application consumes — read by the tool-station UI to
     *  gate whether a modifier can be applied. */
    int slotCost();

    /**
     * Fires on every successful melee swing the player lands on a target. The dispatcher
     * (SMTCON-83) invokes this hook for every {@link Modifier} whose stack-level is
     * {@code > 0}, in {@link slimeknights.sconstruct.common.data.ToolModifiers}
     * insertion order.
     */
    default void onAttack(ItemStack stack, int level, ToolEvents.OnHitContext context) {
        // Default: no-op. Implementing types override this if they need attack-time side effects.
    }

    /**
     * Fires on every block-mine tick the tool participates in. Same dispatcher contract as
     * {@link #onAttack}.
     */
    default void onMine(ItemStack stack, int level, ToolEvents.OnMineContext context) {
        // Default: no-op. Implementing types override this if they need mining-time side effects.
    }

    /**
     * Fires once when the modifier is applied to a tool (or when the level changes). Lets the
     * implementer stamp per-stack persistent state into
     * {@link slimeknights.sconstruct.common.data.ToolPersistentData} that subsequent
     * hooks read from.
     */
    default void onBuild(ItemStack stack, int level) {
        // Default: no-op. Implementing types override this if they need build-time side effects.
    }

    /** Tooltip text for the supplied level. Default returns a plain-text Component containing
     *  the modifier id; production modifiers override this to return a localised
     *  {@link Component#translatable} chain. */
    default Component description(int level) {
        return Component.literal(id() + " " + level);
    }
}
