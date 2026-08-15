package com.bemo.shared.http;

import java.util.Map;

import com.bemo.shared.logging.JsonLoggingUtil;

/**
 * A single name/value pair used inside structured log records.
 */
public record LogLine(String key, String value) {

    public static LogLine of(String key, String value) {
        return new LogLine(key, value);
    }

    public Map<String, Object> asLog() {
        return Map.of(key, value);
    }
}
