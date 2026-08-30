package com.bemo.hr.platform.infrastructure;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed one-minute-window rate limiter, one bucket per API key. In-memory; a restart resets every window.
 */
@Component
public class ApiKeyRateLimiter {

    private static final long WINDOW_MILLIS = 60_000L;

    private record Bucket(long windowStart, AtomicInteger count) {
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @return {@code true} when the request is within the key's per-minute allowance.
     */
    public boolean tryAcquire(String appId, String keyId, int limitPerMin, Instant now) {
        if (limitPerMin <= 0) {
            return true;
        }
        long window = now.toEpochMilli() / WINDOW_MILLIS;
        String bucketKey = appId + ":" + keyId;
        Bucket current = buckets.compute(bucketKey, (key, previous) -> {
            if (previous == null || previous.windowStart() != window) {
                return new Bucket(window, new AtomicInteger(0));
            }
            return previous;
        });
        return current.count().incrementAndGet() <= limitPerMin;
    }

    /** Test/diagnostic helper: forgets every window so a new fixed window starts clean. */
    public void reset() {
        buckets.clear();
    }
}