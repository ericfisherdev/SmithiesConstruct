package slimeknights.tconstruct.port1211.common.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import net.neoforged.neoforge.common.ModConfigSpec;

import slimeknights.tconstruct.port1211.common.pulse.Pulse;
import slimeknights.tconstruct.port1211.common.pulse.PulseGate;
import slimeknights.tconstruct.port1211.common.pulse.PulseLoader;

/**
 * Production {@link ModConfigSpec} for the port-1.21.1 build. Holds the boolean flags that gate
 * each {@link Pulse} subsystem; loaded by NeoForge before {@code FMLCommonSetupEvent} fires, so
 * {@link PulseLoader#boot} can safely read the resolved values during mod construction.
 *
 * <p>The TOML written to {@code <gameDir>/config/tconstruct-common.toml} on first launch groups
 * every pulse flag under the {@code [pulses]} category. Toggling a value there takes effect on
 * the next game launch — the loader reads each flag exactly once during boot.
 *
 * <p>Defaults match the legacy upstream pulse roster from Tinkers' Construct 1.12: every
 * production pulse on, the developer-only {@code debug} pulse off. Add a new pulse by appending
 * its declaration to {@link #PULSE_DECLARATIONS}; the static initialiser builds the spec entry,
 * registers the value, and the production gate picks it up automatically.
 */
public final class Config {

    /** Spec category that holds every pulse-gate boolean. */
    private static final String PULSE_CATEGORY = "pulses";

    /**
     * Ordered declaration of every pulse the loader knows about and its default-enabled state.
     * Order is preserved in the generated TOML so the file reads top-down as a roster.
     */
    private static final List<PulseDeclaration> PULSE_DECLARATIONS = List.of(new PulseDeclaration("shared", true, "Cross-pulse blocks, items, and registries used by every other subsystem."),
            new PulseDeclaration("world", true, "Slime islands, slime entities, world-gen features."),
            new PulseDeclaration("tools", true, "Tool stations, harvest/melee/ranged tools, modifiers, materials."),
            new PulseDeclaration("smeltery", true, "Multiblock smeltery, tanks, casting, alloy logic."),
            new PulseDeclaration("gadgets", true, "Slime slings, slime boots, drying racks, piggybacking."), new PulseDeclaration("debug", false, "Developer-only debug tooling. Off by default."));

    /** The compiled spec; pass this to {@code ModContainer#registerConfig}. */
    public static final ModConfigSpec SPEC;

    /**
     * Pulse id → BooleanValue, in declaration order. Immutable so callers cannot accidentally
     * mutate the gate map at runtime. Values are bound to {@link #SPEC} and resolve to the
     * loaded TOML value at runtime, or the declared default before configs load.
     */
    public static final Map<String, ModConfigSpec.BooleanValue> PULSE_FLAGS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Per-subsystem enable flags. Each pulse gates a self-contained slice of the mod;", "disabling one skips its registry contributions, listeners, and setup hooks.")
                .push(PULSE_CATEGORY);

        Map<String, ModConfigSpec.BooleanValue> flags = new LinkedHashMap<>();
        for (PulseDeclaration decl : PULSE_DECLARATIONS) {
            flags.put(decl.id(), builder.comment(decl.description()).define(decl.id(), decl.defaultEnabled()));
        }

        builder.pop();
        SPEC = builder.build();
        PULSE_FLAGS = Collections.unmodifiableMap(flags);
    }

    private Config() {
    }

    /**
     * Production {@link PulseGate} that reads each pulse's enabled flag from {@link #PULSE_FLAGS}.
     *
     * <p>Falls back to the pulse's declared {@code defaultEnabled()} if a pulse is queried that
     * was never registered in the spec — defensive plumbing for pulses introduced after the spec
     * is built (addon mods, dynamic loaders) so they don't blow up the gate just because they're
     * not in the config file yet.
     *
     * <p>Callers should invoke this <em>after</em> NeoForge has loaded the COMMON config (i.e.
     * inside or after {@code FMLCommonSetupEvent}); calling earlier returns the declared defaults
     * but the underlying {@code BooleanValue#get()} may throw if the spec has not been bound.
     */
    public static PulseGate pulseGate() {
        return pulseGate(Config::resolveFlag);
    }

    /**
     * Test-friendly factory: takes an arbitrary {@code id -> Optional<Boolean>} resolver in place
     * of the production {@link ModConfigSpec.BooleanValue} lookup. Returns a gate that uses the
     * resolved value when present, else falls back to the pulse's declared {@code defaultEnabled}.
     *
     * <p>Lets unit tests exercise the gate's branching logic without booting a real
     * {@link net.neoforged.fml.config.ModConfig}, which would require a {@code ModContainer}.
     */
    public static PulseGate pulseGate(Function<String, Optional<Boolean>> resolver) {
        return (id, defaultEnabled) -> resolver.apply(id).orElse(defaultEnabled);
    }

    private static Optional<Boolean> resolveFlag(String id) {
        ModConfigSpec.BooleanValue value = PULSE_FLAGS.get(id);
        return value == null ? Optional.empty() : Optional.of(value.get());
    }

    /**
     * Single pulse-flag declaration. Defined as a Java record so the constant table at the top of
     * this class reads as a roster rather than a soup of parallel arrays. The {@code description}
     * is the comment NeoForge writes above the boolean in the generated TOML.
     */
    private record PulseDeclaration(String id, boolean defaultEnabled, String description) {
    }
}
