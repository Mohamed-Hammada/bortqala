package com.bemo.shared.http;

import java.nio.charset.StandardCharsets;

/**
 * Utilities for turning raw HTTP bodies into compact, log-safe one-line strings.
 */
public final class LoggingUtils {

    public static final int MAX_BODY_CHARS = 2000;

    private LoggingUtils() {
    }

    /** Collapses whitespace and truncates the body so log lines stay single-line and bounded. */
    public static String sanitizeBody(byte[] body) {
        if (body == null || body.length == 0) {
            return null;
        }
        String text = new String(body, StandardCharsets.UTF_8);
        if (text.isBlank()) {
            return null;
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        if (compact.length() > MAX_BODY_CHARS) {
            compact = compact.substring(0, MAX_BODY_CHARS) + "...(truncated)";
        }
        return compact;
    }
}
