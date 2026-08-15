package com.bemo.shared.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JsonLoggingUtilTest {

    @Test
    void toJsonProducesValidObject() {
        var log = JsonLoggingUtil.newLog();
        log.put("type", "request");
        log.put("status", 200);
        String json = JsonLoggingUtil.toJson(log);
        assertTrue(json.contains("\"type\":\"request\""));
        assertTrue(json.contains("\"status\":200"));
    }

    @Test
    void unsafeKeysAreSanitised() {
        assertEquals("request_id", JsonLoggingUtil.key("request id"));
        assertEquals("a-b.c_1", JsonLoggingUtil.key("a-b.c_1"));
    }

    @Test
    void maskValuesMasksStringsOnly() {
        Object[] masked = JsonLoggingUtil.maskValues("supersecret", 42, "http://example.com");
        assertEquals("********", masked[0]);
        assertEquals(42, masked[1]);
        assertEquals("", masked[2]);
    }
}
