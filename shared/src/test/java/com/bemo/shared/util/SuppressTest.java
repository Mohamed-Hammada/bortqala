package com.bemo.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class SuppressTest {

    @Test
    void uncheckedWrapsResult() {
        int value = Suppress.unchecked(() -> Integer.parseInt("42"));
        assertEquals(42, value);
    }

    @Test
    void uncheckedRunnableRethrowsOriginalExceptionSneakily() {
        assertThrows(IOException.class, () ->
                Suppress.unchecked(() -> {
                    throw new IOException("boom");
                }));
    }

    @Test
    void uncheckedThrowsOriginalExceptionSneakily() {
        assertThrows(IOException.class, () ->
                Suppress.unchecked(() -> {
                    throw new IOException("boom");
                }));
    }
}
