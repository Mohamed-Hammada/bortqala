package com.bemo.hr.employee.application;

import com.bemo.hr.employee.api.EmployeeApi;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeAssignmentRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class HrConfigurationEmployeeTests {
    private final HrConfigurationService hrConfigurationService;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final AppUserRepository appUserRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository employeeAssignmentRepository;
    private final TransactionTemplate tx;

    private final Map<String, List<String>> categoriesByApp = new LinkedHashMap<>();
    private final Map<String, List<String>> employeesByApp = new LinkedHashMap<>();
    private final List<String> createdUsers = new ArrayList<>();
    private final List<String> createdApps = new ArrayList<>();

    @Autowired
    HrConfigurationEmployeeTests(HrConfigurationService hrConfigurationService,
                                 AttendanceCategoryRepository attendanceCategoryRepository,
                                 TenantApplicationRepository tenantApplicationRepository,
                                 AppUserRepository appUserRepository,
                                 EmployeeRepository employeeRepository,
                                 EmployeeAssignmentRepository employeeAssignmentRepository,
                                 PlatformTransactionManager transactionManager) {
        this.hrConfigurationService = hrConfigurationService;
        this.attendanceCategoryRepository = attendanceCategoryRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.appUserRepository = appUserRepository;
        this.employeeRepository = employeeRepository;
        this.employeeAssignmentRepository = employeeAssignmentRepository;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void cleanup() {
        try {
            for (var appEntry : employeesByApp.entrySet()) {
                TenantContext.set(appEntry.getKey());
                tx.executeWithoutResult(status -> {
                    employeeAssignmentRepository.deleteByEmployeeIdIn(appEntry.getValue());
                    employeeRepository.deleteAllById(appEntry.getValue());
                });
            }
            for (var appEntry : categoriesByApp.entrySet()) {
                TenantContext.set(appEntry.getKey());
                tx.executeWithoutResult(status -> attendanceCategoryRepository.deleteAllById(appEntry.getValue()));
            }
            appUserRepository.deleteAllById(createdUsers);
            tenantApplicationRepository.deleteAllById(createdApps);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private TenantApplication app(String code) {
        var created = tenantApplicationRepository.save(new TenantApplication(code, code));
        createdApps.add(created.getId());
        return created;
    }

    private String category(TenantApplication app, String code) {
        return category(app, code, AttendanceMode.MANUAL);
    }

    private String category(TenantApplication app, String code, AttendanceMode mode) {
        TenantContext.set(app.getId());
        var categoryId = tx.execute(status -> attendanceCategoryRepository.save(
                new AttendanceCategory(code, code, 480, PayCycle.MONTHLY, mode, false, 127, true)).getId());
        categoriesByApp.computeIfAbsent(app.getId(), ignored -> new ArrayList<>()).add(categoryId);
        return categoryId;
    }

    private EmployeeApi.UpsertRequest employeeRequest(String code, String categoryId, boolean active) {
        return new EmployeeApi.UpsertRequest(code, "Test Worker", null, categoryId, EmploymentType.FIXED,
                new BigDecimal("5000"), LocalDate.of(2026, 1, 1), null, active, null);
    }

    @Test
    void rejectsDuplicateEmployeeCodeWithinTenantButAllowsSameCodeInAnotherTenant() {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        var appA = app("APP-A-" + suffix);
        var appB = app("APP-B-" + suffix);
        var categoryA = category(appA, "SEC");
        var categoryB = category(appB, "SEC");

        TenantContext.set(appA.getId());
        employeesByApp.computeIfAbsent(appA.getId(), ignored -> new ArrayList<>())
                .add(hrConfigurationService.createEmployee(employeeRequest("EMP1", categoryA, false)).id());
        assertThatThrownBy(() -> hrConfigurationService.createEmployee(employeeRequest("EMP1", categoryA, false)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("already exists");

        TenantContext.set(appB.getId());
        var inOtherTenant = hrConfigurationService.createEmployee(employeeRequest("EMP1", categoryB, false));
        employeesByApp.computeIfAbsent(appB.getId(), ignored -> new ArrayList<>()).add(inOtherTenant.id());
        assertThat(inOtherTenant.employeeCode()).isEqualTo("SEC-EMP1");
    }

    @Test
    void rejectsDuplicateDeviceUserIdForActiveEmployees() {
        var app = app("APP-DEV-" + UUID.randomUUID().toString().substring(0, 6));
        var categoryId = category(app, "BIO", AttendanceMode.BIOMETRIC);

        TenantContext.set(app.getId());
        var first = new EmployeeApi.UpsertRequest("EMP1", "Worker One", "device-1", categoryId,
                EmploymentType.FIXED, new BigDecimal("5000"), LocalDate.of(2026, 1, 1), null, true, null);
        var second = new EmployeeApi.UpsertRequest("EMP2", "Worker Two", "device-1", categoryId,
                EmploymentType.FIXED, new BigDecimal("5000"), LocalDate.of(2026, 1, 1), null, true, null);
        employeesByApp.computeIfAbsent(app.getId(), ignored -> new ArrayList<>())
                .add(hrConfigurationService.createEmployee(first).id());
        assertThatThrownBy(() -> hrConfigurationService.createEmployee(second))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("already mapped");
    }

    @Test
    void recordsAssignmentHistoryOnCreateCategoryMoveAndDeactivate() {
        var app = app("APP-ASSIGN-" + UUID.randomUUID().toString().substring(0, 4));
        var categoryA = category(app, "CAT_A");
        var categoryB = category(app, "CAT_B");

        TenantContext.set(app.getId());
        var created = hrConfigurationService.createEmployee(employeeRequest("EMP1", categoryA, true));
        employeesByApp.computeIfAbsent(app.getId(), ignored -> new ArrayList<>()).add(created.id());
        var history = hrConfigurationService.getEmployeeAssignments(created.id());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).categoryId()).isEqualTo(categoryA);
        assertThat(history.get(0).effectiveTo()).isNull();

        var moved = new EmployeeApi.UpsertRequest("EMP1", "Test Worker", null, categoryB, EmploymentType.FIXED,
                new BigDecimal("5000"), LocalDate.of(2026, 3, 1), null, true, created.version());
        var updated = hrConfigurationService.updateEmployee(created.id(), moved);

        var afterMove = hrConfigurationService.getEmployeeAssignments(updated.id());
        assertThat(afterMove).hasSize(2);
        assertThat(afterMove.get(1).categoryId()).isEqualTo(categoryA);
        assertThat(afterMove.get(1).effectiveTo()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(afterMove.get(0).categoryId()).isEqualTo(categoryB);
        assertThat(afterMove.get(0).effectiveFrom()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(afterMove.get(0).effectiveTo()).isNull();

        hrConfigurationService.deactivateEmployee(updated.id());
        var afterDeactivate = hrConfigurationService.getEmployeeAssignments(updated.id());
        assertThat(afterDeactivate).hasSize(2);
        assertThat(afterDeactivate.get(0).effectiveTo()).isEqualTo(LocalDate.now());
    }

    @Test
    void masksBaseSalaryForUsersWithoutCanViewSalary() {
        var testApp = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue("TEST").orElseThrow();
        TenantContext.set(testApp.getId());
        var categoryId = tx.execute(status -> attendanceCategoryRepository.save(
                new AttendanceCategory("SALARY_TEST_" + UUID.randomUUID().toString().substring(0, 4), "Salary Test",
                        480, PayCycle.MONTHLY, AttendanceMode.BIOMETRIC, false, 127, true)).getId());
        categoriesByApp.computeIfAbsent(testApp.getId(), ignored -> new ArrayList<>()).add(categoryId);
        var created = hrConfigurationService.createEmployee(employeeRequest("SAL1", categoryId, false));
        employeesByApp.computeIfAbsent(testApp.getId(), ignored -> new ArrayList<>()).add(created.id());

        var viewer = appUserRepository.save(new AppUser(testApp.getId(), "salary-viewer", "Salary Viewer",
                "x", Set.of(), Set.of(), true, true));
        var hidden = appUserRepository.save(new AppUser(testApp.getId(), "salary-hidden", "Salary Hidden",
                "x", Set.of(), Set.of(), false, true));
        createdUsers.add(viewer.getId());
        createdUsers.add(hidden.getId());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(hidden.getUsername(), "x", List.of()));
        var masked = hrConfigurationService.listEmployees().stream()
                .filter(employee -> employee.id().equals(created.id())).findFirst().orElseThrow();
        assertThat(masked.baseSalary()).isNull();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(viewer.getUsername(), "x", List.of()));
        var visible = hrConfigurationService.listEmployees().stream()
                .filter(employee -> employee.id().equals(created.id())).findFirst().orElseThrow();
        assertThat(visible.baseSalary()).isEqualByComparingTo(new BigDecimal("5000"));
    }
}
