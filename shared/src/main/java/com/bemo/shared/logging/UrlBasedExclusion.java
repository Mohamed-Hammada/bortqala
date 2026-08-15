package com.bemo.shared.logging;

/**
 * Excludes URL-like payloads (anything containing {@code http} or {@code www.}) from logs by
 * stripping their HTML markup. URLs are excluded because they frequently carry tokens, query
 * strings or SSO links that must not leak into log files.
 */
public class UrlBasedExclusion implements Exclusion {

    private static final String HTML_STRIP_REGEX = "<[^>]+>";

    private final ExclusionType type;

    public UrlBasedExclusion(ExclusionType type) {
        this.type = type;
    }

    @Override
    public boolean excluded(String data) {
        return type == ExclusionType.URL
                && data != null
                && (data.contains("http") || data.contains("www."));
    }

    @Override
    public String excludedSafely(String data) {
        return excluded(data) ? data.replaceAll(HTML_STRIP_REGEX, "") : data;
    }

    public enum ExclusionType {
        URL
    }
}
