package com.bemo.shared.logging;

import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Lightweight helpers for emitting structured (JSON) log records. Builds a single-threaded
 * ordered map that the caller hands to the underlying logger, keeping each record a valid
 * JSON object on one line.
 */
public final class JsonLoggingUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonLoggingUtil() {
    }

    public static Map<String, Object> newLog() {
        return new java.util.LinkedHashMap<>();
    }

    public static String toJson(Map<String, Object> log) {
        try {
            return MAPPER.writeValueAsString(log);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /** Sanitises a free-form key so the resulting JSON property name is safe. */
    public static String key(String key) {
        if (key == null) {
            return "null";
        }
        return key.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    /** Copies a value into {@code target} only when the value is present. */
    public static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key(key), value);
        }
    }

    /** Masks every value in the var-arg args array using {@link Maskers}. */
    public static Object[] maskValues(Object... args) {
        if (args == null) {
            return new Object[0];
        }
        return Arrays.stream(args)
                .map(value -> value instanceof String s ? Maskers.mask(s) : value)
                .toArray();
    }
}
