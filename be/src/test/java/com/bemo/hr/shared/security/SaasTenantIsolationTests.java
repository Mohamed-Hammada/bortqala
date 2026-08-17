package com.bemo.hr.shared.security;

import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SaasTenantIsolationTests {
    private final TenantApplicationRepository tenantApplicationRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    SaasTenantIsolationTests(TenantApplicationRepository tenantApplicationRepository,
                             AttendanceCategoryRepository attendanceCategoryRepository,
                             PlatformTransactionManager transactionManager) {
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.attendanceCategoryRepository = attendanceCategoryRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void tenantIdIsAssignedOnInsertAndFiltersEveryJpaQuery() {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var appA = tenantApplicationRepository.save(new TenantApplication("APP-A-" + suffix, "App A"));
        var appB = tenantApplicationRepository.save(new TenantApplication("APP-B-" + suffix, "App B"));

        TenantContext.set(appA.getId());
        var categoryId = transactionTemplate.execute(status -> attendanceCategoryRepository.save(
                new AttendanceCategory("SECURITY", "الأمن والحراسة", 720, PayCycle.MONTHLY,
                        AttendanceMode.BIOMETRIC, true, 127, true)).getId());
        TenantContext.clear();

        TenantContext.set(appB.getId());
        var appBRows = transactionTemplate.execute(status -> attendanceCategoryRepository.findAll());
        var appBLookupById = transactionTemplate.execute(
                status -> attendanceCategoryRepository.findById(categoryId));
        TenantContext.clear();

        TenantContext.set(appA.getId());
        var appARows = transactionTemplate.execute(status -> attendanceCategoryRepository.findAll());

        assertThat(appBRows).isEmpty();
        assertThat(appBLookupById).isEmpty();
        assertThat(appARows).extracting(AttendanceCategory::getCode).containsExactly("SECURITY");
        assertThat(appARows).extracting(AttendanceCategory::getName).containsExactly("الأمن والحراسة");
    }
}
