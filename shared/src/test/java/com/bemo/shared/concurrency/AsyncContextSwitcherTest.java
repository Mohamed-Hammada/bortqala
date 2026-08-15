package com.bemo.shared.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class AsyncContextSwitcherTest {

    @Test
    void contextRunnablePropagatesMdcToWorkerThread() throws Exception {
        MDC.put("requestId", "req-123");
        AsyncContextSwitcher.ContextSnapshot snapshot = AsyncContextSwitcher.capture();
        MDC.remove("requestId");

        ExecutorService pool = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> seen = new AtomicReference<>();
        try {
            pool.execute(AsyncContextSwitcher.contextRunnable(snapshot, () -> {
                seen.set(MDC.get("requestId"));
                latch.countDown();
            }));
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals("req-123", seen.get());
        } finally {
            pool.shutdownNow();
            MDC.clear();
        }
    }

    @Test
    void scopeRestoresPreviousContextOnClose() {
        MDC.put("requestId", "before");
        AsyncContextSwitcher.ContextSnapshot snapshot = AsyncContextSwitcher.capture();
        MDC.put("requestId", "after");

        try (var scope = snapshot.enter()) {
            assertEquals("before", MDC.get("requestId"));
        }
        assertEquals("after", MDC.get("requestId"));
        MDC.clear();
    }

    @Test
    void emptySnapshotClearsMdc() {
        MDC.clear();
        AsyncContextSwitcher.ContextSnapshot snapshot = AsyncContextSwitcher.capture();
        MDC.put("requestId", "stray");

        try (var scope = snapshot.enter()) {
            assertNull(MDC.get("requestId"));
        }
        MDC.clear();
    }
}
