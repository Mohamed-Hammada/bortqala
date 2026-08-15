package com.bemo.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class NumberUtilsTest {

    @Test
    void toIntUsesFallback() {
        assertEquals(42, NumberUtils.toInt("42", 0));
        assertEquals(0, NumberUtils.toInt("abc", 0));
        assertEquals(7, NumberUtils.toInt(null, 7));
    }

    @Test
    void toBigDecimalHandlesBadInput() {
        assertEquals(new BigDecimal("1.50"), NumberUtils.toBigDecimal("1.50", BigDecimal.ZERO));
        assertEquals(BigDecimal.ZERO, NumberUtils.toBigDecimal("oops", BigDecimal.ZERO));
    }

    @Test
    void isNumeric() {
        assertTrue(NumberUtils.isNumeric("123.45"));
        assertTrue(NumberUtils.isNumeric("-2"));
        assertFalse(NumberUtils.isNumeric("12a"));
        assertFalse(NumberUtils.isNumeric(""));
    }

    @Test
    void roundUsesHalfUp() {
        assertEquals(new BigDecimal("2.35"), NumberUtils.round(new BigDecimal("2.345"), 2));
    }
}
