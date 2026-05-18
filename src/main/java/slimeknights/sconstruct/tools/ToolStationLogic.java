package slimeknights.sconstruct.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import slimeknights.sconstruct.common.data.TinkerDataComponents;
import slimeknights.sconstruct.tools.item.MaterialItem;
import slimeknights.sconstruct.tools.item.ToolCore;

/**
 * Pure-function helper that drives the Tool Station / Tool Forge build path (SMTCON-93). Given
 * a positional list of {@link ItemStack} inputs and a stream of {@link ToolDefinition}
 * candidates, returns the freshly-built tool stack for the first candidate whose part roster
 * matches the inputs slot-for-slot — or {@link Optional#empty()} when no candidate fits.
 *
 * <p>Matching rule, evaluated against each candidate in stream order:
 *
 * <ol>
 *   <li>Trim trailing empty input slots so the legacy 6-slot UI can offer "fewer parts than
 *       the widest tool" recipes (a 3-part pickaxe lives in slots 0-2 with 3-5 left empty).
 *       Then the trimmed length must equal {@code def.getPartCount()}.</li>
 *   <li>Each input slot must hold a {@link MaterialItem} whose static
 *       {@link MaterialItem#partType()} equals {@code def.getPartSlot(i)} — order matters,
 *       slot {@code N} must match part {@code N}.</li>
 *   <li>Each input stack must carry the {@link TinkerDataComponents#PART_MATERIAL} component —
 *       an un-stamped part item is treated as a "no material yet" stack and rejected. The
 *       {@link MaterialItem#getMaterial} fallback to {@link MaterialItem#DEFAULT_MATERIAL} is
 *       intentionally bypassed here so a build doesn't silently assemble a tool out of the
 *       wood-sentinel default; the station UI should refuse the build instead.</li>
 * </ol>
 *
 * <p>On match: {@link #findMatch} returns the resolved {@link Match} record (pure — no
 * {@link ItemStack} allocation, no registry access). {@link #tryBuild} layers the impure
 * {@code new ItemStack(...)} + {@link ToolHelper#setMaterials} step on top so callers that
 * just want the output stack can pipeline through one call. Unit tests target
 * {@link #findMatch} directly because the JUnit harness can't reach {@code new ItemStack(...)}
 * — the item registry freezes before tests run.
 *
 * <p>The caller is responsible for running the server-side {@link ToolHelper#rebuildStats}
 * pass — this helper doesn't have a {@link net.minecraft.server.MinecraftServer} in scope.
 *
 * <p>The candidate stream is consumed exactly once; the helper short-circuits on the first
 * match without forcing the rest of the stream. Every input {@link ItemStack} is read but
 * never mutated.
 */
public final class ToolStationLogic {

    private ToolStationLogic() {
    }

    /**
     * Try to build a tool from the supplied positional inputs against every candidate. Returns
     * the freshly-built tool stack (materials written, stats <em>not</em> yet rebuilt) for the
     * first candidate whose definition matches, or {@link Optional#empty()} when no candidate
     * fits.
     *
     * @param inputs positional input stacks — slot {@code i} feeds part slot {@code i} of the
     *     candidate definition. Trailing empty slots are trimmed before the length check so a
     *     6-slot UI can hold a 3-part tool in its first three slots.
     * @param candidates tool definitions to consider. Iterated lazily in stream order; the
     *     first match wins.
     * @param toolForDefinition resolver from the matched {@link ToolDefinition} back to its
     *     {@link ToolCore} item — invoked exactly once (on match) so the caller doesn't pay
     *     the cost of resolving an item for definitions that don't fit.
     */
    public static Optional<ItemStack> tryBuild(List<ItemStack> inputs, Stream<ToolDefinition> candidates, Function<ToolDefinition, ToolCore> toolForDefinition) {
        Objects.requireNonNull(toolForDefinition, "toolForDefinition");
        return findMatch(inputs, candidates).map(match -> {
            ItemStack built = new ItemStack(toolForDefinition.apply(match.definition()));
            ToolHelper.setMaterials(built, match.materials());
            return built;
        });
    }

    /**
     * Pure-function match resolution — same matching rule as {@link #tryBuild} but stops at
     * the {@code (definition, materials)} pair without constructing an {@link ItemStack}.
     * Exposed package-private so unit tests can exercise the matching logic without the
     * registry-bound {@code new ItemStack(tool)} call site that the JUnit harness can't reach.
     */
    static Optional<Match> findMatch(List<ItemStack> inputs, Stream<ToolDefinition> candidates) {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(candidates, "candidates");
        // Snapshot the populated (non-empty) prefix length: empty trailing slots are legal
        // padding from the 6-slot UI but trailing non-empty slots past the definition's part
        // count must reject (a 4th input on a 3-part pickaxe is an over-fill, not padding).
        int populated = trimmedLength(inputs);
        if (populated == 0) {
            return Optional.empty();
        }
        return candidates.map(def -> matchOne(inputs, populated, def)).filter(Optional::isPresent).map(Optional::get).findFirst();
    }

    /**
     * Attempt a match against one definition. Returns the definition-and-materials pair on
     * success or {@link Optional#empty()} when the definition doesn't fit the populated input
     * prefix.
     */
    private static Optional<Match> matchOne(List<ItemStack> inputs, int populated, ToolDefinition def) {
        if (def.getPartCount() != populated) {
            return Optional.empty();
        }
        List<ResourceLocation> materials = new ArrayList<>(populated);
        for (int i = 0; i < populated; i++) {
            ItemStack slot = inputs.get(i);
            if (!(slot.getItem() instanceof MaterialItem partItem)) {
                return Optional.empty();
            }
            if (partItem.partType() != def.getPartSlot(i)) {
                return Optional.empty();
            }
            ResourceLocation materialId = slot.get(TinkerDataComponents.PART_MATERIAL.get());
            if (materialId == null) {
                // No material stamped — the part hasn't been bound at the part builder yet.
                // Treat as un-buildable rather than silently falling back to the wood default.
                return Optional.empty();
            }
            materials.add(materialId);
        }
        return Optional.of(new Match(def, List.copyOf(materials)));
    }

    /**
     * Length of the populated prefix of the input list — i.e. the number of slots before the
     * first trailing run of empty stacks. Empty slots in the middle of the prefix are not
     * trimmed (they remain in the count and will fail the {@code MaterialItem} check), so a
     * gap in the middle of the inputs is a build failure rather than a silent compaction.
     */
    static int trimmedLength(List<ItemStack> inputs) {
        int last = inputs.size();
        while (last > 0 && inputs.get(last - 1).isEmpty()) {
            last--;
        }
        return last;
    }

    /**
     * Resolved match record: the {@link ToolDefinition} whose part roster fit the inputs,
     * paired with the positional material id list extracted from the input stacks' {@code
     * PART_MATERIAL} components. Used as the intermediate value between {@link #findMatch}
     * (pure) and {@link #tryBuild} (allocates a new {@link ItemStack}).
     */
    record Match(ToolDefinition definition, List<ResourceLocation> materials) {
        Match {
            Objects.requireNonNull(definition, "definition");
            materials = List.copyOf(materials);
        }
    }
}
