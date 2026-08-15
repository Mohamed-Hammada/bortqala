package com.bemo.hr.shared.i18n;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TranslationRepository extends JpaRepository<TranslationEntry, String> {
    List<TranslationEntry> findAllByLocaleIgnoreCaseAndAppIdIsNullOrderByTranslationKeyAsc(String locale);

    List<TranslationEntry> findAllByLocaleIgnoreCaseAndAppIdOrderByTranslationKeyAsc(String locale, String appId);

    Optional<TranslationEntry> findByLocaleIgnoreCaseAndTranslationKeyAndAppIdIsNull(
            String locale, String translationKey);

    Optional<TranslationEntry> findByLocaleIgnoreCaseAndTranslationKeyAndAppId(
            String locale, String translationKey, String appId);

    @Query(value = """
            select distinct t.translationKey
            from TranslationEntry t
            where lower(t.locale) = lower(:locale)
              and (t.appId is null or t.appId = :appId)
              and (
                    :search = ''
                    or lower(t.translationKey) like lower(concat('%', :search, '%'))
                    or lower(t.textValue) like lower(concat('%', :search, '%'))
              )
            order by t.translationKey asc
            """,
            countQuery = """
            select count(distinct t.translationKey)
            from TranslationEntry t
            where lower(t.locale) = lower(:locale)
              and (t.appId is null or t.appId = :appId)
              and (
                    :search = ''
                    or lower(t.translationKey) like lower(concat('%', :search, '%'))
                    or lower(t.textValue) like lower(concat('%', :search, '%'))
              )
            """)
    Page<String> findTranslationKeysForScope(@Param("locale") String locale,
                                             @Param("appId") String appId,
                                             @Param("search") String search,
                                             Pageable pageable);

    @Query("""
            select t
            from TranslationEntry t
            where lower(t.locale) = lower(:locale)
              and t.translationKey in :keys
              and (t.appId is null or t.appId = :appId)
            order by t.translationKey asc
            """)
    List<TranslationEntry> findEntriesForScopeKeys(@Param("locale") String locale,
                                                    @Param("appId") String appId,
                                                    @Param("keys") List<String> keys);

    long countByLocaleIgnoreCaseAndAppId(String locale, String appId);
}
