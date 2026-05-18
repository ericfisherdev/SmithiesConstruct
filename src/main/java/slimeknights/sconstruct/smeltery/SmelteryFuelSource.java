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
}
