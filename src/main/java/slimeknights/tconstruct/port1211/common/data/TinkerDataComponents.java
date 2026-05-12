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
 * <p>Phase 1 ships {@link #TOOL_MATERIALS} (SMTCON-18) and {@link #TOOL_MODIFIERS}
 * (SMTCON-19); SMTCON-20 through SMTCON-21 will append the remaining three components
 * ({@code ToolStats}, {@code ToolPersistentData}, {@code ToolBroken}) alongside them.
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
