package slimeknights.sconstruct.smeltery.block.entity.module;

import java.util.Objects;
import java.util.function.IntConsumer;
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
        int consumed = hottest.consumeFuel(FUEL_DRAW_PER_LOAD * loads);
        if (consumed <= 0) {
            temperatureSink.accept(0);
            return false;
        }
        temperatureSink.accept(hottest.getTemperature());
        return true;
    }
}
