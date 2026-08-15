package com.bemo.shared.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaskersTest {

    @Test
    void shortValuesAreFullyMasked() {
        assertEquals("********", Maskers.mask("secret"));
    }

    @Test
    void longValuesKeepFirstAndLastCharacters() {
        String masked = Maskers.mask("abcdefghijklmnopqrstuvwxyz");
        assertEquals("ab********yz", masked);
    }

    @Test
    void customMaskIsUsedWhenProvided() {
        assertEquals("###", Maskers.mask("x", "###"));
    }

    @Test
    void nullAndBlankArePassedThrough() {
        assertEquals(null, Maskers.mask(null));
        assertEquals("", Maskers.mask(""));
    }

    @Test
    void urlsAreExcludedEntirely() {
        assertTrue(Maskers.isExcluded("https://example.com/with/token"));
        assertEquals("", Maskers.mask("http://example.com?a=b"));
    }

    @Test
    void hasErrorDetectsUrlsInAList() {
        assertTrue(Maskers.hasError(java.util.List.of("fine", "http://evil.example")));
        assertFalse(Maskers.hasError(java.util.List.of("fine", "ok")));
    }
}
