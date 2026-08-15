package com.bemo.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AssertionTest {

    @Test
    void notNullReturnsTheValue() {
        assertEquals("x", Assertion.notNull("x", "value"));
        assertThrows(IllegalArgumentException.class, () -> Assertion.notNull(null, "value"));
    }

    @Test
    void notBlankRejectsWhitespace() {
        assertEquals("abc", Assertion.notBlank("abc", "value"));
        assertThrows(IllegalArgumentException.class, () -> Assertion.notBlank("  ", "value"));
    }

    @Test
    void isTrueAndState() {
        Assertion.isTrue(true, "must be true");
        assertThrows(IllegalArgumentException.class, () -> Assertion.isTrue(false, "must be true"));
        Assertion.state(true, "must hold");
        assertThrows(IllegalStateException.class, () -> Assertion.state(false, "must hold"));
    }

    @Test
    void numericChecks() {
        Assertion.positive(1, "value");
        assertThrows(IllegalArgumentException.class, () -> Assertion.positive(0, "value"));
        Assertion.nonNegative(BigDecimal.ZERO, "value");
        assertThrows(IllegalArgumentException.class, () -> Assertion.nonNegative(new BigDecimal("-1"), "value"));
    }
}
