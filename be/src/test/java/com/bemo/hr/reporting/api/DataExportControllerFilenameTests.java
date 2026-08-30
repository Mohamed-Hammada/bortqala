package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.application.DataExportService;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.shared.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportControllerFilenameTests {

    @Mock private DataExportService dataExportService;
    @Mock private AuthService authService;
    @Mock private TranslationService translationService;
    private DataExportController controller;

    @BeforeEach
    void setUp() {
        controller = new DataExportController(dataExportService, authService, translationService);
    }

    private AuthApi.PreferenceResponse pref(String locale) {
        return new AuthApi.PreferenceResponse(
                ThemePreference.LIGHT, TableDensity.COMPACT, locale, ExcelTableStyle.GOLD,
                20, "/", false, false, 0, Set.of(), Set.of(), List.of(),
                false, false, Instant.EPOCH);
    }

    @Test
    void arLocaleUsesTranslatedDisplayNameFromTranslationCatalog() {
        when(authService.currentPreferences("alice")).thenReturn(pref("ar-EG"));
        when(translationService.translateOrDefault("export.file.categories", "ar-EG", "categories"))
                .thenReturn("الفئات");
        when(dataExportService.categories(any())).thenReturn(new byte[]{1});

        var response = controller.export("categories", null, null, null, null,
                new UsernamePasswordAuthenticationToken("alice", null));

        String header = response.getHeaders().getFirst("Content-Disposition");
        assertThat(header).contains("filename*=UTF-8''");
        assertThat(header).contains("%D8%A7%D9%84%D9%81%D8%A6%D8%A7%D8%AA-");
    }

    @Test
    void enLocaleUsesScopeSlugDirectly() {
        when(authService.currentPreferences("bob")).thenReturn(pref("en-US"));
        when(dataExportService.categories(any())).thenReturn(new byte[]{1});

        var response = controller.export("categories", null, null, null, null,
                new UsernamePasswordAuthenticationToken("bob", null));

        String header = response.getHeaders().getFirst("Content-Disposition");
        assertThat(header).contains("categories-");
    }
}
