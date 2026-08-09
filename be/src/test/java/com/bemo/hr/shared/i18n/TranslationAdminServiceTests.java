package com.bemo.hr.shared.i18n;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationAdminServiceTests {
    @Mock TranslationRepository translationRepository;
    @Mock TenantApplicationRepository appRepository;
    @Mock AuditService auditService;

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
}
