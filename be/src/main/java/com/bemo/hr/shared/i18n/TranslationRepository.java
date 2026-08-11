package com.bemo.hr.shared.i18n;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TranslationRepository extends JpaRepository<TranslationEntry, String> {
    List<TranslationEntry> findAllByLocaleIgnoreCaseAndAppIdIsNullOrderByTranslationKeyAsc(String locale);

    List<TranslationEntry> findAllByLocaleIgnoreCaseAndAppIdOrderByTranslationKeyAsc(String locale, String appId);

    Optional<TranslationEntry> findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull(
            String locale, String translationKey);

    Optional<TranslationEntry> findByLocaleIgnoreCaseAndTranslationKeyAndAppId(
            String locale, String translationKey, String appId);
}
