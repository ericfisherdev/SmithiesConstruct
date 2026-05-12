package slimeknights.tconstruct.port1211.common.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import net.neoforged.neoforge.common.ModConfigSpec;

import org.junit.jupiter.api.Test;

import slimeknights.tconstruct.port1211.common.pulse.PulseGate;

/**
 * Pinned-behaviour tests for the per-pulse config wiring.
 *
 * <p>Exercises only what the spec exposes without loading a real {@link
 * net.neoforged.fml.config.ModConfig}: the roster of declared pulses, their declared defaults,
 * their TOML paths, and the {@link Config#pulseGate(java.util.function.Function)} branching logic.
 * The actual TOML round-trip and {@code BooleanValue#get()} resolution are verified in-game by
 * the gameTestServer / runClient acceptance criteria on the ticket — they require a live
 * {@code ModContainer} which unit tests cannot construct.
 */
class ConfigTest {

    @Test
    void specIsBuilt() {
        assertNotNull(Config.SPEC, "Config.SPEC should be built by the static initialiser");
    }

    @Test
    void pulseFlagsHaveExpectedRoster() {
        assertIterableEquals(List.of("shared", "world", "tools", "smeltery", "gadgets", "debug"), Config.PULSE_FLAGS.keySet(),
                "Insertion order is preserved so the generated TOML reads as a stable roster");
    }

    @Test
    void pulseFlagDefaultsMatchLegacyRoster() {
        assertAll(() -> assertTrue(defaultOf("shared"), "shared pulse should be on by default"), () -> assertTrue(defaultOf("world"), "world pulse should be on by default"),
                () -> assertTrue(defaultOf("tools"), "tools pulse should be on by default"), () -> assertTrue(defaultOf("smeltery"), "smeltery pulse should be on by default"),
                () -> assertTrue(defaultOf("gadgets"), "gadgets pulse should be on by default"), () -> assertFalse(defaultOf("debug"), "debug pulse should be off by default"));
    }

    @Test
    void pulseFlagPathsAreNamespacedUnderPulsesCategory() {
        for (String id : Config.PULSE_FLAGS.keySet()) {
            assertEquals(List.of("pulses", id), Config.PULSE_FLAGS.get(id).getPath(), "Each flag should live at pulses.<id> in the generated TOML");
        }
    }

    @Test
    void pulseFlagsMapIsImmutable() {
        // PULSE_FLAGS is exposed as an api surface; callers must not be able to drop a pulse at
        // runtime by mutating the map. Defensive-immutability check guards that contract.
        assertThrows(UnsupportedOperationException.class, () -> Config.PULSE_FLAGS.remove("debug"));
    }

    @Test
    void testablePulseGateUsesResolverValueWhenPresent() {
        PulseGate gate = Config.pulseGate(id -> "tools".equals(id) ? Optional.of(false) : Optional.empty());
        // Resolver says false → gate disables the pulse even though the declared default is true.
        assertFalse(gate.isEnabled("tools", true), "Resolver-supplied value should override the declared default");
    }

    @Test
    void testablePulseGateFallsBackToDeclaredDefaultWhenResolverEmpty() {
        PulseGate gate = Config.pulseGate(id -> Optional.empty());
        assertAll(() -> assertTrue(gate.isEnabled("unknown", true), "Empty resolver → fall back to declared-true"),
                () -> assertFalse(gate.isEnabled("unknown", false), "Empty resolver → fall back to declared-false"));
    }

    private static boolean defaultOf(String pulseId) {
        ModConfigSpec.BooleanValue value = Config.PULSE_FLAGS.get(pulseId);
        assertNotNull(value, "Pulse '" + pulseId + "' should be registered in the spec");
        return value.getDefault();
    }
}
