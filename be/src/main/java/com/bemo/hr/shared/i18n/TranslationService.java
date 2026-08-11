package com.bemo.hr.shared.i18n;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TranslationService {
    private static final String DEFAULT_LOCALE = "ar-EG";
    private static final Set<String> SUPPORTED_LOCALES = Set.of("ar-EG", "en-US");
    private final TranslationRepository translationRepository;

    public TranslationService(TranslationRepository translationRepository) {
        this.translationRepository = translationRepository;
    }

    public TranslationBundle bundle(String locale) {
        String normalized = SUPPORTED_LOCALES.stream().filter(item -> item.equalsIgnoreCase(locale))
                .findFirst().orElseThrow(() -> new BusinessRuleException("Unsupported locale.",
                        "I18N_UNSUPPORTED_LOCALE", HttpStatus.CONFLICT));
        Map<String, String> messages = new LinkedHashMap<>();
        translationRepository.findAllByLocaleIgnoreCaseAndAppIdIsNullOrderByTranslationKeyAsc(normalized)
                .forEach(entry -> messages.put(entry.getTranslationKey(), entry.getTextValue()));
        String appId = TenantContext.current();
        if (appId != null) {
            translationRepository.findAllByLocaleIgnoreCaseAndAppIdOrderByTranslationKeyAsc(normalized, appId)
                    .forEach(entry -> messages.put(entry.getTranslationKey(), entry.getTextValue()));
        }
        return new TranslationBundle(normalized, appId, Map.copyOf(messages));
    }

    public String translate(String key, String locale) {
        return translateOrDefault(key, locale, key);
    }

    public String translateOrDefault(String key, String locale, String defaultMsg) {
        String normalized = normalize(locale);
        String appId = TenantContext.current();
        if (appId != null) {
            var scoped = translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppId(normalized, key, appId);
            if (scoped.isPresent()) return scoped.get().getTextValue();
        }
        return translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull(normalized, key)
                .map(TranslationEntry::getTextValue).orElse(defaultMsg);
    }

    public boolean isSupported(String locale) {
        return SUPPORTED_LOCALES.stream().anyMatch(item -> item.equalsIgnoreCase(locale));
    }

    public String resolveLocale(String acceptLanguage) {
        return resolveLocale(acceptLanguage, DEFAULT_LOCALE);
    }

    public String resolveLocale(String acceptLanguage, String defaultLocale) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return defaultLocale;
        }
        List<LocalePreference> preferences = new ArrayList<>();
        for (String part : acceptLanguage.split(",")) {
            if (part == null || part.isBlank()) continue;
            String tag = part.trim();
            double quality = 1.0;
            int semicolon = tag.indexOf(';');
            if (semicolon >= 0) {
                quality = parseQuality(tag.substring(semicolon + 1));
                tag = tag.substring(0, semicolon).trim();
            }
            if (tag.isEmpty() || "*".equals(tag)) continue;
            preferences.add(new LocalePreference(tag, quality));
        }
        preferences.sort(Comparator.comparingDouble(LocalePreference::quality).reversed());
        for (LocalePreference preference : preferences) {
            String matched = matchSupported(preference.locale());
            if (matched != null) {
                return matched;
            }
        }
        return defaultLocale;
    }

    private double parseQuality(String parameters) {
        for (String parameter : parameters.split(";")) {
            String entry = parameter.trim();
            if (entry.regionMatches(true, 0, "q=", 0, 2)) {
                try {
                    double value = Double.parseDouble(entry.substring(2).trim());
                    if (value >= 0 && value <= 1) return value;
                } catch (NumberFormatException ignored) {
                    // Ignore malformed quality and fall through to the default.
                }
            }
        }
        return 1.0;
    }

    private String matchSupported(String tag) {
        for (String supported : SUPPORTED_LOCALES) {
            if (supported.equalsIgnoreCase(tag)) return supported;
        }
        int dash = tag.indexOf('-');
        String language = (dash > 0 ? tag.substring(0, dash) : tag).trim();
        if (language.isEmpty()) return null;
        for (String supported : SUPPORTED_LOCALES) {
            if (supported.length() > language.length()
                    && supported.charAt(language.length()) == '-'
                    && supported.regionMatches(true, 0, language, 0, language.length())) {
                return supported;
            }
        }
        return null;
    }

    private String normalize(String locale) {
        return SUPPORTED_LOCALES.stream().filter(item -> item.equalsIgnoreCase(locale))
                .findFirst().orElse(DEFAULT_LOCALE);
    }

    private record LocalePreference(String locale, double quality) { }

    public record TranslationBundle(String locale, String appId, Map<String, String> messages) { }
}
