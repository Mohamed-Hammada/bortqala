package com.bemo.shared.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HTMLValidatorTest {

    @Test
    void plainTextIsSafe() {
        assertTrue(HTMLValidator.isSafe("just text"));
    }

    @Test
    void scriptsAreRemovedBySanitise() {
        String sanitized = HTMLValidator.sanitize("<p>Hello <script>alert(1)</script></p>");
        assertFalse(sanitized.contains("<script"));
        assertTrue(sanitized.contains("Hello"));
    }

    @Test
    void safeTagsAreKept() {
        String sanitized = HTMLValidator.sanitize("<p>Hello <b>world</b></p>");
        assertTrue(sanitized.contains("<p>"));
        assertTrue(sanitized.contains("Hello"));
    }

    @Test
    void nullIsPassedThrough() {
        assertEquals(null, HTMLValidator.sanitize(null));
        assertTrue(HTMLValidator.isSafe(null));
    }
}
