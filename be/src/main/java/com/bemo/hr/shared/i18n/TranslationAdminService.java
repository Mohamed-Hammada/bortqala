package com.bemo.hr.shared.i18n;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class TranslationAdminService {
    private static final List<String> SUPPORTED_LOCALES = List.of("ar-EG", "en-US");
    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 25, 50, 100);
    private static final int MAX_IMPORT_ROWS = 5_000;
    private static final long MAX_IMPORT_FILE_BYTES = 5L * 1024 * 1024;

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

    /**
     * Kept for existing callers/tests that need the complete effective list.
     */
    @Transactional(readOnly = true)
    public List<TranslationRow> list(String locale, String appId) {
        return rowsForScope(locale, appId);
    }

    /**
     * Database-backed pagination contract used by the translation-management page.
     * The first query pages distinct translation keys for the selected scope, then
     * a second query loads only the default/override rows required for that page.
     */
    @Transactional(readOnly = true)
    public TranslationPage page(String locale, String appId, String search, int page, int size) {
        if (page < 0) {
            throw new BusinessRuleException("Page index cannot be negative.",
                    "I18N_INVALID_PAGE", HttpStatus.BAD_REQUEST);
        }
        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BusinessRuleException("Page size must be one of 10, 25, 50 or 100.",
                    "I18N_INVALID_PAGE_SIZE", HttpStatus.BAD_REQUEST);
        }

        String normalizedLocale = normalize(locale);
        String normalizedAppId = normalizeAppId(appId);
        validateApp(normalizedAppId);
        String normalizedSearch = search == null ? "" : search.strip();

        int effectivePage = page;
        Page<String> keyPage = translationRepository.findTranslationKeysForScope(
                normalizedLocale, normalizedAppId, normalizedSearch, PageRequest.of(effectivePage, size));

        if (keyPage.getTotalElements() == 0) {
            effectivePage = 0;
        } else if (effectivePage > 0 && effectivePage >= keyPage.getTotalPages()) {
            effectivePage = keyPage.getTotalPages() - 1;
            keyPage = translationRepository.findTranslationKeysForScope(
                    normalizedLocale, normalizedAppId, normalizedSearch, PageRequest.of(effectivePage, size));
        }

        List<String> keys = keyPage.getContent();
        List<TranslationEntry> pageEntries = keys.isEmpty()
                ? List.of()
                : translationRepository.findEntriesForScopeKeys(normalizedLocale, normalizedAppId, keys);

        Map<String, TranslationEntry> defaults = new LinkedHashMap<>();
        Map<String, TranslationEntry> overrides = new LinkedHashMap<>();
        pageEntries.forEach(entry -> {
            if (entry.getAppId() == null) {
                defaults.put(entry.getTranslationKey(), entry);
            } else {
                overrides.put(entry.getTranslationKey(), entry);
            }
        });

        List<TranslationRow> rows = keys.stream().map(key -> {
            TranslationEntry base = defaults.get(key);
            TranslationEntry scoped = overrides.get(key);
            String defaultValue = base == null ? null : base.getTextValue();
            String overrideValue = scoped == null ? null : scoped.getTextValue();
            return new TranslationRow(key, defaultValue, overrideValue,
                    overrideValue != null ? overrideValue : defaultValue, scoped != null);
        }).toList();

        long overriddenCount = normalizedAppId == null
                ? 0
                : translationRepository.countByLocaleIgnoreCaseAndAppId(normalizedLocale, normalizedAppId);

        return new TranslationPage(
                rows,
                effectivePage,
                size,
                keyPage.getTotalElements(),
                keyPage.getTotalPages(),
                overriddenCount);
    }

    @Transactional
    @CacheEvict(cacheNames = "translationBundles", allEntries = true)
    public TranslationImportResult importSpreadsheet(String locale, String appId, MultipartFile file, String actor) {
        String normalizedLocale = normalize(locale);
        String normalizedAppId = normalizeAppId(appId);
        validateApp(normalizedAppId);

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Translation file is required.",
                    "I18N_IMPORT_FILE_REQUIRED", HttpStatus.CONFLICT);
        }
        if (file.getSize() > MAX_IMPORT_FILE_BYTES) {
            throw new BusinessRuleException("Translation Excel file must not exceed 5 MB.",
                    "I18N_IMPORT_FILE_TOO_LARGE", HttpStatus.CONFLICT);
        }

        String filename = file.getOriginalFilename() == null ? "translations.xlsx" : file.getOriginalFilename();
        if (!filename.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xls)$")) {
            throw new BusinessRuleException("Translation file must be an XLSX or XLS workbook.",
                    "I18N_IMPORT_INVALID_FILE_TYPE", HttpStatus.CONFLICT);
        }
        Map<String, String> importedValues = parseWorkbook(file);

        int createdCount = 0;
        int updatedCount = 0;
        int unchangedCount = 0;

        for (var imported : importedValues.entrySet()) {
            String normalizedKey = normalizeKey(imported.getKey());
            String normalizedText = normalizeText(imported.getValue());

            TranslationEntry entry = normalizedAppId == null
                    ? translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull(
                            normalizedLocale, normalizedKey).orElse(null)
                    : translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppId(
                            normalizedLocale, normalizedKey, normalizedAppId).orElse(null);

            if (entry == null) {
                entry = new TranslationEntry(normalizedKey, normalizedLocale, normalizedText, normalizedAppId);
                translationRepository.save(entry);
                createdCount++;
            } else if (!Objects.equals(entry.getTextValue(), normalizedText)) {
                entry.updateTextValue(normalizedText);
                translationRepository.save(entry);
                updatedCount++;
            } else {
                unchangedCount++;
            }

            auditService.record("TRANSLATION_IMPORT_UPSERT", "TRANSLATION",
                    entry.getId(), actor,
                    importAuditDetails(normalizedKey, normalizedLocale, normalizedAppId, filename),
                    null);
        }

        return new TranslationImportResult(
                importedValues.size(),
                createdCount,
                updatedCount,
                unchangedCount);
    }

    @Transactional
    @CacheEvict(cacheNames = "translationBundles", allEntries = true)
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
    @CacheEvict(cacheNames = "translationBundles", allEntries = true)
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

    private Map<String, String> parseWorkbook(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BusinessRuleException("The uploaded Excel file is empty.",
                        "I18N_IMPORT_EMPTY_FILE", HttpStatus.CONFLICT);
            }

            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new BusinessRuleException("The uploaded Excel file is missing a header row.",
                        "I18N_IMPORT_MISSING_HEADER", HttpStatus.CONFLICT);
            }

            int keyColumn = -1;
            int valueColumn = -1;
            DataFormatter formatter = new DataFormatter();
            for (Cell cell : header) {
                String headerValue = normalizeHeader(formatter.formatCellValue(cell));
                if ("key".equals(headerValue) || "translationkey".equals(headerValue)) {
                    keyColumn = cell.getColumnIndex();
                }
                if ("value".equals(headerValue)
                        || "textvalue".equals(headerValue)
                        || "translation".equals(headerValue)
                        || "translationvalue".equals(headerValue)) {
                    valueColumn = cell.getColumnIndex();
                }
            }

            if (keyColumn < 0 || valueColumn < 0) {
                throw new BusinessRuleException("Excel header must include key and value columns.",
                        "I18N_IMPORT_INVALID_HEADER", HttpStatus.CONFLICT);
            }

            Map<String, String> result = new LinkedHashMap<>();
            List<Integer> duplicateRows = new ArrayList<>();
            int importedRowCount = 0;
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String key = formatter.formatCellValue(row.getCell(keyColumn)).strip();
                String value = formatter.formatCellValue(row.getCell(valueColumn)).strip();
                if (key.isEmpty() && value.isEmpty()) {
                    continue;
                }
                if (key.isEmpty() || value.isEmpty()) {
                    throw new BusinessRuleException("Excel import row " + (rowIndex + 1) + " must contain both key and value.",
                            "I18N_IMPORT_INVALID_ROW", HttpStatus.CONFLICT);
                }

                importedRowCount++;
                if (importedRowCount > MAX_IMPORT_ROWS) {
                    throw new BusinessRuleException("Excel import exceeds the maximum allowed 5000 rows.",
                            "I18N_IMPORT_ROW_LIMIT", HttpStatus.CONFLICT);
                }

                String dedupeKey = key.toLowerCase(Locale.ROOT);
                if (result.containsKey(dedupeKey)) {
                    duplicateRows.add(rowIndex + 1);
                }
                result.put(dedupeKey, value);
            }

            if (result.isEmpty()) {
                throw new BusinessRuleException("The uploaded Excel file does not contain any translation rows.",
                        "I18N_IMPORT_EMPTY_DATA", HttpStatus.CONFLICT);
            }

            if (!duplicateRows.isEmpty()) {
                throw new BusinessRuleException("Excel import contains duplicate translation keys at rows " + duplicateRows + ".",
                        "I18N_IMPORT_DUPLICATE_KEYS", HttpStatus.CONFLICT);
            }

            Map<String, String> restoredCase = new LinkedHashMap<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                String key = formatter.formatCellValue(row.getCell(keyColumn)).strip();
                String value = formatter.formatCellValue(row.getCell(valueColumn)).strip();
                if (key.isEmpty() || value.isEmpty()) continue;
                restoredCase.put(key, value);
            }
            return restoredCase;
        } catch (EncryptedDocumentException | IOException ex) {
            throw new BusinessRuleException("The uploaded Excel file could not be read.",
                    "I18N_IMPORT_INVALID_FILE", HttpStatus.CONFLICT);
        }
    }

    private List<TranslationRow> rowsForScope(String locale, String appId) {
        String normalized = normalize(locale);
        String normalizedAppId = normalizeAppId(appId);
        validateApp(normalizedAppId);

        Map<String, TranslationEntry> defaults = byKey(
                translationRepository.findAllByLocaleIgnoreCaseAndAppIdIsNullOrderByTranslationKeyAsc(normalized));
        Map<String, TranslationEntry> overrides = normalizedAppId == null ? Map.of() : byKey(
                translationRepository.findAllByLocaleIgnoreCaseAndAppIdOrderByTranslationKeyAsc(
                        normalized, normalizedAppId));

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

    private String normalizeHeader(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    private String auditDetails(String key, String locale, String appId) {
        return "{\"key\":\"" + escapeJson(key) + "\",\"locale\":\"" + locale
                + "\",\"scope\":\"" + (appId == null ? "DEFAULT" : escapeJson(appId)) + "\"}";
    }

    private String importAuditDetails(String key, String locale, String appId, String filename) {
        return "{\"key\":\"" + escapeJson(key) + "\",\"locale\":\"" + locale
                + "\",\"scope\":\"" + (appId == null ? "DEFAULT" : escapeJson(appId))
                + "\",\"sourceFile\":\"" + escapeJson(filename) + "\"}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record AppOption(String id, String code, String name, boolean active) { }

    public record TranslationUpdate(String locale, String appId, String textValue) { }

    public record TranslationRow(String key, String defaultValue, String overrideValue,
                                 String effectiveValue, boolean overridden) { }

    public record TranslationPage(List<TranslationRow> content,
                                  int page,
                                  int size,
                                  long totalElements,
                                  int totalPages,
                                  long overriddenCount) { }

    public record TranslationImportResult(int importedCount,
                                          int createdCount,
                                          int updatedCount,
                                          int unchangedCount) { }
}
