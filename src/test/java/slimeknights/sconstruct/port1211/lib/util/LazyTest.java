package slimeknights.sconstruct.port1211.lib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Pinned-behaviour tests for {@link Lazy}: every {@code get()} returns the same cached value;
 * the supplier is invoked at most once even under concurrent access; null values are memoised
 * (not silently re-supplied); a throwing supplier leaves the wrapper unresolved so the next
 * call can try again.
 */
class LazyTest {

    @Test
    void ofRejectsNullSource() {
        assertThrows(NullPointerException.class, () -> Lazy.of(null));
    }

    @Test
    void supplierIsInvokedExactlyOnceAcrossManyGets() {
        AtomicInteger calls = new AtomicInteger();
        Lazy<String> lazy = Lazy.of(() -> {
            calls.incrementAndGet();
            return "computed";
        });

        assertFalse(lazy.isResolved(), "Lazy should not have invoked the supplier before the first get()");
        assertEquals("computed", lazy.get(), "First get() returns the supplier's value");
        assertEquals("computed", lazy.get(), "Second get() returns the cached value");
        assertEquals("computed", lazy.get(), "Third get() returns the cached value");
        assertEquals(1, calls.get(), "Supplier must be invoked exactly once across multiple gets");
        assertTrue(lazy.isResolved(), "isResolved() flips true after the first get()");
    }

    @Test
    void cachedValueIsReferenceStable() {
        Object payload = new Object();
        Lazy<Object> lazy = Lazy.of(() -> payload);
        assertSame(payload, lazy.get(), "First get() must return the supplied instance");
        assertSame(lazy.get(), lazy.get(), "Subsequent calls must return the same reference");
    }

    @Test
    void nullValueIsMemoisedNotResupplied() {
        // A supplier that returns null must only be invoked once — without memoising the null
        // result, the wrapper would call back into the supplier every time get() saw a null
        // cache, defeating the whole point.
        AtomicInteger calls = new AtomicInteger();
        Lazy<String> lazy = Lazy.of(() -> {
            calls.incrementAndGet();
            return null;
        });

        assertNull(lazy.get(), "Lazy must propagate a null result");
        assertNull(lazy.get(), "Second get() must still return null (cached)");
        assertEquals(1, calls.get(), "Supplier must be invoked exactly once even when it returns null");
    }

    @Test
    void throwingSupplierLeavesLazyUnresolvedSoNextCallCanRetry() {
        // The wrapper deliberately doesn't memoise exceptions — a transient supplier failure
        // shouldn't permanently poison the slot. The next caller gets a fresh attempt.
        AtomicBoolean failNextCall = new AtomicBoolean(true);
        Lazy<String> lazy = Lazy.of(() -> {
            if (failNextCall.getAndSet(false)) {
                throw new IllegalStateException("transient");
            }
            return "ok-on-retry";
        });

        assertThrows(IllegalStateException.class, lazy::get);
        assertFalse(lazy.isResolved(), "A throwing supplier must leave the wrapper unresolved");
        assertEquals("ok-on-retry", lazy.get(), "Next call retries the supplier and caches the success");
        assertTrue(lazy.isResolved(), "Successful retry flips isResolved() true");
    }

    @Test
    void concurrentGettersOnlyInvokeTheSupplierOnce() throws Exception {
        int threadCount = 32;
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        Lazy<String> lazy = Lazy.of(() -> {
            calls.incrementAndGet();
            // Simulate non-trivial work so racing threads have a real window to collide.
            try {
                Thread.sleep(5);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "memoised";
        });

        // try-with-resources on ExecutorService is Java 19+; on exit it calls shutdown() and
        // waits for the pool to drain. By that point the done latch has already counted down,
        // so close() returns immediately.
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            List<Future<String>> futures = new ArrayList<>(threadCount);
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        start.await();
                        return lazy.get();
                    }
                    finally {
                        done.countDown();
                    }
                }));
            }
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS), "All threads must complete within 10s");
            // Surface any per-worker AssertionError or exception. Without collecting futures
            // the assertEquals inside the task would be swallowed by submit()'s return value
            // and the test would falsely pass.
            for (Future<String> future : futures) {
                assertEquals("memoised", future.get(1, TimeUnit.SECONDS), "Every worker must observe the memoised value");
            }
        }
        assertEquals(1, calls.get(), "Even with " + threadCount + " concurrent get()s, supplier must run exactly once");
    }
}
