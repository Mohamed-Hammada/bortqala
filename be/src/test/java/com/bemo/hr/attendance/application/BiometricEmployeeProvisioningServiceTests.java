package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.domain.BiometricSource;
import com.bemo.hr.attendance.infrastructure.BiometricSourceRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.domain.*;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class BiometricEmployeeProvisioningServiceTests {

    private final List<String> createdApps = new ArrayList<>();
    private final List<String> createdCategories = new ArrayList<>();
    private final List<String> createdSources = new ArrayList<>();
    private final List<String> createdEmployees = new ArrayList<>();
    @Autowired
    private BiometricEmployeeProvisioningService provisioningService;
    @Autowired
    private AttendanceCategoryRepository attendanceCategoryRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private PunchRecordRepository punchRecordRepository;
    @Autowired
    private BiometricSourceRepository biometricSourceRepository;
    @Autowired
    private TenantApplicationRepository tenantApplicationRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void cleanup() {
        try {
            String app = createdApps.isEmpty() ? null : createdApps.get(createdApps.size() - 1);
            if (app != null) {
                TenantContext.set(app);
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    employeeRepository.deleteAllById(createdEmployees);
                    biometricSourceRepository.deleteAllById(createdSources);
                    if (!createdCategories.isEmpty()) {
                        entityManager.createNativeQuery("DELETE FROM employee_code_sequences WHERE category_id IN (:ids)")
                                .setParameter("ids", createdCategories)
                                .executeUpdate();
                    }
                    attendanceCategoryRepository.deleteAllById(createdCategories);
                });
            }
            tenantApplicationRepository.deleteAllById(createdApps);
        } finally {
            TenantContext.clear();
        }
    }

    private TenantApplication app() {
        var created = tenantApplicationRepository.save(
                new TenantApplication("APP-PROV-" + UUID.randomUUID().toString().substring(0, 6),
                        "Provisioning Test"));
        createdApps.add(created.getId());
        return created;
    }

    private AttendanceCategory category(String code, String name, CategoryScope scope, boolean active) {
        var cat = new AttendanceCategory(code, name, 480, PayCycle.THIRTY_DAYS, AttendanceMode.BIOMETRIC,
                false, 111, active, scope);
        var saved = attendanceCategoryRepository.save(cat);
        createdCategories.add(saved.getId());
        return saved;
    }

    @Test
    void configureSource_withValidCategory_succeeds() {
        var app = app();
        TenantContext.set(app.getId());
        var cat = category("EMP_CAT", "موظفو الإدارة", CategoryScope.EMPLOYEE, true);
        var source = new BiometricSource(BiometricSource.SourceType.FILE_DEVICE, "Gate 1", "gate_1");

        provisioningService.configureSource(source, true, cat.getId(), "FIXED", "FIRST_PUNCH", true);

        assertThat(source.isAutoCreateEmployees()).isTrue();
        assertThat(source.getAutoCreateCategoryId()).isEqualTo(cat.getId());
        assertThat(source.getAutoCreateEmploymentType()).isEqualTo("FIXED");
        assertThat(source.getAutoCreateActiveFromMode()).isEqualTo("FIRST_PUNCH");
        assertThat(source.isAutoCreateEmployeeActive()).isTrue();
    }

    @Test
    void configureSource_withInactiveCategory_throwsException() {
        var app = app();
        TenantContext.set(app.getId());
        var cat = category("INACT_CAT", "فئة ملغاة", CategoryScope.EMPLOYEE, false);
        var source = new BiometricSource(BiometricSource.SourceType.FILE_DEVICE, "Gate 1", "gate_1");

        assertThatThrownBy(() -> provisioningService.configureSource(source, true, cat.getId(), "FIXED", "FIRST_PUNCH", true))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "BIO_AUTO_EMPLOYEE_CATEGORY_INACTIVE");
    }

    @Test
    void configureSource_withWorkerScopeCategory_throwsException() {
        var app = app();
        TenantContext.set(app.getId());
        var cat = category("WRK_CAT", "عمالة اليومية", CategoryScope.WORKER, true);
        var source = new BiometricSource(BiometricSource.SourceType.FILE_DEVICE, "Gate 1", "gate_1");

        assertThatThrownBy(() -> provisioningService.configureSource(source, true, cat.getId(), "FIXED", "FIRST_PUNCH", true))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "BIO_AUTO_EMPLOYEE_CATEGORY_SCOPE");
    }

    @Test
    void configureSource_withNoCategory_whenExistingActiveCategories_throwsException() {
        var app = app();
        TenantContext.set(app.getId());
        category("EMP_CAT", "موظفو الإدارة", CategoryScope.EMPLOYEE, true);
        var source = new BiometricSource(BiometricSource.SourceType.FILE_DEVICE, "Gate 1", "gate_1");

        assertThatThrownBy(() -> provisioningService.configureSource(source, true, null, "FIXED", "FIRST_PUNCH", true))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "BIO_AUTO_EMPLOYEE_CATEGORY_REQUIRED");
    }

    @Test
    void configureSource_withNoCategory_whenNoExistingCategories_createsDefaultCategory() {
        var app = app();
        TenantContext.set(app.getId());
        var source = new BiometricSource(BiometricSource.SourceType.FILE_DEVICE, "Gate 1", "gate_1");

        provisioningService.configureSource(source, true, null, "FIXED", "FIRST_PUNCH", true);

        assertThat(source.isAutoCreateEmployees()).isTrue();
        assertThat(source.getAutoCreateCategoryId()).isNotNull();
        var defaultCat = attendanceCategoryRepository.findById(source.getAutoCreateCategoryId()).orElseThrow();
        createdCategories.add(defaultCat.getId());
        assertThat(defaultCat.getCode()).startsWith("BIO_AUTO");
        assertThat(defaultCat.getName()).isEqualTo("Biometric Employees - Auto");
    }

    @Test
    void resolveEmployeeId_provisionsNewEmployeeWhenEnabled() {
        var app = app();
        TenantContext.set(app.getId());
        var cat = category("EMP_CAT", "موظفو الإدارة", CategoryScope.EMPLOYEE, true);
        var source = new BiometricSource(BiometricSource.SourceType.FILE_DEVICE, "Gate 1", "gate_1");
        provisioningService.configureSource(source, true, cat.getId(), "FIXED", "FIRST_PUNCH", true);
        var savedSource = biometricSourceRepository.save(source);
        createdSources.add(savedSource.getId());

        String employeeId = provisioningService.resolveEmployeeId(
                savedSource, "DEV-999", "أحمد محمود", Instant.now(), "tester");

        assertThat(employeeId).isNotNull();
        createdEmployees.add(employeeId);

        var employee = employeeRepository.findById(employeeId).orElseThrow();
        assertThat(employee.getDeviceUserId()).isEqualTo("DEV-999");
        assertThat(employee.getFullName()).isEqualTo("أحمد محمود");
        assertThat(employee.getCategoryId()).isEqualTo(cat.getId());
        assertThat(employee.getEmploymentType()).isEqualTo(EmploymentType.FIXED);
        assertThat(employee.isActive()).isTrue();
    }
}
