package com.bemo.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DateUtilsTest {

    @Test
    void epochMillisRoundTrip() {
        Instant instant = Instant.parse("2026-08-15T10:00:00Z");
        Long millis = DateUtils.toEpochMillis(instant);
        assertEquals(instant, DateUtils.fromEpochMillis(millis));
        assertNull(DateUtils.toEpochMillis(null));
        assertNull(DateUtils.fromEpochMillis(null));
    }

    @Test
    void dayBoundariesAreUtc() {
        LocalDate date = LocalDate.of(2026, 8, 15);
        assertEquals("2026-08-15T00:00:00Z", DateUtils.startOfDayUtc(date).toString());
        assertEquals("2026-08-15T23:59:59.999999999Z", DateUtils.endOfDayUtc(date).toString());
    }

    @Test
    void isBetweenUsesHalfOpenInterval() {
        Instant start = Instant.parse("2026-08-15T00:00:00Z");
        Instant end = Instant.parse("2026-08-16T00:00:00Z");
        assertTrue(DateUtils.isBetween(Instant.parse("2026-08-15T12:00:00Z"), start, end));
        assertTrue(DateUtils.isBetween(start, start, end));
        assertTrue(!DateUtils.isBetween(end, start, end));
    }

    @Test
    void formatIsoProducesLocalDateTime() {
        assertEquals("2026-08-15T10:00:00",
                DateUtils.formatIso(Instant.parse("2026-08-15T10:00:00Z")));
    }
}
