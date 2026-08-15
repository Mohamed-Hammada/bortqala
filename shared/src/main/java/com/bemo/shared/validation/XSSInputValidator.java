package com.bemo.shared.validation;

import java.util.regex.Pattern;

/**
 * Detects classic XSS payload patterns in free-form input (script tags, javascript: URIs,
 * inline event handlers). The {@code @EnableXSSValidation} filter chain uses this to reject
 * offending request bodies with a 400.
 */
public final class XSSInputValidator {

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<\\s*script[^>]*>|<\\s*\\/\\s*script\\s*>|"
                    + "javascript\\s*:|vbscript\\s*:|"
                    + "on(load|error|click|mouseover|mouseout|focus|blur|change|submit|keyup|keydown|keypress)\\s*=|"
                    + "expression\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    private XSSInputValidator() {
    }

    public static boolean isMalicious(String input) {
        return input != null && XSS_PATTERN.matcher(input).find();
    }

    public static boolean isValid(String input) {
        return !isMalicious(input);
    }
}
