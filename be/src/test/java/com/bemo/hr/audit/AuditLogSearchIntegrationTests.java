package com.bemo.hr.audit;

import com.bemo.hr.audit.domain.AuditLog;
import com.bemo.hr.audit.infrastructure.AuditLogRepository;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuditLogSearchIntegrationTests {

    @Autowired
    private AuditLogRepository repository;
    @Autowired
    private TenantApplicationRepository tenantApplicationRepository;

    private String appId;

    @BeforeEach
    void setUp() {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue("TEST").orElseThrow();
        appId = app.getId();
        TenantContext.set(appId);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private AuditLog log(String action, String entityType, String entityId, String username, long occurredAt) {
        AuditLog log = new AuditLog(action, entityType, entityId, username,
                "{\"code\":\"" + entityId + "\"}", "10.0.0.1");
        try {
            java.lang.reflect.Field occurredAtField = AuditLog.class.getDeclaredField("occurredAt");
            occurredAtField.setAccessible(true);
            occurredAtField.setLong(log, occurredAt);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return repository.save(log);
    }

    @Test
    void searchFiltersByEntityActionUsernameAndRange() {
        long base = System.currentTimeMillis();
        AuditLog created = log("CREATE", "EMPLOYEE", "QA-EMP-RETEST-0808", "qa-admin", base - 1000);
        log("CREATE", "CONTRACTOR", "QA-CTR-RETEST-0808", "qa-admin", base - 2000);
        log("UPDATE", "EMPLOYEE", "QA-EMP-RETEST-0808", "other-user", base - 3000);

        Page<AuditLog> result = repository.search(
                "EMPLOYEE", "CREATE", null, null, null, null, PageRequest.of(0, 20));
        assertThat(result.getContent()).extracting(AuditLog::getId).containsExactly(created.getId());

        Page<AuditLog> byUser = repository.search(
                null, null, "qa-admin", null, null, null, PageRequest.of(0, 20));
        assertThat(byUser.getTotalElements()).isEqualTo(2);

        Page<AuditLog> byCodeSearch = repository.search(
                null, null, null, "QA-CTR-RETEST-0808", null, null, PageRequest.of(0, 20));
        assertThat(byCodeSearch.getTotalElements()).isEqualTo(1);

        Page<AuditLog> byRange = repository.search(
                null, null, null, null, base - 1500, base - 500, PageRequest.of(0, 20));
        assertThat(byRange.getTotalElements()).isEqualTo(1);

        Page<AuditLog> noMatch = repository.search(
                "WORKER", null, null, null, null, null, PageRequest.of(0, 20));
        assertThat(noMatch.getTotalElements()).isZero();
    }
}
