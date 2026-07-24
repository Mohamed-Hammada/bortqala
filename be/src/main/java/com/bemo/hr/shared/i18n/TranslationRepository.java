package com.bemo.hr.shared.i18n;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranslationRepository extends JpaRepository<TranslationEntry, String> {
    List<TranslationEntry> findAllByLocaleIgnoreCaseOrderByTranslationKeyAsc(String locale);
}
