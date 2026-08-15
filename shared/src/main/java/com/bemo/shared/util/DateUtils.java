package com.bemo.shared.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Date helpers aligned with the repo's epoch-millisecond API date convention and ISO-8601
 * display formatting.
 */
public final class DateUtils {

    public static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateUtils() {
    }

    /** Epoch millis {@literal ->} instant (null-safe). */
    public static Instant fromEpochMillis(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    /** Instant {@literal ->} epoch millis (null-safe). */
    public static Long toEpochMillis(Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }

    public static LocalDate toUtcDate(Instant instant) {
        return instant == null ? null : instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    public static Instant startOfDayUtc(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static Instant endOfDayUtc(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
    }

    public static LocalDateTime toUtcDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static String formatIso(Instant instant) {
        return instant == null ? null : ISO_DATETIME.format(toUtcDateTime(instant));
    }

    public static String formatIsoDate(LocalDate date) {
        return date == null ? null : ISO_DATE.format(date);
    }

    public static boolean isBetween(Instant value, Instant startInclusive, Instant endExclusive) {
        return value != null && startInclusive != null && endExclusive != null
                && !value.isBefore(startInclusive) && value.isBefore(endExclusive);
    }
}
