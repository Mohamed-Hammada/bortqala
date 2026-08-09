package com.bemo.hr.shared.i18n;

import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTests {

    @Mock
    private TranslationRepository repository;

    private TranslationService service() {
        return new TranslationService(repository);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void translateOrDefaultUsesSingleKeyLookup() {
        TranslationEntry entry = org.mockito.Mockito.mock(TranslationEntry.class);
        when(entry.getTextValue()).thenReturn("القيمة");
        when(repository.findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull("ar-EG", "key.one"))
                .thenReturn(Optional.of(entry));

        assertThat(service().translateOrDefault("key.one", "ar-EG", "default")).isEqualTo("القيمة");
        verify(repository, never()).findAllByLocaleIgnoreCaseAndAppIdIsNullOrderByTranslationKeyAsc(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void translateOrDefaultFallsBackToDefaultMessageWhenKeyMissing() {
        when(repository.findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull("ar-EG", "missing.key"))
                .thenReturn(Optional.empty());

        assertThat(service().translateOrDefault("missing.key", "ar-EG", "fallback")).isEqualTo("fallback");
    }

    @Test
    void translateOrDefaultFallsBackToDefaultLocaleForUnsupportedLocale() {
        when(repository.findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull("ar-EG", "key.one"))
                .thenReturn(Optional.empty());

        assertThat(service().translateOrDefault("key.one", "fr-FR", "fallback")).isEqualTo("fallback");
        verify(repository).findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull("ar-EG", "key.one");
    }

    @Test
    void bundleUsesAppOverrideAndKeepsMissingKeysFromDefaultScope() {
        TranslationEntry baseTitle = new TranslationEntry("nav.title", "ar-EG", "العنوان العام", null);
        TranslationEntry baseSave = new TranslationEntry("common.save", "ar-EG", "حفظ", null);
        TranslationEntry appTitle = new TranslationEntry("nav.title", "ar-EG", "عنوان ديمو", "demo-app");
        when(repository.findAllByLocaleIgnoreCaseAndAppIdIsNullOrderByTranslationKeyAsc("ar-EG"))
                .thenReturn(List.of(baseSave, baseTitle));
        when(repository.findAllByLocaleIgnoreCaseAndAppIdOrderByTranslationKeyAsc("ar-EG", "demo-app"))
                .thenReturn(List.of(appTitle));
        TenantContext.set("demo-app");

        TranslationService.TranslationBundle bundle = service().bundle("ar-EG");

        assertThat(bundle.appId()).isEqualTo("demo-app");
        assertThat(bundle.messages()).containsEntry("nav.title", "عنوان ديمو")
                .containsEntry("common.save", "حفظ");
    }

    @Test
    void translatePrefersAppOverrideThenFallsBackToPlatformDefault() {
        TranslationEntry appEntry = new TranslationEntry("key.one", "en-US", "Demo value", "demo-app");
        TranslationEntry defaultEntry = new TranslationEntry("key.two", "en-US", "Default value", null);
        TenantContext.set("demo-app");
        when(repository.findByLocaleIgnoreCaseAndTranslationKeyAndAppId("en-US", "key.one", "demo-app"))
                .thenReturn(Optional.of(appEntry));
        when(repository.findByLocaleIgnoreCaseAndTranslationKeyAndAppId("en-US", "key.two", "demo-app"))
                .thenReturn(Optional.empty());
        when(repository.findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull("en-US", "key.two"))
                .thenReturn(Optional.of(defaultEntry));

        assertThat(service().translateOrDefault("key.one", "en-US", "fallback")).isEqualTo("Demo value");
        assertThat(service().translateOrDefault("key.two", "en-US", "fallback")).isEqualTo("Default value");
    }

    @Test
    void resolveLocaleReturnsDefaultWhenHeaderMissingOrBlank() {
        assertThat(service().resolveLocale(null)).isEqualTo("ar-EG");
        assertThat(service().resolveLocale("   ")).isEqualTo("ar-EG");
        assertThat(service().resolveLocale("")).isEqualTo("ar-EG");
    }

    @Test
    void resolveLocaleMatchesExactSupportedTagCaseInsensitively() {
        assertThat(service().resolveLocale("en-US")).isEqualTo("en-US");
        assertThat(service().resolveLocale("EN-us")).isEqualTo("en-US");
        assertThat(service().resolveLocale("ar-EG")).isEqualTo("ar-EG");
    }

    @Test
    void resolveLocaleMatchesLanguagePrefixToSupportedRegion() {
        assertThat(service().resolveLocale("en")).isEqualTo("en-US");
        assertThat(service().resolveLocale("en-GB, en;q=0.8")).isEqualTo("en-US");
        assertThat(service().resolveLocale("AR")).isEqualTo("ar-EG");
    }

    @Test
    void resolveLocaleRespectsQualityOrdering() {
        assertThat(service().resolveLocale("en-US;q=0.5, ar;q=0.9")).isEqualTo("ar-EG");
        assertThat(service().resolveLocale("fr;q=0.9, en-US;q=0.3")).isEqualTo("en-US");
    }

    @Test
    void resolveLocaleIgnoresUnsupportedLanguagesAndWildcard() {
        assertThat(service().resolveLocale("fr-FR, de;q=0.7")).isEqualTo("ar-EG");
        assertThat(service().resolveLocale("*, en-US;q=0.5")).isEqualTo("en-US");
        assertThat(service().resolveLocale("*")).isEqualTo("ar-EG");
    }

    @Test
    void resolveLocaleUsesExplicitDefaultWhenProvided() {
        assertThat(service().resolveLocale("fr-FR", "en-US")).isEqualTo("en-US");
        assertThat(service().resolveLocale(null, "en-US")).isEqualTo("en-US");
    }
}
