package slimeknights.sconstruct.port1211.lib.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Thread-safe lazy {@link Supplier} that invokes its delegate at most once. Replaces upstream
 * Mantle's {@code slimeknights.mantle.util.LazyValue} — Mantle has no 1.21.x release, so the
 * legacy single-class utility lives here under {@code port1211/lib/util/} until pulses port
 * over.
 *
 * <p>{@link #get} is {@code synchronized} so the first caller's value is the one every caller
 * sees, including across threads. The class deliberately memoises {@code null} too: callers
 * that wrap a supplier returning {@code null} only see one invocation, not one per
 * {@code get()} call. If the supplier throws, the exception propagates and the wrapper stays
 * un-resolved — the next call re-attempts.
 */
public final class Lazy<T> implements Supplier<T> {

    /** Dedicated lock object — synchronising on {@code this} would expose our monitor to
     *  outside callers and let an unrelated caller stall the resolution of every Lazy
     *  instance they happen to hold. */
    private final Object lock = new Object();
    private final Supplier<? extends T> source;
    private T value;
    private boolean resolved;

    private Lazy(Supplier<? extends T> source) {
        this.source = source;
    }

    /** Factory. Construction itself is allocation-only; no invocation of the supplier yet. */
    public static <T> Lazy<T> of(Supplier<? extends T> source) {
        Objects.requireNonNull(source, "source");
        return new Lazy<>(source);
    }

    /**
     * Returns the supplier's value, computing and caching it on the first call. Subsequent calls
     * (including those racing on other threads) return the cached value without re-invoking the
     * supplier.
     */
    @Override
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    public T get() {
        synchronized (lock) {
            if (!resolved) {
                value = source.get();
                resolved = true;
            }
            return value;
        }
    }

    /** Whether {@link #get} has already computed and cached a value. Mostly useful in tests. */
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    public boolean isResolved() {
        synchronized (lock) {
            return resolved;
        }
    }
}
