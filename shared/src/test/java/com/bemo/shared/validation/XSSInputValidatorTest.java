package com.bemo.shared.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class XSSInputValidatorTest {

    @Test
    void plainTextIsValid() {
        assertTrue(XSSInputValidator.isValid("hello world"));
        assertTrue(XSSInputValidator.isValid("حاجز text with numbers 123"));
    }

    @Test
    void scriptTagsAreFlagged() {
        assertTrue(XSSInputValidator.isMalicious("<script>alert(1)</script>"));
        assertTrue(XSSInputValidator.isMalicious("<SCRIPT>alert(1)</SCRIPT>"));
    }

    @Test
    void javascriptUrlsAreFlagged() {
        assertTrue(XSSInputValidator.isMalicious("<a href='javascript:alert(1)'>x</a>"));
    }

    @Test
    void eventHandlersAreFlagged() {
        assertTrue(XSSInputValidator.isMalicious("<img src=x onerror=alert(1)>"));
        assertTrue(XSSInputValidator.isMalicious("<div onmouseover=\"alert(1)\">"));
    }
}
