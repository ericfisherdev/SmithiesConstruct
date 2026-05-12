package slimeknights.tconstruct.port1211.common.pulse;

/**
 * Decides whether a given {@link Pulse} should be loaded.
 *
 * <p>The default production wiring (SMTCON-16) backs this with the project's config: the gate
 * reads the {@code pulses.<id>} flag and falls back to the pulse's declared default. Tests and
 * one-off scripts can pass in any lambda that satisfies the contract — typically
 * {@code (id, def) -> def} for "enable everything" or a {@code Set::contains} for a fixed
 * allowlist.
 *
 * <p>Defined as a separate interface, rather than as a {@code Predicate<String>}, so the gate
 * can see the pulse's declared default and surface it in error messages if needed.
 */
@FunctionalInterface
public interface PulseGate {

    /**
     * @param id              the pulse's {@link Pulse#id()} value.
     * @param defaultEnabled  whether the pulse declares itself enabled by default.
     * @return {@code true} if the loader should register and subscribe this pulse.
     */
    boolean isEnabled(String id, boolean defaultEnabled);

    /** Returns a gate that always honours the pulse's own default. */
    static PulseGate allowAllDefaults() {
        return (id, defaultEnabled) -> defaultEnabled;
    }
}
