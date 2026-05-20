package slimeknights.sconstruct.smeltery.block.entity.module;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import slimeknights.sconstruct.smeltery.SmelteryFuelSource;

/**
 * Burner for a heated multiblock or single-block furnace (SMTCON-222 / SMTCON-217) — owns the
 * fuel-draw policy that decides when to drain a charge of fuel and at what temperature the
 * resulting heat sustains. Decoupled from the smeltery controller so a future single-block
 * melter or alloy furnace can instantiate one against its own tank-discovery without
 * copy-pasting the burn logic.
 *
 * <p><strong>Charge model (SMTCON-217).</strong> A burn cycle is a fixed-size charge drained
 * from the hottest available fuel source: {@value #FUEL_CHARGE_AMOUNT_MB} mB sustains
 * {@value #FUEL_CHARGE_DURATION_TICKS} ticks of heat at the base draw rate. Each server tick
 * the module decrements {@link #remainingFuelTicks} by {@link #fuelDrawPerTick}; when the
 * counter falls to zero or below it tries to drain another charge from the source. The cached
 * fuel temperature carries the smeltery through a charge even if the source tank empties
 * mid-burn, matching upstream Tinkers' Construct 1.20.1's
 * {@code FuelModule.tryLiquidFuel} behaviour.
 *
 * <p><strong>Shell-size scaling.</strong> A bigger smeltery burns the same charge faster — the
 * host (the controller's {@code bindStructure}) calls {@link #setFuelDrawPerTick} with a value
 * derived from the multiblock's wall block count: {@code 1 + walls / 15}, matching TC's
 * {@code BLOCKS_PER_FUEL}. At {@code fuelDrawPerTick = 1} (minimum smeltery) a charge lasts a
 * full {@value #FUEL_CHARGE_DURATION_TICKS} ticks; at higher rates it lasts proportionally
 * fewer ticks.
 *
 * <p>The module is otherwise stateless — it does not own its fuel sources. The host wires in a
 * {@link Supplier} that resolves the hottest available {@link SmelteryFuelSource} (or
 * {@code null}) and an {@link IntConsumer} that receives the resulting temperature each tick.
 *
 * <p>Burn rate is now independent of how many items are melting — heating the bowl melts
 * everything in it for free, exactly as in every TC release since 1.16. The pre-SMTCON-217
 * per-melt rate is gone; the controller's tick gate (only call {@link #tickBurn} when the
 * smeltery has active loads) is what keeps an idle furnace cold.
 */
public final class SmelteryFuelModule {

    /**
     * Millibuckets of fuel a single charge drains from the active fuel tank. Mirrors the
     * {@code amount} field of upstream TC 1.20.1's lava {@code melting_fuel} recipe.
     */
    public static final int FUEL_CHARGE_AMOUNT_MB = 50;

    /**
     * Ticks of heat a single fuel charge buys at the base {@link #fuelDrawPerTick} of 1.
     * Mirrors the {@code duration} field of upstream's lava recipe — 50 mB at base rate
     * sustains 100 ticks of heat for a minimum-size smeltery. A larger smeltery's
     * {@code fuelDrawPerTick > 1} burns through the same charge proportionally faster.
     */
    public static final int FUEL_CHARGE_DURATION_TICKS = 100;

    /** Tag key under which the remaining-ticks counter is persisted. */
    private static final String TAG_REMAINING_FUEL_TICKS = "RemainingFuelTicks";

    /** Tag key under which the cached fuel temperature is persisted. */
    private static final String TAG_CACHED_FUEL_TEMPERATURE = "CachedFuelTemperature";

    private final Supplier<SmelteryFuelSource> fuelSourceSupplier;
    private final IntConsumer temperatureSink;

    /**
     * Ticks of heat remaining on the currently-burning fuel charge — decremented by
     * {@link #fuelDrawPerTick} every burn tick. When it reaches zero or below the next call to
     * {@link #tickBurn} pulls a fresh {@value #FUEL_CHARGE_AMOUNT_MB} mB charge from the
     * hottest tank. Persisted so a saved smeltery resumes its in-flight charge.
     */
    private int remainingFuelTicks;

    /**
     * Temperature in kelvin the active charge sustains while {@link #remainingFuelTicks} is
     * positive. Cached at the moment the charge was drained so the source tank can be emptied
     * or removed mid-burn without cooling the smeltery prematurely. Persisted alongside the
     * remaining ticks.
     */
    private int cachedFuelTemperature;

    /**
     * Ticks of fuel-charge progress consumed per server tick — scales with the assembled shell
     * size via the host's {@link #setFuelDrawPerTick} call at bind time. Defaults to 1 (the
     * minimum-size smeltery rate) so a loose controller burns at the base rate when its host
     * has not configured the rate. Derived, not persisted.
     */
    private int fuelDrawPerTick = 1;

    /**
     * @param fuelSourceSupplier resolves the hottest fuel source available <em>this tick</em>,
     *                           or {@code null} when no source can provide fuel. Recomputed on
     *                           every {@link #tickBurn} call so a tank that runs dry or a
     *                           multiblock that loses a tank does not keep heating the burner.
     * @param temperatureSink    receives the temperature the burner heats to this tick:
     *                           positive while heat is sustained (either a fresh charge or
     *                           remaining ticks on an existing one), {@code 0} when paused.
     */
    public SmelteryFuelModule(Supplier<SmelteryFuelSource> fuelSourceSupplier, IntConsumer temperatureSink) {
        this.fuelSourceSupplier = Objects.requireNonNull(fuelSourceSupplier, "fuelSourceSupplier");
        this.temperatureSink = Objects.requireNonNull(temperatureSink, "temperatureSink");
    }

    /**
     * Sets the per-tick burn rate (SMTCON-217). The host calls this from its multiblock-bind
     * path with a rate derived from the shell's wall block count; an idle or unbound host can
     * leave it at the default of 1. Clamped to {@code >= 1} so a misconfigured zero cannot
     * indefinitely sustain a single charge.
     */
    public void setFuelDrawPerTick(int rate) {
        this.fuelDrawPerTick = Math.max(1, rate);
    }

    /** The per-tick burn rate this module is currently using. */
    public int getFuelDrawPerTick() {
        return fuelDrawPerTick;
    }

    /** Whether the module has remaining ticks on an in-flight charge — diagnostic / UI accessor. */
    public boolean hasFuel() {
        return remainingFuelTicks > 0;
    }

    /** Remaining ticks on the active charge, or {@code 0} when no charge is active. */
    public int getRemainingFuelTicks() {
        return remainingFuelTicks;
    }

    /** Reports the temperature {@link #tickBurn} last sustained on the active charge. */
    public int getCachedFuelTemperature() {
        return cachedFuelTemperature;
    }

    /**
     * Runs one tick of burner work. If the current charge has ticks remaining, decrements by
     * {@link #fuelDrawPerTick} and emits the cached temperature. Otherwise tries to drain a new
     * {@value #FUEL_CHARGE_AMOUNT_MB} mB charge from the hottest fuel source; on success the
     * charge fields are refreshed and the source's temperature emitted. With no remaining ticks
     * and no source that can refill the charge, emits {@code 0} and returns {@code false} so
     * the host can pause its active loads.
     */
    public boolean tickBurn() {
        return tickBurnGated(temperature -> true);
    }

    /**
     * Like {@link #tickBurn()} but a fresh-charge draw is gated by {@code temperaturePredicate},
     * which sees the temperature the next charge <em>would</em> sustain (via
     * {@link SmelteryFuelSource#previewFuelTemperature()}). When the predicate rejects the
     * previewed temperature, no fuel is consumed and the burner reports zero heat — so a host
     * with active loads that require less heat than this fuel provides can refuse to burn an
     * expensive source on a cheap recipe (SMTCON-224).
     *
     * <p>Ticks already paid for on the active charge are not gated — once the host has
     * committed to a charge, the remaining ticks burn through at the cached temperature
     * regardless of whether a future charge would still satisfy the predicate. This matches
     * upstream behaviour: the gate decides whether to <em>start</em> a charge, not whether to
     * continue one.
     */
    public boolean tickBurnGated(IntPredicate temperaturePredicate) {
        Objects.requireNonNull(temperaturePredicate, "temperaturePredicate");
        if (remainingFuelTicks > 0) {
            remainingFuelTicks -= fuelDrawPerTick;
            temperatureSink.accept(cachedFuelTemperature);
            return true;
        }
        SmelteryFuelSource hottest = fuelSourceSupplier.get();
        if (hottest == null) {
            temperatureSink.accept(0);
            return false;
        }
        int previewed = hottest.previewFuelTemperature();
        if (previewed <= 0 || !temperaturePredicate.test(previewed)) {
            temperatureSink.accept(0);
            return false;
        }
        // A tank that holds less than a full charge cannot start a new burn — drain nothing and
        // report no heat so the host pauses cleanly rather than partial-charging.
        int consumed = hottest.consumeFuel(FUEL_CHARGE_AMOUNT_MB);
        if (consumed < FUEL_CHARGE_AMOUNT_MB) {
            temperatureSink.accept(0);
            return false;
        }
        remainingFuelTicks = FUEL_CHARGE_DURATION_TICKS - fuelDrawPerTick;
        cachedFuelTemperature = previewed;
        temperatureSink.accept(previewed);
        return true;
    }

    /**
     * Reads the temperature the burner would heat to on its next fresh charge — without
     * drawing any fuel. {@code 0} when no source can currently provide fuel. Callers can use
     * this to power a UI panel ("you'll get N K next charge") or to feed a recipe-temperature
     * gate to {@link #tickBurnGated} without re-resolving the source themselves.
     *
     * <p>Returns the cached temperature when the module is still burning through ticks paid
     * for by a previous charge — that is the heat the smeltery actually has right now, even
     * if the source tank has since emptied.
     */
    public int previewTemperature() {
        if (remainingFuelTicks > 0) {
            return cachedFuelTemperature;
        }
        SmelteryFuelSource hottest = fuelSourceSupplier.get();
        return hottest == null ? 0 : hottest.previewFuelTemperature();
    }

    /** Serialises the in-flight charge state for disk save (SMTCON-217). */
    public void writeToNBT(CompoundTag tag) {
        tag.putInt(TAG_REMAINING_FUEL_TICKS, remainingFuelTicks);
        tag.putInt(TAG_CACHED_FUEL_TEMPERATURE, cachedFuelTemperature);
    }

    /**
     * Restores the in-flight charge state from disk. Defensive against missing tags (older saves
     * predating SMTCON-217 have no charge state — the smeltery just starts fresh) and against
     * negative values (clamped to zero, which is the natural reset state).
     */
    public void readFromNBT(CompoundTag tag) {
        remainingFuelTicks = tag.contains(TAG_REMAINING_FUEL_TICKS, Tag.TAG_INT) ? Math.max(0, tag.getInt(TAG_REMAINING_FUEL_TICKS)) : 0;
        cachedFuelTemperature = tag.contains(TAG_CACHED_FUEL_TEMPERATURE, Tag.TAG_INT) ? Math.max(0, tag.getInt(TAG_CACHED_FUEL_TEMPERATURE)) : 0;
        // Consistency guard: ticks without a cached temperature is a corrupt or partially-written
        // save — the burner would tick away the charge emitting zero heat, which looks identical
        // to out-of-fuel but still wastes the charge counter. Reset to a clean unburned state.
        if (cachedFuelTemperature == 0) {
            remainingFuelTicks = 0;
        }
    }
}
