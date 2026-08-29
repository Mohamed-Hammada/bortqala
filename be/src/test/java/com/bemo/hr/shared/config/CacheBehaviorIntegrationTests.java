package com.bemo.hr.shared.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WP-18 T-3: proves the production cache stack (CacheConfig + CacheManager +
 * the {@code dashboard}/{@code accessCatalog} cache names used by
 * DashboardService and AccessCatalogService) behaves as the acceptance criteria
 * require: a second identical call within the TTL hits the cache, the write
 * path evicts, and the TTL is configurable via {@code hr.cache.ttl-seconds}.
 */
@SpringBootTest(properties = "hr.cache.ttl-seconds=1")
@org.springframework.context.annotation.Import(CacheBehaviorIntegrationTests.ProbeConfig.class)
class CacheBehaviorIntegrationTests {

    @TestConfiguration
    static class ProbeConfig {
        @Bean
        ProbeCacheService probeCacheService() {
            return new ProbeCacheService();
        }
    }

    static class ProbeCacheService {
        private final AtomicInteger dashboardCalls = new AtomicInteger();
        private final AtomicInteger catalogCalls = new AtomicInteger();

        @Cacheable(cacheNames = "dashboard", key = "'probe-dashboard'")
        public String dashboardSummary() {
            dashboardCalls.incrementAndGet();
            return "ok";
        }

        @Cacheable(cacheNames = "accessCatalog", key = "'probe-catalog'")
        public String catalogLookup() {
            catalogCalls.incrementAndGet();
            return "ok";
        }

        @CacheEvict(cacheNames = {"dashboard", "accessCatalog"}, allEntries = true)
        public void writePathInvalidesAggregations() {
            // mirrors TranslationAdminService @CacheEvict pattern
        }

        public int getDashboardCalls() {
            return dashboardCalls.get();
        }

        public int getCatalogCalls() {
            return catalogCalls.get();
        }

        public void resetCalls() {
            dashboardCalls.set(0);
            catalogCalls.set(0);
        }
    }

    @Autowired
    CacheManager cacheManager;
    @Autowired
    ProbeCacheService probe;
    @Autowired
    Environment environment;

    @BeforeEach
    void resetCaches() {
        var dashboard = cacheManager.getCache("dashboard");
        var catalog = cacheManager.getCache("accessCatalog");
        if (dashboard != null) {
            dashboard.clear();
        }
        if (catalog != null) {
            catalog.clear();
        }
        if (probe != null) {
            probe.resetCalls();
        }
    }

    @Test
    void secondIdenticalDashboardCallWithinTtlHitsCache() {
        probe.dashboardSummary();
        probe.dashboardSummary();
        assertThat(probe.getDashboardCalls()).isEqualTo(1);

        probe.catalogLookup();
        probe.catalogLookup();
        assertThat(probe.getCatalogCalls()).isEqualTo(1);
    }

    @Test
    void writePathEvictsCachedAggregations() {
        probe.dashboardSummary();
        probe.writePathInvalidesAggregations();
        probe.dashboardSummary();
        assertThat(probe.getDashboardCalls()).isEqualTo(2);
    }

    @Test
    void ttlSecondsPropertyIsWiredIntoCacheManager() throws InterruptedException {
        assertThat(environment.getProperty("hr.cache.ttl-seconds")).isEqualTo("1");

        probe.dashboardSummary(); // entry cached
        long deadline = System.currentTimeMillis() + 5_000;
        while (probe.getDashboardCalls() < 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            probe.dashboardSummary();
        }
        // With the configured 1s TTL the entry expires and the call recomputes;
        // with the 300s default it would never recompute inside the 5s budget.
        assertThat(probe.getDashboardCalls())
                .as("cached value must expire after the configured ttl-seconds")
                .isGreaterThanOrEqualTo(2);
    }
}