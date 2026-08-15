package com.bemo.shared.validation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Rich-text sanitisation built on jsoup. Strips scripts, event handlers and dangerous URLs so
 * stored HTML cannot execute client-side. Use for content marked {@code @SafeHtml}.
 */
public final class HTMLValidator {

    private static final Safelist SAFE_TAGS = Safelist.relaxed()
            .addTags("p", "br", "span", "div")
            .addAttributes("span", "style")
            .addAttributes("a", "target", "rel")
            .addProtocols("a", "href", "http", "https")
            .addProtocols("img", "src", "http", "https", "data");

    private HTMLValidator() {
    }

    public static boolean isSafe(String html) {
        if (html == null || html.isBlank()) {
            return true;
        }
        String sanitized = sanitize(html);
        return html.equals(sanitized);
    }

    /** Removes anything that is not in the safe allow-list. */
    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        Document.OutputSettings settings = new Document.OutputSettings()
                .prettyPrint(false);
        return Jsoup.clean(html, "", SAFE_TAGS, settings);
    }
}
