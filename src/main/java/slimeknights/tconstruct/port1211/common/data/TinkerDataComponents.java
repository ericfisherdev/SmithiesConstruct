package slimeknights.tconstruct.port1211.common.data;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.tconstruct.port1211.common.TinkerRegistries;

/**
 * Registration hub for every tool {@code DataComponentType} the mod ships. Each constant is a
 * {@link DeferredHolder} bound to {@link TinkerRegistries#DATA_COMPONENTS}; pulses fetch the
 * underlying {@code DataComponentType} via {@code TOOL_MATERIALS.get()} when attaching the
 * component to a tool's {@code DataComponentMap}.
 *
 * <p>Concentrates the {@code DATA_COMPONENTS.registerComponentType(...)} calls so every tool
 * component lives in one file with consistent persistence/network-sync wiring — the legacy 1.12
 * port had a per-component capability boilerplate that's now collapsed into this hub.
 *
 * <p>Phase 1 ships all five tool data components here: {@link #TOOL_MATERIALS} (SMTCON-18),
 * {@link #TOOL_MODIFIERS} (SMTCON-19), {@link #TOOL_STATS} (SMTCON-20), and finally
 * {@link #TOOL_PERSISTENT_DATA} + {@link #TOOL_BROKEN} (SMTCON-21).
 */
public final class TinkerDataComponents {

    /**
     * Ordered list of material ResourceLocations for a built tool, one entry per part slot.
     * Persistent (saved with the ItemStack's component map) and network-synchronised (sent to
     * the client whenever the stack syncs).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolMaterials>> TOOL_MATERIALS = TinkerRegistries.DATA_COMPONENTS.registerComponentType("toolmaterials",
            builder -> builder.persistent(ToolMaterials.CODEC).networkSynchronized(ToolMaterials.STREAM_CODEC));

    /**
     * Insertion-ordered map of modifier id → level. Iteration order is fixed at construction
     * time and survives every codec round-trip and {@link ToolModifiers#with} call so tooltip
     * rendering and modifier resolution are deterministic.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolModifiers>> TOOL_MODIFIERS = TinkerRegistries.DATA_COMPONENTS.registerComponentType("toolmodifiers",
            builder -> builder.persistent(ToolModifiers.CODEC).networkSynchronized(ToolModifiers.STREAM_CODEC));

    /**
     * Cached, fully-resolved stat snapshot — durability, attack, mining, harvest level, free
     * modifier slots, ranged-tool numbers. Computed from materials + modifiers and pinned on the
     * ItemStack so per-tick consumers (tooltips, attribute resolution, damage events) don't
     * recompute.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolStats>> TOOL_STATS = TinkerRegistries.DATA_COMPONENTS.registerComponentType("toolstats",
            builder -> builder.persistent(ToolStats.CODEC).networkSynchronized(ToolStats.STREAM_CODEC));

    /**
     * Per-modifier persistent state — each registered modifier owns a {@code CompoundTag} slot
     * keyed by its id. Lets modifiers carry running state (charges, absorbed-damage buffers,
     * tick counters) on the ItemStack without spawning a {@code DataComponentType} per modifier.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolPersistentData>> TOOL_PERSISTENT_DATA = TinkerRegistries.DATA_COMPONENTS.registerComponentType("toolpersistentdata",
            builder -> builder.persistent(ToolPersistentData.CODEC).networkSynchronized(ToolPersistentData.STREAM_CODEC));

    /**
     * Single-boolean broken-state flag. Held separately from {@link #TOOL_STATS} / {@link
     * #TOOL_MATERIALS} so the renderer / tooltip layer can flip on the broken bit (every
     * durability-zero hit) without re-serialising the heavier stat/material components.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolBroken>> TOOL_BROKEN = TinkerRegistries.DATA_COMPONENTS.registerComponentType("toolbroken",
            builder -> builder.persistent(ToolBroken.CODEC).networkSynchronized(ToolBroken.STREAM_CODEC));

    private TinkerDataComponents() {
    }

    /**
     * Forces {@link DeferredHolder} fields to be class-load resolved, ensuring the static
     * initialiser of this class runs (and therefore the underlying
     * {@link TinkerRegistries#DATA_COMPONENTS} sees the registration) even when no other code
     * has yet referenced a constant here.
     *
     * <p>Called explicitly from {@code TConstruct} during mod construction; the no-op return
     * value just keeps the call site readable.
     */
    public static void init() {
        // Reference a field so the static initialiser is forced to run. Returning the holder
        // here would tempt callers to use the return value; making it a void no-op keeps the
        // intent obvious.
        TOOL_MATERIALS.getId();
    }
}
