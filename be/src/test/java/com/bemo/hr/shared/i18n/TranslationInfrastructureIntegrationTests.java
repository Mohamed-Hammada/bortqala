package com.bemo.hr.shared.i18n;

import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TranslationInfrastructureIntegrationTests {
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    TranslationService translationService;
    @Autowired
    CacheManager cacheManager;

    @AfterEach
    void clearContextAndCache() {
        TenantContext.clear();
        var cache = cacheManager.getCache("translationBundles");
        if (cache != null) cache.clear();
    }

    @Test
    @Transactional
    void databaseGeneratesTranslationIdWhenSeedOmitsIt() {
        jdbcTemplate.update("insert into translations (translation_key, locale, text_value) values (?, ?, ?)",
                "test.dynamic-id", "en-US", "Generated");

        String id = jdbcTemplate.queryForObject(
                "select id from translations where translation_key = ? and locale = ?",
                String.class, "test.dynamic-id", "en-US");

        assertThat(id).isNotBlank().hasSize(36);
    }

    @Test
    void publicBundleIsCachedByNormalizedLocaleAndSystemScope() {
        var cache = cacheManager.getCache("translationBundles");
        assertThat(cache).isNotNull();
        cache.clear();

        translationService.bundle("en-US");

        assertThat(cache.get("en-us|SYSTEM")).isNotNull();
    }
}
