package com.bemo.shared.logging;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

/**
 * Safe logging entry point. Every value passed through {@code safeLog}/{@code formatLog} is
 * masked with {@link Maskers}, so secrets, tokens and URLs never reach log files.
 */
public final class SafeLoggingUtil {

    private SafeLoggingUtil() {
    }

    public static String safeLog(String original) {
        return original == null ? null : Maskers.mask(original);
    }

    public static boolean hasSensitiveData(String value) {
        return value != null && Maskers.isExcluded(value);
    }

    /**
     * Renders a SLF4J-style message template, masking each argument before substitution.
     * Example: {@code formatLog("login for {} failed", "user@example.com")}.
     */
    public static String formatLog(String message, Object... args) {
        Object[] masked = Arrays.stream(args)
                .map(SafeLoggingUtil::mask)
                .toArray();
        return MessageFormatter.arrayFormat(message, masked).getMessage();
    }

    private static Object mask(Object value) {
        return value instanceof String s ? Maskers.mask(s) : value;
    }

    /** Logs a masked, formatted WARN line and returns the formatted message for rethrow. */
    public static String logAndWarn(Logger logger, String message, Object... args) {
        String formatted = formatLog(message, args);
        logger.warn(formatted);
        return formatted;
    }

    /** Logs a masked, formatted ERROR line and returns the formatted message for rethrow. */
    public static String logAndError(Logger logger, String message, Object... args) {
        String formatted = formatLog(message, args);
        logger.error(formatted);
        return formatted;
    }
}
