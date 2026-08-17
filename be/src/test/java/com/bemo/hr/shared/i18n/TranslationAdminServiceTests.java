package com.bemo.hr.shared.i18n;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranslationAdminServiceTests {
    @Mock
    TranslationRepository translationRepository;
    @Mock
    TenantApplicationRepository appRepository;
    @Mock
    AuditService auditService;

    private TranslationAdminService service() {
        return new TranslationAdminService(translationRepository, appRepository, auditService);
    }

    @Test
    void listCombinesDefaultsAndOverridesWithEffectiveValue() {
        TranslationEntry defaultTitle = new TranslationEntry("nav.title", "ar-EG", "العنوان العام", null);
        TranslationEntry defaultSave = new TranslationEntry("common.save", "ar-EG", "حفظ", null);
        TranslationEntry overrideTitle = new TranslationEntry("nav.title", "ar-EG", "عنوان العميل", "app-1");

        when(appRepository.existsById("app-1")).thenReturn(true);
        when(translationRepository.findAllByLocaleIgnoreCaseAndAppIdIsNullOrderByTranslationKeyAsc("ar-EG"))
                .thenReturn(List.of(defaultSave, defaultTitle));
        when(translationRepository.findAllByLocaleIgnoreCaseAndAppIdOrderByTranslationKeyAsc("ar-EG", "app-1"))
                .thenReturn(List.of(overrideTitle));

        var rows = service().list("ar-EG", "app-1");

        assertThat(rows).extracting(TranslationAdminService.TranslationRow::key)
                .containsExactly("common.save", "nav.title");
        assertThat(rows.get(1).effectiveValue()).isEqualTo("عنوان العميل");
        assertThat(rows.get(1).overridden()).isTrue();
        assertThat(rows.get(0).effectiveValue()).isEqualTo("حفظ");
        assertThat(rows.get(0).overridden()).isFalse();
    }

    @Test
    void pageFiltersAndReturnsOnlyRequestedServerPage() {
        List<String> keys = List.of(
                "translation.key.10", "translation.key.11", "translation.key.12", "translation.key.13",
                "translation.key.14", "translation.key.15", "translation.key.16", "translation.key.17",
                "translation.key.18", "translation.key.19");
        List<TranslationEntry> entries = keys.stream()
                .map(key -> new TranslationEntry(key, "en-US", "Value " + key.substring(key.length() - 2), null))
                .toList();
        var pageable = PageRequest.of(1, 10);

        when(translationRepository.findTranslationKeysForScope("en-US", null, "value", pageable))
                .thenReturn(new PageImpl<>(keys, pageable, 30));
        when(translationRepository.findEntriesForScopeKeys("en-US", null, keys))
                .thenReturn(entries);

        var result = service().page("en-US", null, "value", 1, 10);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(30);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.content()).hasSize(10);
        assertThat(result.content().get(0).key()).isEqualTo("translation.key.10");
        assertThat(result.content().get(9).key()).isEqualTo("translation.key.19");
        assertThat(result.overriddenCount()).isZero();
    }

    @Test
    void pageReportsTotalApplicationOverridesAcrossTheScope() {
        TranslationEntry defaultSave = new TranslationEntry("common.save", "ar-EG", "حفظ", null);
        TranslationEntry defaultTitle = new TranslationEntry("nav.title", "ar-EG", "العنوان العام", null);
        TranslationEntry overrideTitle = new TranslationEntry("nav.title", "ar-EG", "عنوان العميل", "app-1");
        List<String> keys = List.of("common.save", "nav.title");
        var pageable = PageRequest.of(0, 10);

        when(appRepository.existsById("app-1")).thenReturn(true);
        when(translationRepository.findTranslationKeysForScope("ar-EG", "app-1", "", pageable))
                .thenReturn(new PageImpl<>(keys, pageable, 2));
        when(translationRepository.findEntriesForScopeKeys("ar-EG", "app-1", keys))
                .thenReturn(List.of(defaultSave, defaultTitle, overrideTitle));
        when(translationRepository.countByLocaleIgnoreCaseAndAppId("ar-EG", "app-1")).thenReturn(1L);

        var result = service().page("ar-EG", "app-1", "", 0, 10);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.overriddenCount()).isEqualTo(1);
        assertThat(result.content()).extracting(TranslationAdminService.TranslationRow::key)
                .containsExactly("common.save", "nav.title");
        assertThat(result.content().get(1).effectiveValue()).isEqualTo("عنوان العميل");
    }

    @Test
    void importSpreadsheetUpsertsRowsAndReturnsSummary() throws Exception {
        when(appRepository.existsById("app-1")).thenReturn(true);
        TranslationEntry existing = new TranslationEntry("nav.title", "ar-EG", "عنوان قديم", "app-1");

        when(translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppId("ar-EG", "common.save", "app-1"))
                .thenReturn(Optional.empty());
        when(translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppId("ar-EG", "nav.title", "app-1"))
                .thenReturn(Optional.of(existing));

        var file = workbookFile(List.of(
                new String[]{"common.save", "حفظ"},
                new String[]{"nav.title", "عنوان جديد"}
        ));

        var result = service().importSpreadsheet("ar-EG", "app-1", file, "superadmin");

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.unchangedCount()).isZero();
        assertThat(existing.getTextValue()).isEqualTo("عنوان جديد");
        verify(translationRepository, times(2)).save(any(TranslationEntry.class));
        verify(auditService, times(2)).record(eq("TRANSLATION_IMPORT_UPSERT"), eq("TRANSLATION"),
                any(), eq("superadmin"), any(), eq(null));
    }

    @Test
    void saveUpdatesOnlySelectedApplicationAndWritesAuditEvent() {
        TranslationEntry defaultEntry = new TranslationEntry("nav.title", "en-US", "Default title", null);
        TranslationEntry overrideEntry = new TranslationEntry("nav.title", "en-US", "Old title", "app-1");

        when(appRepository.existsById("app-1")).thenReturn(true);
        when(translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppId("en-US", "nav.title", "app-1"))
                .thenReturn(Optional.of(overrideEntry));
        when(translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull("en-US", "nav.title"))
                .thenReturn(Optional.of(defaultEntry));

        var row = service().save("nav.title",
                new TranslationAdminService.TranslationUpdate("en-US", "app-1", "Client title"), "superadmin");

        assertThat(overrideEntry.getTextValue()).isEqualTo("Client title");
        assertThat(row.defaultValue()).isEqualTo("Default title");
        assertThat(row.overrideValue()).isEqualTo("Client title");
        verify(translationRepository).save(overrideEntry);
        verify(auditService).record("TRANSLATION_UPSERT", "TRANSLATION", overrideEntry.getId(),
                "superadmin", "{\"key\":\"nav.title\",\"locale\":\"en-US\",\"scope\":\"app-1\"}", null);
    }

    @Test
    void restoreDefaultDeletesOnlyApplicationOverride() {
        TranslationEntry defaultEntry = new TranslationEntry("nav.title", "en-US", "Default title", null);
        TranslationEntry overrideEntry = new TranslationEntry("nav.title", "en-US", "Client title", "app-1");

        when(appRepository.existsById("app-1")).thenReturn(true);
        when(translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppId("en-US", "nav.title", "app-1"))
                .thenReturn(Optional.of(overrideEntry), Optional.empty());
        when(translationRepository.findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull("en-US", "nav.title"))
                .thenReturn(Optional.of(defaultEntry));

        var row = service().restoreDefault("nav.title", "en-US", "app-1", "superadmin");

        verify(translationRepository).delete(overrideEntry);
        assertThat(row.overrideValue()).isNull();
        assertThat(row.effectiveValue()).isEqualTo("Default title");
        assertThat(row.overridden()).isFalse();
    }

    private MockMultipartFile workbookFile(List<String[]> rows) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("translations");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("key");
            header.createCell(1).setCellValue("value");
            for (int i = 0; i < rows.size(); i++) {
                var row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(rows.get(i)[0]);
                row.createCell(1).setCellValue(rows.get(i)[1]);
            }
            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "translations.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }
}
