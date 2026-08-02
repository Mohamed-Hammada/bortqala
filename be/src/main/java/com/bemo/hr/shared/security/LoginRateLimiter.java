package com.bemo.hr.shared.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    static final int MAX_ATTEMPTS = 10;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final int MAX_KEYS = 10_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(String tenantKey, String usernameKey, String deviceKey, String ipKey) {
        return allow(key(tenantKey, usernameKey)) && allow(key(tenantKey, "device", deviceKey))
                && allow(key(tenantKey, "ip", ipKey));
    }

    public boolean allow(String key) {
        Instant now = Instant.now();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.startedAt.plus(WINDOW).isBefore(now)) {
                return new Window(now, 1);
            }
            return new Window(existing.startedAt, existing.count + 1);
        });
        if (windows.size() > MAX_KEYS) {
            windows.entrySet().removeIf(entry -> entry.getValue().startedAt.plus(WINDOW).isBefore(now));
        }
        return window.count <= MAX_ATTEMPTS;
    }

    public void reset(String tenantKey, String usernameKey) {
        windows.remove(key(tenantKey, usernameKey));
    }

    private static String key(String... parts) {
        return String.join("|", parts);
    }

    private record Window(Instant startedAt, int count) { }
}
