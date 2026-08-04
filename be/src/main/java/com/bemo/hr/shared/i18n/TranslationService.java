package com.bemo.hr.shared.i18n;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TranslationService {
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
        translationRepository.findAllByLocaleIgnoreCaseOrderByTranslationKeyAsc(normalized)
                .forEach(entry -> messages.put(entry.getTranslationKey(), entry.getTextValue()));
        return new TranslationBundle(normalized, Map.copyOf(messages));
    }

    public String translate(String key, String locale) {
        return translateOrDefault(key, locale, key);
    }

    public String translateOrDefault(String key, String locale, String defaultMsg) {
        return translationRepository.findAllByLocaleIgnoreCaseOrderByTranslationKeyAsc(normalize(locale))
                .stream()
                .filter(e -> e.getTranslationKey().equals(key))
                .map(TranslationEntry::getTextValue)
                .findFirst()
                .orElse(defaultMsg);
    }

    public boolean isSupported(String locale) {
        return SUPPORTED_LOCALES.stream().anyMatch(item -> item.equalsIgnoreCase(locale));
    }

    private String normalize(String locale) {
        return SUPPORTED_LOCALES.stream().filter(item -> item.equalsIgnoreCase(locale))
                .findFirst().orElse("ar-EG");
    }

    public record TranslationBundle(String locale, Map<String, String> messages) { }
}
