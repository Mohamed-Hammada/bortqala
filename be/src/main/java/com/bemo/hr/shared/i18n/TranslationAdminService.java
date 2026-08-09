package com.bemo.hr.shared.i18n;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TranslationAdminService {
    private static final List<String> SUPPORTED_LOCALES = List.of("ar-EG", "en-US");

    private final TranslationRepository translationRepository;
    private final TenantApplicationRepository appRepository;
    private final AuditService auditService;

    public TranslationAdminService(TranslationRepository translationRepository,
                                   TenantApplicationRepository appRepository,
                                   AuditService auditService) {
        this.translationRepository = translationRepository;
        this.appRepository = appRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AppOption> apps() {
        return appRepository.findAll().stream()
                .map(app -> new AppOption(app.getId(), app.getCode(), app.getName(), app.isActive()))
                .sorted(java.util.Comparator.comparing(AppOption::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TranslationRow> list(String locale, String appId) {
        String normalized = normalize(locale);
        appId = normalizeAppId(appId);
        validateApp(appId);
        Map<String, TranslationEntry> defaults = byKey(
                translationRepository.findAllByLocaleIgnoreCaseAndAppIdIsNullOrderByTranslationKeyAsc(normalized));
        Map<String, TranslationEntry> overrides = appId == null ? Map.of() : byKey(
                translationRepository.findAllByLocaleIgnoreCaseAndAppIdOrderByTranslationKeyAsc(normalized, appId));
        var keys = new java.util.TreeSet<String>();
        keys.addAll(defaults.keySet());
        keys.addAll(overrides.keySet());
        return keys.stream().map(key -> {
            TranslationEntry base = defaults.get(key);
            TranslationEntry scoped = overrides.get(key);
            String defaultValue = base == null ? null : base.getTextValue();
            String overrideValue = scoped == null ? null : scoped.getTextValue();
            return new TranslationRow(key, defaultValue, overrideValue,
                    overrideValue != null ? overrideValue : defaultValue, scoped != null);
        }).toList();
    }

    @Transactional
    public TranslationRow save(String key, TranslationUpdate request, String actor) {
        String normalizedKey = normalizeKey(key);
        String normalizedLocale = normalize(request.locale());
        String textValue = normalizeText(request.textValue());
        String appId = normalizeAppId(request.appId());
        validateApp(appId);

        TranslationEntry entry = appId == null
                ? translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull(
                        normalizedLocale, normalizedKey).orElse(null)
                : translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppId(
                        normalizedLocale, normalizedKey, appId).orElse(null);
        if (entry == null) {
            entry = new TranslationEntry(normalizedKey, normalizedLocale, textValue, appId);
        } else {
            entry.updateTextValue(textValue);
        }
        translationRepository.save(entry);
        auditService.record("TRANSLATION_UPSERT", "TRANSLATION", entry.getId(), actor,
                auditDetails(normalizedKey, normalizedLocale, appId), null);
        return row(normalizedKey, normalizedLocale, appId);
    }

    @Transactional
    public TranslationRow restoreDefault(String key, String locale, String appId, String actor) {
        String normalizedAppId = normalizeAppId(appId);
        if (normalizedAppId == null) {
            throw new BusinessRuleException("Default translations cannot be restored by deleting them.",
                    "I18N_DEFAULT_DELETE_NOT_ALLOWED", HttpStatus.CONFLICT);
        }
        String normalizedKey = normalizeKey(key);
        String normalizedLocale = normalize(locale);
        validateApp(normalizedAppId);
        translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppId(
                normalizedLocale, normalizedKey, normalizedAppId).ifPresent(entry -> {
                    translationRepository.delete(entry);
                    auditService.record("TRANSLATION_OVERRIDE_DELETE", "TRANSLATION", entry.getId(), actor,
                            auditDetails(normalizedKey, normalizedLocale, normalizedAppId), null);
                });
        return row(normalizedKey, normalizedLocale, normalizedAppId);
    }

    private TranslationRow row(String key, String locale, String appId) {
        String defaultValue = translationRepository
                .findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull(locale, key)
                .map(TranslationEntry::getTextValue).orElse(null);
        String overrideValue = appId == null ? null : translationRepository
                .findByLocaleIgnoreCaseAndTranslationKeyAndAppId(locale, key, appId)
                .map(TranslationEntry::getTextValue).orElse(null);
        return new TranslationRow(key, defaultValue, overrideValue,
                overrideValue != null ? overrideValue : defaultValue, overrideValue != null);
    }

    private Map<String, TranslationEntry> byKey(List<TranslationEntry> entries) {
        Map<String, TranslationEntry> result = new LinkedHashMap<>();
        entries.forEach(entry -> result.put(entry.getTranslationKey(), entry));
        return result;
    }

    private void validateApp(String appId) {
        if (appId != null && !appRepository.existsById(appId)) {
            throw new BusinessRuleException("Application was not found.", "I18N_APP_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
    }

    private String normalize(String locale) {
        return SUPPORTED_LOCALES.stream().filter(item -> item.equalsIgnoreCase(locale))
                .findFirst().orElseThrow(() -> new BusinessRuleException("Unsupported locale.",
                        "I18N_UNSUPPORTED_LOCALE", HttpStatus.CONFLICT));
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank() || key.strip().length() > 150
                || !key.strip().matches("[A-Za-z0-9._-]+")) {
            throw new BusinessRuleException("Translation key must use letters, numbers, dots, underscores or hyphens and not exceed 150 characters.",
                    "I18N_INVALID_KEY", HttpStatus.CONFLICT);
        }
        return key.strip();
    }

    private String normalizeAppId(String appId) {
        return appId == null || appId.isBlank() ? null : appId.strip();
    }

    private String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException("Translation text is required.",
                    "I18N_TEXT_REQUIRED", HttpStatus.CONFLICT);
        }
        return text.strip();
    }

    private String auditDetails(String key, String locale, String appId) {
        return "{\"key\":\"" + key.replace("\"", "\\\"") + "\",\"locale\":\"" + locale
                + "\",\"scope\":\"" + (appId == null ? "DEFAULT" : appId) + "\"}";
    }

    public record AppOption(String id, String code, String name, boolean active) { }
    public record TranslationUpdate(String locale, String appId, String textValue) { }
    public record TranslationRow(String key, String defaultValue, String overrideValue,
                                 String effectiveValue, boolean overridden) { }
}
