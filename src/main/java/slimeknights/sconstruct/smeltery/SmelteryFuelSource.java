package slimeknights.sconstruct.smeltery;

/**
 * A heat source the smeltery controller can draw fuel from (SMTCON-119). Implemented by the
 * seared tanks: a tank filled with lava or molten metal is a fuel source, and the controller
 * polls every tank bound to its assembled structure each tick, picks the hottest one that can
 * provide fuel, and consumes from it before advancing its melts.
 *
 * <p>Abstracting fuel behind this interface keeps the controller independent of <em>what</em>
 * the fuel actually is — lava (the legacy default) and any molten metal both satisfy the same
 * three-method contract, and a future fuel block need only implement it to slot in.
 */
public interface SmelteryFuelSource {

    /**
     * The temperature in kelvin this source heats the smeltery to while it is the active fuel.
     * Hotter sources unlock higher-tier melts. Returns {@code 0} when the source currently holds
     * nothing that can act as fuel.
     */
    int getTemperature();

    /**
     * Whether this source can supply fuel right now — it holds a fuel fluid and has a positive
     * temperature. The controller skips a source that returns {@code false} when picking the
     * hottest available fuel.
     */
    boolean canProvideFuel();

    /**
     * Consumes up to {@code desiredMb} millibuckets of fuel and returns the amount actually
     * consumed. A source with less fuel than requested consumes (and returns) what it has; a
     * source that cannot provide fuel consumes nothing and returns {@code 0}. A non-positive
     * {@code desiredMb} (zero or negative) is not a valid request — implementations must consume
     * nothing and return {@code 0} for it.
     *
     * @param desiredMb the millibuckets of fuel the controller wants to draw this tick; a
     *                  non-positive value consumes nothing
     * @return the millibuckets actually consumed, always {@code 0} or greater
     */
    int consumeFuel(int desiredMb);

    /**
     * The temperature {@link #consumeFuel} <em>would</em> set if called right now — without
     * actually draining any fuel. The controller uses this to decide whether the next charge is
     * worth burning (SMTCON-224): preview first, commit only if the recipes loaded would benefit
     * from that heat. Returns {@code 0} when the source cannot currently provide fuel, matching
     * {@link #canProvideFuel}'s contract.
     *
     * <p>The default implementation returns {@link #getTemperature} when {@link #canProvideFuel}
     * is true, which is the right answer for every present source whose temperature is
     * independent of how much fuel is left. A source whose burn-rate or temperature would change
     * during a draw should override this for the more precise answer.
     */
    default int previewFuelTemperature() {
        return canProvideFuel() ? getTemperature() : 0;
    }

    /**
     * The millibuckets {@link #consumeFuel} <em>would</em> return if called with the same
     * {@code desiredMb} — without actually draining (SMTCON-217). Callers that need to decide
     * whether to commit to a charge (e.g. the smeltery's per-tick charge model, which would
     * waste a partial-charge mB count if it consumed first and then noticed the shortfall) use
     * this to gate the destructive {@link #consumeFuel} call.
     *
     * <p>The default is intentionally conservative: it returns {@code 0} for every input. A
     * source that does not know how much fuel it actually has must not falsely claim a full
     * charge is available; implementations <em>must</em> override this in lockstep with
     * {@link #consumeFuel} so the preview and the destructive consume agree on capacity
     * (e.g. {@code SearedTankBE.simulateConsume} drains its {@code FluidTank} via the standard
     * {@code FluidAction.SIMULATE} flag).
     *
     * @param desiredMb the millibuckets the caller would pass to {@link #consumeFuel}; a
     *                  non-positive value previews nothing
     * @return the millibuckets the next call to {@link #consumeFuel} would consume, always
     *         {@code 0} or greater
     */
    default int simulateConsume(int desiredMb) {
        return 0;
    }
}
