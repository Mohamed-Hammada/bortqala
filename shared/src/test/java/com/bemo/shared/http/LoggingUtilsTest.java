package com.bemo.shared.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class LoggingUtilsTest {

    @Test
    void nullAndEmptyBodiesReturnNull() {
        assertNull(LoggingUtils.sanitizeBody(null));
        assertNull(LoggingUtils.sanitizeBody(new byte[0]));
        assertNull(LoggingUtils.sanitizeBody("   ".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void whitespaceIsCollapsedToSingleLine() {
        String sanitized = LoggingUtils.sanitizeBody("{\n  \"a\": 1\n}".getBytes(StandardCharsets.UTF_8));
        assertEquals("{ \"a\": 1 }", sanitized);
    }

    @Test
    void longBodiesAreTruncated() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            big.append('x');
        }
        String sanitized = LoggingUtils.sanitizeBody(big.toString().getBytes(StandardCharsets.UTF_8));
        assertTrue(sanitized.length() <= LoggingUtils.MAX_BODY_CHARS + "...(truncated)".length());
        assertTrue(sanitized.endsWith("(truncated)"));
    }
}
