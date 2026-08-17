package com.bemo.hr.employee.application;

import com.bemo.hr.audit.domain.AuditLog;
import com.bemo.hr.audit.infrastructure.AuditLogRepository;
import com.bemo.hr.employee.api.EmployeeApi;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.reporting.domain.DailyAttendanceResult;
import com.bemo.hr.reporting.domain.DailyStatus;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EmployeeCodeDedupServiceTests {

    private final EmployeeCodeDedupService service;
    private final HrConfigurationService hrConfigurationService;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceReportRepository attendanceReportRepository;
    private final DailyAttendanceResultRepository dailyAttendanceResultRepository;
    private final AuditLogRepository auditLogRepository;
    private final TransactionTemplate tx;

    private final Map<String, List<String>> employeesByApp = new java.util.LinkedHashMap<>();
    private final List<String> reportsByApp = new ArrayList<>();
    private final List<String> createdApps = new ArrayList<>();

    @Autowired
    EmployeeCodeDedupServiceTests(EmployeeCodeDedupService service,
                                  HrConfigurationService hrConfigurationService,
                                  AttendanceCategoryRepository attendanceCategoryRepository,
                                  TenantApplicationRepository tenantApplicationRepository,
                                  EmployeeRepository employeeRepository,
                                  AttendanceReportRepository attendanceReportRepository,
                                  DailyAttendanceResultRepository dailyAttendanceResultRepository,
                                  AuditLogRepository auditLogRepository,
                                  PlatformTransactionManager transactionManager) {
        this.service = service;
        this.hrConfigurationService = hrConfigurationService;
        this.attendanceCategoryRepository = attendanceCategoryRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceReportRepository = attendanceReportRepository;
        this.dailyAttendanceResultRepository = dailyAttendanceResultRepository;
        this.auditLogRepository = auditLogRepository;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void cleanup() {
        try {
            for (var entry : employeesByApp.entrySet()) {
                TenantContext.set(entry.getKey());
                tx.executeWithoutResult(status -> {
                    dailyAttendanceResultRepository.deleteAll();
                    attendanceReportRepository.deleteAllById(reportsByApp);
                    employeeRepository.deleteAllById(entry.getValue());
                });
            }
            for (var appId : employeesByApp.keySet()) {
                TenantContext.set(appId);
                tx.executeWithoutResult(status -> attendanceCategoryRepository.deleteAll());
            }
            tenantApplicationRepository.deleteAllById(createdApps);
        } finally {
            TenantContext.clear();
        }
    }

    private TenantApplication app(String code) {
        var created = tenantApplicationRepository.save(new TenantApplication(code, code));
        createdApps.add(created.getId());
        return created;
    }

    private String category(TenantApplication app) {
        TenantContext.set(app.getId());
        return tx.execute(status -> attendanceCategoryRepository.save(
                new AttendanceCategory("SEC-" + UUID.randomUUID().toString().substring(0, 4), "Security",
                        480, PayCycle.MONTHLY, AttendanceMode.MANUAL, false, 127, true)).getId());
    }

    private String employee(TenantApplication app, String code, String categoryId) {
        TenantContext.set(app.getId());
        var created = hrConfigurationService.createEmployee(new EmployeeApi.UpsertRequest(
                code, "Test Employee", null, categoryId, EmploymentType.FIXED,
                new BigDecimal("5000"), LocalDate.of(2026, 1, 1), null, false, null));
        employeesByApp.computeIfAbsent(app.getId(), ignored -> new ArrayList<>()).add(created.id());
        return created.id();
    }

    private void seedDailyResult(TenantApplication app, String employeeId, String employeeCode) {
        TenantContext.set(app.getId());
        var report = new AttendanceReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15),
                PayCycle.HALF_MONTHLY, "v1", "test");
        tx.executeWithoutResult(status -> {
            attendanceReportRepository.save(report);
            dailyAttendanceResultRepository.save(new DailyAttendanceResult(
                    report.getId(), employeeId, "cat", LocalDate.of(2026, 8, 5), employeeCode,
                    "Test Employee", "Security", Instant.now(), Instant.now().plusSeconds(28800),
                    1, 480, 480, 0, 0, 0, DailyStatus.PRESENT, null, "v1"));
        });
        reportsByApp.add(report.getId());
    }

    private List<AuditLog> corrections() {
        return auditLogRepository
                .findByEntityTypeOrderByOccurredAtDesc("EMPLOYEE", PageRequest.of(0, 50))
                .getContent().stream()
                .filter(log -> "CODE_CORRECTION".equals(log.getAction()))
                .toList();
    }

    @Test
    void detectsOnlyTheCanonicalRepeatedPattern() {
        assertThat(EmployeeCodeDedupService.duplicatedCanonical("QA-EMP-0807-QA-EMP-0807"))
                .contains("QA-EMP-0807");
        assertThat(EmployeeCodeDedupService.duplicatedCanonical("QA-EMP-0807")).isEmpty();
        assertThat(EmployeeCodeDedupService.duplicatedCanonical("QA-EMP-0807-QA-EMP-0808")).isEmpty();
        assertThat(EmployeeCodeDedupService.duplicatedCanonical("AB-AB")).isEmpty();
        assertThat(EmployeeCodeDedupService.duplicatedCanonical(null)).isEmpty();
        assertThat(EmployeeCodeDedupService.duplicatedCanonical("X")).isEmpty();
    }

    @Test
    void dryRunReportsCorrectionsWithoutApplying() {
        var app = app("APP-DRY-" + UUID.randomUUID().toString().substring(0, 6));
        var categoryId = category(app);
        var employeeId = employee(app, "QA-EMP-0807-QA-EMP-0807", categoryId);

        TenantContext.set(app.getId());
        var report = service.correct(true, "qa-admin");

        assertThat(report.correctedCount()).isZero();
        assertThat(report.items()).hasSize(1);
        assertThat(report.items().get(0).oldCode()).isEqualTo("QA-EMP-0807-QA-EMP-0807");
        assertThat(report.items().get(0).newCode()).isEqualTo("QA-EMP-0807");

        assertThat(employeeRepository.findById(employeeId).orElseThrow().getEmployeeCode())
                .isEqualTo("QA-EMP-0807-QA-EMP-0807");
        assertThat(corrections()).isEmpty();
    }

    @Test
    void applyNormalizesCodeSyncsSnapshotsAndRecordsAudit() {
        var app = app("APP-APP-" + UUID.randomUUID().toString().substring(0, 6));
        var categoryId = category(app);
        var employeeId = employee(app, "QA-EMP-0807-QA-EMP-0807", categoryId);
        seedDailyResult(app, employeeId, "QA-EMP-0807-QA-EMP-0807");

        TenantContext.set(app.getId());
        var report = service.correct(false, "qa-admin");

        assertThat(report.correctedCount()).isEqualTo(1);
        assertThat(employeeRepository.findById(employeeId).orElseThrow().getEmployeeCode())
                .isEqualTo("QA-EMP-0807");

        var daily = dailyAttendanceResultRepository.findAll();
        assertThat(daily).hasSize(1);
        assertThat(daily.get(0).getEmployeeCode()).isEqualTo("QA-EMP-0807");

        var audit = corrections();
        assertThat(audit).hasSize(1);
        assertThat(audit.get(0).getUsername()).isEqualTo("qa-admin");
        assertThat(audit.get(0).getDetailsJson()).contains("QA-EMP-0807-QA-EMP-0807")
                .contains("QA-EMP-0807");
    }

    @Test
    void conflictingCanonicalReceivesASuffixedCode() {
        var app = app("APP-CON-" + UUID.randomUUID().toString().substring(0, 6));
        var categoryId = category(app);
        employee(app, "QA-EMP-0807", categoryId);
        var duplicatedId = employee(app, "QA-EMP-0807-QA-EMP-0807", categoryId);

        TenantContext.set(app.getId());
        var report = service.correct(false, "qa-admin");

        assertThat(report.correctedCount()).isEqualTo(1);
        var item = report.items().get(0);
        assertThat(item.conflict()).isTrue();
        assertThat(item.newCode()).startsWith("QA-EMP-0807-");
        assertThat(employeeRepository.findById(duplicatedId).orElseThrow().getEmployeeCode())
                .isEqualTo(item.newCode());
    }

    @Test
    void applyIsIdempotentAndSuffixedConflictResolutionIsStable() {
        var app = app("APP-IDM-" + UUID.randomUUID().toString().substring(0, 6));
        var categoryId = category(app);
        employee(app, "QA-EMP-0807", categoryId);
        var duplicatedId = employee(app, "QA-EMP-0807-QA-EMP-0807", categoryId);

        TenantContext.set(app.getId());
        var first = service.correct(false, "qa-admin");
        var second = service.correct(false, "qa-admin");

        assertThat(first.correctedCount()).isEqualTo(1);
        assertThat(second.correctedCount()).isZero();
        assertThat(second.items()).isEmpty();
        assertThat(employeeRepository.findById(duplicatedId).orElseThrow().getEmployeeCode())
                .isEqualTo(first.items().get(0).newCode());
    }
}
