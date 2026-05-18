package slimeknights.sconstruct.tools.modifier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

/**
 * Closed taxonomy of modifier dispatch shapes — the code-side classification a
 * {@link Modifier} declares to drive its serialised form and runtime hook routing. Sealed
 * against five permits matching the legacy 1.12 trait families:
 *
 * <ul>
 *   <li>{@link SimpleStatBoostType} — pure-stat additions (sharpness, redstone) that
 *       {@link slimeknights.sconstruct.tools.StatsBuilder} folds into the cached
 *       {@code ToolStats} snapshot.</li>
 *   <li>{@link AttackTriggerType} — modifiers that fire on melee swing (knockback, fiery,
 *       beheading).</li>
 *   <li>{@link MiningTriggerType} — modifiers that fire on mine tick (auto-smelt, silk-touch,
 *       luck).</li>
 *   <li>{@link RightClickType} — modifiers driven by player right-click interaction
 *       (necrotic siphon, soulbound bind, fortify).</li>
 *   <li>{@link OnBuildType} — modifiers that stamp persistent state at build time (mossy
 *       auto-repair, soulbound owner, diamond-cap durability boost).</li>
 * </ul>
 *
 * <p>Each permit defines its own {@link MapCodec} via {@link #instanceCodec()} — the dispatch
 * codec in {@link Modifier#DIRECT_CODEC} reads the {@code type} discriminator off the JSON
 * object and routes the rest of the parse to that codec. Adding a new dispatch shape means
 * adding a permit here (compile-time sealed-switch enforcement covers the exhaustive case in
 * SMTCON-83's hooks dispatcher).
 */
public sealed interface ModifierType permits SimpleStatBoostType, AttackTriggerType, MiningTriggerType, RightClickType, OnBuildType {

    /** Codec routing the JSON dispatch field through the lowercase {@code id}. Decode goes via
     *  {@code flatXmap} so an unknown id surfaces as a {@link DataResult#error} rather than
     *  throwing — datapacks that ship a type the mod doesn't know fail gracefully. */
    Codec<ModifierType> CODEC = Codec.STRING.flatXmap(id -> {
        ModifierType type = All.BY_ID.get(id);
        if (type != null) {
            return DataResult.success(type);
        }
        return DataResult.error(() -> "Unknown ModifierType: " + id);
    }, type -> DataResult.success(type.id()));

    /** Stable lowercase identifier used as the JSON {@code type} discriminator value. */
    String id();

    /** Map codec parsing the per-instance fields once the dispatcher has selected this type.
     *  Each permit's codec consumes the rest of the JSON object minus the {@code type} field —
     *  Mojang's {@code dispatch} infrastructure handles that exclusion automatically. */
    MapCodec<? extends Modifier> instanceCodec();

    /**
     * Constants holder for the closed set of singleton {@link ModifierType} instances. Lives
     * in a nested class — not on the sealed interface directly — because PMD's
     * {@code ConstantsInInterface} rule (SMTCON-68 CI feedback) discourages public-static
     * fields on interfaces. The {@link #VALUES} array is the iteration source used by
     * {@link #CODEC} for id → type lookup.
     */
    final class All {

        /** Every singleton {@link ModifierType} instance. {@link List#of} is immutable so the
         *  dispatch table cannot be reassigned at runtime — protects {@link #CODEC}'s lookup
         *  against accidental or hostile {@code VALUES[i] = ...} writes that would silently
         *  poison every modifier decode. Order is the canonical type order (SimpleStatBoost
         *  first because it's the most common dispatch shape). */
        public static final List<ModifierType> VALUES = List.of(SimpleStatBoostType.INSTANCE, AttackTriggerType.INSTANCE, MiningTriggerType.INSTANCE, RightClickType.INSTANCE, OnBuildType.INSTANCE);

        /** Pre-built id → type lookup so {@link #CODEC}'s decode step is constant-time rather
         *  than walking the list. Built once at class load — adding a new permit goes through
         *  {@link #VALUES} and is automatically reflected here. */
        public static final Map<String, ModifierType> BY_ID = VALUES.stream().collect(Collectors.toUnmodifiableMap(ModifierType::id, Function.identity()));

        private All() {
        }
    }
}
