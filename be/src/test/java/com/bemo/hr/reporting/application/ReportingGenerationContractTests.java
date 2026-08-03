package com.bemo.hr.reporting.application;

import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.domain.ScheduleRule;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import com.bemo.hr.reporting.api.ReportingApi;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyResultSnapshotRepository;
import com.bemo.hr.reporting.infrastructure.HolidayProposalRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReportingGenerationContractTests {
    private final ReportingService reportingService;
    private final AttendanceReportRepository attendanceReportRepository;
    private final DailyAttendanceResultRepository dailyAttendanceResultRepository;
    private final HolidayProposalRepository holidayProposalRepository;
    private final DayAnomalyRepository dayAnomalyRepository;
    private final DayAnomalyResultSnapshotRepository dayAnomalyResultSnapshotRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final EmployeeRepository employeeRepository;
    private final ScheduleRuleRepository scheduleRuleRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final TransactionTemplate tx;

    private final List<String> createdApps = new ArrayList<>();
    private final List<String> createdReports = new ArrayList<>();
    private final List<String> createdEmployees = new ArrayList<>();
    private final List<String> createdCategories = new ArrayList<>();
    private final List<String> createdSchedules = new ArrayList<>();

    @Autowired
    ReportingGenerationContractTests(ReportingService reportingService,
                                     AttendanceReportRepository attendanceReportRepository,
                                     DailyAttendanceResultRepository dailyAttendanceResultRepository,
                                     HolidayProposalRepository holidayProposalRepository,
                                     DayAnomalyRepository dayAnomalyRepository,
                                     DayAnomalyResultSnapshotRepository dayAnomalyResultSnapshotRepository,
                                     AttendanceCategoryRepository attendanceCategoryRepository,
                                     EmployeeRepository employeeRepository,
                                     ScheduleRuleRepository scheduleRuleRepository,
                                     TenantApplicationRepository tenantApplicationRepository,
                                     PlatformTransactionManager transactionManager) {
        this.reportingService = reportingService;
        this.attendanceReportRepository = attendanceReportRepository;
        this.dailyAttendanceResultRepository = dailyAttendanceResultRepository;
        this.holidayProposalRepository = holidayProposalRepository;
        this.dayAnomalyRepository = dayAnomalyRepository;
        this.dayAnomalyResultSnapshotRepository = dayAnomalyResultSnapshotRepository;
        this.attendanceCategoryRepository = attendanceCategoryRepository;
        this.employeeRepository = employeeRepository;
        this.scheduleRuleRepository = scheduleRuleRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void cleanup() {
        try {
            String app = createdApps.isEmpty() ? null : createdApps.get(createdApps.size() - 1);
            if (app != null) {
                TenantContext.set(app);
                tx.executeWithoutResult(status -> {
                    for (String reportId : createdReports) {
                        dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(reportId)
                                .forEach(item -> dailyAttendanceResultRepository.deleteById(item.getId()));
                        holidayProposalRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(reportId)
                                .forEach(item -> holidayProposalRepository.deleteById(item.getId()));
                        var anomalies = dayAnomalyRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(reportId);
                        for (var anomaly : anomalies) {
                            var snapshots = dayAnomalyResultSnapshotRepository.findByAnomalyId(anomaly.getId());
                            dayAnomalyResultSnapshotRepository.deleteAll(snapshots);
                        }
                        dayAnomalyRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(reportId)
                                .forEach(item -> dayAnomalyRepository.deleteById(item.getId()));
                    }
                    attendanceReportRepository.deleteAllById(createdReports);
                    scheduleRuleRepository.deleteAllById(createdSchedules);
                    employeeRepository.deleteAllById(createdEmployees);
                    attendanceCategoryRepository.deleteAllById(createdCategories);
                });
            }
            tenantApplicationRepository.deleteAllById(createdApps);
        } finally {
            TenantContext.clear();
        }
    }

    private String app() {
        var created = tenantApplicationRepository.save(
                new TenantApplication("APP-RPT-" + UUID.randomUUID().toString().substring(0, 6), "Report Test"));
        createdApps.add(created.getId());
        return created.getId();
    }

    private AttendanceCategory category() {
        var saved = attendanceCategoryRepository.save(new AttendanceCategory("RPT", "Report Category", 480,
                PayCycle.MONTHLY, AttendanceMode.MANUAL, false, 127, true));
        createdCategories.add(saved.getId());
        return saved;
    }

    private Employee employee(String categoryId) {
        var saved = employeeRepository.save(new Employee("RPT-E1", "Report Tester", null, categoryId,
                EmploymentType.FIXED, LocalDate.of(2026, 1, 1), null, true));
        createdEmployees.add(saved.getId());
        return saved;
    }

    private ScheduleRule schedule(String categoryId) {
        var saved = scheduleRuleRepository.save(new ScheduleRule(categoryId, "Standard",
                LocalDate.of(2025, 1, 1), null, LocalTime.of(8, 0), 480, 15));
        createdSchedules.add(saved.getId());
        return saved;
    }

    @Test
    void previewReportsScopeWithoutPersistingAnything() {
        String app = app();
        TenantContext.set(app);
        var category = category();
        employee(category.getId());
        schedule(category.getId());

        var preview = reportingService.preview(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5), PayCycle.MONTHLY);

        assertThat(preview.employeeCount()).isEqualTo(1);
        assertThat(preview.workdays()).isEqualTo(5);
        assertThat(preview.scheduleCoverageCount()).isEqualTo(5);
        assertThat(preview.categories()).singleElement()
                .satisfies(c -> assertThat(c.categoryName()).isEqualTo("Report Category"));
        assertThat(preview.existingReportId()).isNull();
        assertThat(preview.overlappingReportIds()).isEmpty();
        assertThat(attendanceReportRepository.count()).isZero();
    }

    @Test
    void duplicateGenerationReplaysTheSameReportInsteadOfCreatingAnother() {
        String app = app();
        TenantContext.set(app);
        var category = category();
        employee(category.getId());

        var first = reportingService.create(
                new ReportingApi.CreateRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), PayCycle.MONTHLY),
                "tester");
        createdReports.add(first.report().id());
        assertThat(first.report().status().name()).isEqualTo("IN_REVIEW");
        assertThat(first.report().generationHash()).hasSize(64);

        var replay = reportingService.create(
                new ReportingApi.CreateRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), PayCycle.MONTHLY),
                "tester");

        assertThat(replay.report().id()).isEqualTo(first.report().id());
        assertThat(attendanceReportRepository.count()).isEqualTo(1);
        assertThat(dailyAttendanceResultRepository.countByReportId(first.report().id()))
                .isEqualTo(dailyAttendanceResultRepository.countByReportId(replay.report().id()));
    }

    @Test
    void eachReportCapturesTheConfigurationPolicyVersionAtGenerationTime() {
        String app = app();
        TenantContext.set(app);
        var category = category();
        employee(category.getId());

        var january = reportingService.create(
                new ReportingApi.CreateRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), PayCycle.MONTHLY),
                "tester");
        createdReports.add(january.report().id());
        String policyBefore = attendanceReportRepository.findById(january.report().id()).orElseThrow().getConfigurationVersion();

        category.update("RPT", "Report Category Updated", 480, PayCycle.MONTHLY, AttendanceMode.MANUAL,
                false, 127, true);
        attendanceCategoryRepository.saveAndFlush(category);

        var february = reportingService.create(
                new ReportingApi.CreateRequest(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), PayCycle.MONTHLY),
                "tester");
        createdReports.add(february.report().id());

        String policyAfter = attendanceReportRepository.findById(february.report().id()).orElseThrow().getConfigurationVersion();
        assertThat(policyAfter).isNotEqualTo(policyBefore);
        assertThat(february.report().generationHash()).isNotEqualTo(january.report().generationHash());
    }
}
