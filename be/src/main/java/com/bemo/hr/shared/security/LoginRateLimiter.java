package com.bemo.hr.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    static final int MAX_USERNAME_ATTEMPTS = 5;
    static final int MAX_DEVICE_ATTEMPTS = 15;
    static final int MAX_IP_ATTEMPTS = 50;
    static final int MAX_GLOBAL_IP_ATTEMPTS = 100;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final int MAX_KEYS = 10_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean isGlobalIpBlocked(String ipKey) {
        return blocked(key("global-ip", ipKey), MAX_GLOBAL_IP_ATTEMPTS);
    }

    public boolean isTenantBlocked(String tenantId, String username, String deviceId, String ip) {
        if (blocked(key(tenantId, "username", username), MAX_USERNAME_ATTEMPTS)) return true;
        if (deviceId != null && !deviceId.isBlank()
                && blocked(key(tenantId, "device", deviceId), MAX_DEVICE_ATTEMPTS)) return true;
        return blocked(key(tenantId, "ip", ip), MAX_IP_ATTEMPTS);
    }

    public void recordFailure(String tenantId, String username, String deviceId, String ip) {
        record(key(tenantId, "username", username), MAX_USERNAME_ATTEMPTS);
        if (deviceId != null && !deviceId.isBlank()) {
            record(key(tenantId, "device", deviceId), MAX_DEVICE_ATTEMPTS);
        }
        record(key(tenantId, "ip", ip), MAX_IP_ATTEMPTS);
    }

    public void recordGlobalIpFailure(String ipKey) {
        record(key("global-ip", ipKey), MAX_GLOBAL_IP_ATTEMPTS);
    }

    public void reset(String tenantId, String username) {
        windows.remove(key(tenantId, "username", username));
    }

    private boolean blocked(String key, int limit) {
        Window window = windows.get(key);
        return window != null && window.startedAt.plus(WINDOW).isAfter(Instant.now()) && window.count >= limit;
    }

    private void record(String key, int limit) {
        Instant now = Instant.now();
        windows.compute(key, (k, existing) -> {
            if (existing == null || existing.startedAt.plus(WINDOW).isBefore(now)) {
                return new Window(now, 1);
            }
            return new Window(existing.startedAt, existing.count + 1);
        });
        if (windows.size() > MAX_KEYS) {
            windows.entrySet().removeIf(entry -> entry.getValue().startedAt.plus(WINDOW).isBefore(now));
        }
    }

    private static String key(String... parts) {
        return String.join("|", parts);
    }

    private record Window(Instant startedAt, int count) { }
}
