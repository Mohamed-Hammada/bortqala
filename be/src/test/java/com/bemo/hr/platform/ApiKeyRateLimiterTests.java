package com.bemo.hr.platform;

import com.bemo.hr.platform.infrastructure.ApiKeyRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyRateLimiterTests {

    private static final Instant NOW = Instant.parse("2026-08-28T10:00:00Z");

    @Test
    void withinLimit_allows() {
        ApiKeyRateLimiter limiter = new ApiKeyRateLimiter();
        assertTrue(limiter.tryAcquire("app-1", "key-1", 3, NOW));
        assertTrue(limiter.tryAcquire("app-1", "key-1", 3, NOW.plusSeconds(10)));
        assertTrue(limiter.tryAcquire("app-1", "key-1", 3, NOW.plusSeconds(20)));
    }

    @Test
    void overLimit_blocks() {
        ApiKeyRateLimiter limiter = new ApiKeyRateLimiter();
        assertTrue(limiter.tryAcquire("app-1", "key-1", 2, NOW));
        assertTrue(limiter.tryAcquire("app-1", "key-1", 2, NOW.plusSeconds(5)));
        assertFalse(limiter.tryAcquire("app-1", "key-1", 2, NOW.plusSeconds(30)));
    }

    @Test
    void windowRollover_resetsCount() {
        ApiKeyRateLimiter limiter = new ApiKeyRateLimiter();
        assertTrue(limiter.tryAcquire("app-1", "key-1", 1, NOW));
        assertFalse(limiter.tryAcquire("app-1", "key-1", 1, NOW.plusSeconds(30)));
        assertTrue(limiter.tryAcquire("app-1", "key-1", 1, NOW.plusSeconds(61)));
    }

    @Test
    void differentKeys_areIsolated() {
        ApiKeyRateLimiter limiter = new ApiKeyRateLimiter();
        assertTrue(limiter.tryAcquire("app-1", "key-a", 1, NOW));
        assertTrue(limiter.tryAcquire("app-1", "key-b", 1, NOW));
        assertFalse(limiter.tryAcquire("app-1", "key-a", 1, NOW));
    }

    @Test
    void unlimited_whenLimitNonPositive() {
        ApiKeyRateLimiter limiter = new ApiKeyRateLimiter();
        assertTrue(limiter.tryAcquire("app-1", "key-1", 0, NOW));
        assertTrue(limiter.tryAcquire("app-1", "key-1", 0, NOW));
    }
}