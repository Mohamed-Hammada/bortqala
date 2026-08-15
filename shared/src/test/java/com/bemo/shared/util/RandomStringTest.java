package com.bemo.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RandomStringTest {

    @Test
    void alphanumericHasRequestedLength() {
        String value = RandomString.alphanumeric(12);
        assertEquals(12, value.length());
        assertTrue(value.matches("[A-Za-z0-9]{12}"));
    }

    @Test
    void numericHasRequestedLengthAndDigitsOnly() {
        String value = RandomString.numeric(8);
        assertEquals(8, value.length());
        assertTrue(value.matches("[0-9]{8}"));
    }

    @Test
    void hexIsTwiceTheByteCount() {
        assertEquals(32, RandomString.hex(16).length());
        assertTrue(RandomString.hex(16).matches("[0-9a-f]{32}"));
    }

    @Test
    void rejectsZeroLength() {
        assertThrows(IllegalArgumentException.class, () -> RandomString.alphanumeric(0));
    }

    @Test
    void producesDistinctValues() {
        String a = RandomString.hex(8);
        String b = RandomString.hex(8);
        assertTrue(!a.equals(b));
    }
}
