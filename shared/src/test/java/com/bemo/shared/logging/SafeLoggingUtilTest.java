package com.bemo.shared.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SafeLoggingUtilTest {

    @Test
    void safeLogMasksSecrets() {
        assertEquals("ab********yz", SafeLoggingUtil.safeLog("abcdefghijklmnopqrstuvwxyz"));
        assertEquals(null, SafeLoggingUtil.safeLog(null));
    }

    @Test
    void formatLogMasksArguments() {
        String rendered = SafeLoggingUtil.formatLog("login {} failed with token {}",
                "user@example.com", "shortsecret");
        assertTrue(rendered.contains("us********om"), "long values keep first/last two chars");
        assertFalse(rendered.contains("user@example.com"));
        assertTrue(rendered.contains("********"));
        assertFalse(rendered.contains("shortsecret"));
    }

    @Test
    void sensitiveDataDetection() {
        assertTrue(SafeLoggingUtil.hasSensitiveData("http://example.com"));
        assertFalse(SafeLoggingUtil.hasSensitiveData("plain text"));
    }

    @Test
    void nonStringArgumentsAreLeftAlone() {
        String rendered = SafeLoggingUtil.formatLog("count {}", 42);
        assertEquals("count 42", rendered);
    }
}
