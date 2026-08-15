package com.bemo.shared.logging;

import java.util.List;

import org.springframework.util.StringUtils;

/**
 * Central masking engine. Short values (length {@code <= fullMaskThreshold}) are fully masked;
 * longer values keep their first two and last two characters so context remains readable.
 * URL-like payloads are excluded entirely rather than masked.
 */
public final class Maskers {

    public static final String DEFAULT_MASK = "********";
    public static final String EMPTY_STRING = "";
    public static final int DEFAULT_FULL_MASK_THRESHOLD = 12;
    public static final int DEFAULT_PARTIAL_PREFIX = 2;
    public static final int DEFAULT_PARTIAL_SUFFIX = 2;

    private static final List<Exclusion> EXCLUSIONS =
            List.of(new UrlBasedExclusion(UrlBasedExclusion.ExclusionType.URL));

    private Maskers() {
    }

    public static boolean isExcluded(String data) {
        return EXCLUSIONS.stream().anyMatch(exclusion -> exclusion.excluded(data));
    }

    public static String mask(String data) {
        return mask(data, DEFAULT_MASK, DEFAULT_FULL_MASK_THRESHOLD);
    }

    public static String mask(String data, String mask) {
        return mask(data, mask, DEFAULT_FULL_MASK_THRESHOLD);
    }

    public static String mask(String data, String mask, int fullMaskThreshold) {
        if (isExcluded(data)) {
            return EMPTY_STRING;
        }
        if (!StringUtils.hasText(data)) {
            return data;
        }
        if (data.length() <= fullMaskThreshold) {
            return mask;
        }
        return data.substring(0, DEFAULT_PARTIAL_PREFIX)
                + mask
                + data.substring(data.length() - DEFAULT_PARTIAL_SUFFIX);
    }

    public static boolean hasError(List<String> values) {
        return values != null && values.stream().anyMatch(Maskers::isExcluded);
    }
}
