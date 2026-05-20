package slimeknights.sconstruct.smeltery.block.entity.module;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import slimeknights.sconstruct.smeltery.SmelteryFuelSource;

/**
 * Burner for a heated multiblock or single-block furnace (SMTCON-222) — owns the fuel-draw
 * policy that decides how much fuel to consume per server tick and at what temperature the
 * resulting heat sustains. Decoupled from the smeltery controller so a future single-block
 * melter or alloy furnace can instantiate one against its own tank-discovery and load-size
 * suppliers without copy-pasting the burn logic.
 *
 * <p>The module is intentionally stateless beyond what the host wires in: it does not own its
 * fuel sources or its active-load metric. The host supplies a {@link Supplier} that returns the
 * hottest available {@link SmelteryFuelSource} (or {@code null}), an {@link IntSupplier} that
 * returns the active-load count (e.g. melt count for a smeltery), and an {@link IntConsumer}
 * that the module calls with the resulting temperature each tick.
 *
 * <p>The default fuel-draw policy mirrors the controller's pre-extraction behaviour: a tick with
 * {@code N} active loads draws {@code FUEL_DRAW_PER_LOAD * N} millibuckets from the hottest
 * source. A zero-consumption draw (empty tank or cold fuel) reports an out-of-fuel result so the
 * host can pause its loads rather than running them cold.
 */
public final class SmelteryFuelModule {

    /**
     * Millibuckets of fuel drawn per active load per server tick — the per-load burn rate the
     * extracted policy ports verbatim from the original controller constant.
     */
    public static final int FUEL_DRAW_PER_LOAD = 10;

    private final Supplier<SmelteryFuelSource> fuelSourceSupplier;
    private final IntSupplier activeLoadSupplier;
    private final IntConsumer temperatureSink;

    /**
     * @param fuelSourceSupplier resolves the hottest fuel source available <em>this tick</em>, or
     *                           {@code null} when no source can provide fuel. Recomputed on every
     *                           {@link #tickBurn} call so a tank that runs dry or a multiblock
     *                           that loses a tank does not keep heating the burner.
     * @param activeLoadSupplier returns the active-load count (e.g. number of in-flight melts).
     *                           {@code 0} consumes nothing — the burner does not heat an idle
     *                           furnace.
     * @param temperatureSink    receives the temperature the burner heats to this tick:
     *                           positive while fuel is available, {@code 0} when paused.
     */
    public SmelteryFuelModule(Supplier<SmelteryFuelSource> fuelSourceSupplier, IntSupplier activeLoadSupplier, IntConsumer temperatureSink) {
        this.fuelSourceSupplier = Objects.requireNonNull(fuelSourceSupplier, "fuelSourceSupplier");
        this.activeLoadSupplier = Objects.requireNonNull(activeLoadSupplier, "activeLoadSupplier");
        this.temperatureSink = Objects.requireNonNull(temperatureSink, "temperatureSink");
    }

    /**
     * Runs one tick of burner work: resolves the hottest fuel source, attempts to draw enough
     * fuel for the current active load, and reports the resulting temperature to
     * {@link #temperatureSink}. Returns {@code true} when fuel was drawn (the host should advance
     * its active loads) and {@code false} when no fuel was available (the host should pause its
     * active loads rather than running them cold).
     */
    public boolean tickBurn() {
        return tickBurnGated(temperature -> true);
    }

    /**
     * Like {@link #tickBurn()} but the burn is gated by {@code temperaturePredicate}, which sees
     * the temperature the next charge <em>would</em> sustain (via
     * {@link SmelteryFuelSource#previewFuelTemperature()}). When the predicate rejects the
     * previewed temperature, no fuel is consumed and the burner reports zero heat — so a host
     * with active loads that require less heat than this fuel provides can refuse to burn an
     * expensive source on a cheap recipe (SMTCON-224).
     *
     * <p>The pre-existing {@link #tickBurn()} entry point passes a predicate that accepts every
     * positive temperature, preserving the burn-on-any-fuel behaviour the controller has today
     * — recipe-temperature gating is opt-in for callers that pass a tighter predicate.
     */
    public boolean tickBurnGated(IntPredicate temperaturePredicate) {
        Objects.requireNonNull(temperaturePredicate, "temperaturePredicate");
        SmelteryFuelSource hottest = fuelSourceSupplier.get();
        if (hottest == null) {
            temperatureSink.accept(0);
            return false;
        }
        int loads = Math.max(0, activeLoadSupplier.getAsInt());
        if (loads == 0) {
            // Idle furnace — the host normally short-circuits before getting here, but a defensive
            // zero-load tick consumes nothing and reports no heat rather than draining a tank for
            // no work.
            temperatureSink.accept(0);
            return false;
        }
        int previewed = hottest.previewFuelTemperature();
        if (previewed <= 0 || !temperaturePredicate.test(previewed)) {
            // Either the source cannot provide fuel right now, or the caller decided the previewed
            // temperature is not worth a charge. Either way, do not consume and report no heat.
            temperatureSink.accept(0);
            return false;
        }
        int consumed = hottest.consumeFuel(FUEL_DRAW_PER_LOAD * loads);
        if (consumed <= 0) {
            // Preview said yes but the consume returned zero — the source's state changed between
            // the two reads (another caller drained it). Treat as out-of-fuel for this tick.
            temperatureSink.accept(0);
            return false;
        }
        temperatureSink.accept(hottest.getTemperature());
        return true;
    }

    /**
     * Reads the temperature the burner would heat to on its next charge — without drawing any
     * fuel. {@code 0} when no source can currently provide fuel. Callers can use this to power a
     * UI panel ("you'll get N K next charge") or to feed a recipe-temperature gate to
     * {@link #tickBurnGated} without re-resolving the source themselves.
     */
    public int previewTemperature() {
        SmelteryFuelSource hottest = fuelSourceSupplier.get();
        return hottest == null ? 0 : hottest.previewFuelTemperature();
    }
}
