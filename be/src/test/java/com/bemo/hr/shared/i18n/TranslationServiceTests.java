package com.bemo.hr.shared.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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

    @Test
    void translateOrDefaultUsesSingleKeyLookup() {
        TranslationEntry entry = org.mockito.Mockito.mock(TranslationEntry.class);
        when(entry.getTextValue()).thenReturn("القيمة");
        when(repository.findByLocaleIgnoreCaseAndTranslationKey("ar-EG", "key.one")).thenReturn(Optional.of(entry));

        assertThat(service().translateOrDefault("key.one", "ar-EG", "default")).isEqualTo("القيمة");
        verify(repository, never()).findAllByLocaleIgnoreCaseOrderByTranslationKeyAsc(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void translateOrDefaultFallsBackToDefaultMessageWhenKeyMissing() {
        when(repository.findByLocaleIgnoreCaseAndTranslationKey("ar-EG", "missing.key"))
                .thenReturn(Optional.empty());

        assertThat(service().translateOrDefault("missing.key", "ar-EG", "fallback")).isEqualTo("fallback");
    }

    @Test
    void translateOrDefaultFallsBackToDefaultLocaleForUnsupportedLocale() {
        when(repository.findByLocaleIgnoreCaseAndTranslationKey("ar-EG", "key.one")).thenReturn(Optional.empty());

        assertThat(service().translateOrDefault("key.one", "fr-FR", "fallback")).isEqualTo("fallback");
        verify(repository).findByLocaleIgnoreCaseAndTranslationKey("ar-EG", "key.one");
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
