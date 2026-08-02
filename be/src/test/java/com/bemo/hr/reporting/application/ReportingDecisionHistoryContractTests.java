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
import com.bemo.hr.reporting.domain.AttendanceDecision;
import com.bemo.hr.reporting.domain.AttendanceReportDecision;
import com.bemo.hr.reporting.domain.DailyAttendanceResult;
import com.bemo.hr.reporting.infrastructure.AttendanceReportDecisionRepository;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyResultSnapshotRepository;
import com.bemo.hr.reporting.infrastructure.HolidayProposalRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ReportingDecisionHistoryContractTests {
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
    private final AttendanceReportDecisionRepository attendanceReportDecisionRepository;
    private final TransactionTemplate tx;

    private final List<String> createdApps = new ArrayList<>();
    private final List<String> createdReports = new ArrayList<>();
    private final List<String> createdEmployees = new ArrayList<>();
    private final List<String> createdCategories = new ArrayList<>();
    private final List<String> createdSchedules = new ArrayList<>();

    @Autowired
    ReportingDecisionHistoryContractTests(ReportingService reportingService,
                                          AttendanceReportRepository attendanceReportRepository,
                                          DailyAttendanceResultRepository dailyAttendanceResultRepository,
                                          HolidayProposalRepository holidayProposalRepository,
                                          DayAnomalyRepository dayAnomalyRepository,
                                          DayAnomalyResultSnapshotRepository dayAnomalyResultSnapshotRepository,
                                          AttendanceCategoryRepository attendanceCategoryRepository,
                                          EmployeeRepository employeeRepository,
                                          ScheduleRuleRepository scheduleRuleRepository,
                                          TenantApplicationRepository tenantApplicationRepository,
                                          AttendanceReportDecisionRepository attendanceReportDecisionRepository,
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
        this.attendanceReportDecisionRepository = attendanceReportDecisionRepository;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void cleanup() {
        try {
            String app = createdApps.isEmpty() ? null : createdApps.getLast();
            if (app != null) {
                TenantContext.set(app);
                tx.executeWithoutResult(status -> {
                    for (String reportId : createdReports) {
                        attendanceReportDecisionRepository.deleteAll(
                                attendanceReportDecisionRepository.findByReportIdOrderByCreatedAtAsc(reportId));
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
                new TenantApplication("APP-RDH-" + UUID.randomUUID().toString().substring(0, 6), "Decision History Test"));
        createdApps.add(created.getId());
        return created.getId();
    }

    private ReportingApi.Details generatedReport() {
        var category = attendanceCategoryRepository.save(new AttendanceCategory("RDH", "Decision Category", 480,
                PayCycle.MONTHLY, AttendanceMode.MANUAL, false, 127, true));
        createdCategories.add(category.getId());
        var employee = employeeRepository.save(new Employee("RDH-E1", "Decision Tester", null, category.getId(),
                EmploymentType.FIXED, LocalDate.of(2026, 1, 1), null, true));
        createdEmployees.add(employee.getId());
        scheduleRuleRepository.save(new ScheduleRule(category.getId(), "Standard",
                LocalDate.of(2025, 1, 1), null, LocalTime.of(8, 0), 480, 15));
        var details = reportingService.create(
                new ReportingApi.CreateRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), PayCycle.MONTHLY),
                "tester");
        createdReports.add(details.report().id());
        return details;
    }

    private ReportingApi.DailyResult blockingRow(ReportingApi.Details details) {
        return details.dailyResults().stream().filter(row -> row.decision() == null).findFirst()
                .orElseThrow(() -> new AssertionError("Expected a blocking row"));
    }

    @Test
    void eachDecisionAppendsAnImmutableHistoryRowAndDecisionIsReversible() {
        String app = app();
        TenantContext.set(app);
        ReportingApi.Details generated = generatedReport();
        String reportId = generated.report().id();
        var row = blockingRow(generated);

        var first = reportingService.decideDaily(reportId, row.id(),
                new ReportingApi.DecisionRequest(AttendanceDecision.ABSENCE, 0, "first pass", row.version()), "reviewer1");
        assertThat(first.allowedActions()).contains("DECISION", "BULK_DECISION", "APPROVE", "EXPORT");

        var historyAfterFirst = reportingService.decisionHistory(reportId);
        assertThat(historyAfterFirst).hasSize(1);
        assertThat(historyAfterFirst.getFirst().operation()).isEqualTo("DECIDE");
        assertThat(historyAfterFirst.getFirst().previousDecision()).isNull();
        assertThat(historyAfterFirst.getFirst().newDecision()).isEqualTo(AttendanceDecision.ABSENCE);
        assertThat(historyAfterFirst.getFirst().actor()).isEqualTo("reviewer1");

        var updated = reportingService.get(reportId).dailyResults().stream()
                .filter(item -> item.id().equals(row.id())).findFirst().orElseThrow();
        assertThat(updated.decision()).isEqualTo(AttendanceDecision.ABSENCE);
        assertThat(updated.version()).isEqualTo(row.version() + 1);

        var reversed = reportingService.decideDaily(reportId, row.id(),
                new ReportingApi.DecisionRequest(AttendanceDecision.NORMAL_DAY, 480, "corrected", updated.version()), "reviewer2");

        var historyAfterSecond = reportingService.decisionHistory(reportId);
        assertThat(historyAfterSecond).hasSize(2);
        assertThat(historyAfterSecond.get(1).previousDecision()).isEqualTo(AttendanceDecision.ABSENCE);
        assertThat(historyAfterSecond.get(1).newDecision()).isEqualTo(AttendanceDecision.NORMAL_DAY);
        assertThat(reversed.dailyResults().stream().filter(item -> item.id().equals(row.id())).findFirst()
                .orElseThrow().decision()).isEqualTo(AttendanceDecision.NORMAL_DAY);
    }

    @Test
    void staleReviewerIsRejectedAndCanRetryAfterReload() {
        String app = app();
        TenantContext.set(app);
        ReportingApi.Details generated = generatedReport();
        String reportId = generated.report().id();
        var row = blockingRow(generated);

        reportingService.decideDaily(reportId, row.id(),
                new ReportingApi.DecisionRequest(AttendanceDecision.ABSENCE, 0, null, row.version()), "reviewerB");

        assertThatThrownBy(() -> reportingService.decideDaily(reportId, row.id(),
                new ReportingApi.DecisionRequest(AttendanceDecision.NORMAL_DAY, 480, null, row.version()), "reviewerA"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("مراجع آخر");

        var reloaded = reportingService.get(reportId).dailyResults().stream()
                .filter(item -> item.id().equals(row.id())).findFirst().orElseThrow();
        var retried = reportingService.decideDaily(reportId, row.id(),
                new ReportingApi.DecisionRequest(AttendanceDecision.NORMAL_DAY, 480, null, reloaded.version()), "reviewerA");
        assertThat(retried.dailyResults().stream().filter(item -> item.id().equals(row.id())).findFirst()
                .orElseThrow().decision()).isEqualTo(AttendanceDecision.NORMAL_DAY);
        assertThat(reportingService.decisionHistory(reportId)).hasSize(2);
    }

    @Test
    void operationIdIsUniquePerTenantPerAction() {
        String app = app();
        TenantContext.set(app);
        ReportingApi.Details generated = generatedReport();
        String reportId = generated.report().id();
        var row = blockingRow(generated);
        var resultId = row.id();

        tx.executeWithoutResult(status -> {
            var result = dailyAttendanceResultRepository.findById(resultId).orElseThrow();
            var next = new DailyAttendanceResult.DecisionState(AttendanceDecision.ABSENCE, 0, "op", "tester", Instant.now());
            attendanceReportDecisionRepository.saveAndFlush(new AttendanceReportDecision(
                    reportId, resultId, "OP-1", "DECIDE", result.decisionState(), next, "tester"));
        });
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            var result = dailyAttendanceResultRepository.findById(resultId).orElseThrow();
            var next = new DailyAttendanceResult.DecisionState(AttendanceDecision.NORMAL_DAY, 480, "op", "tester", Instant.now());
            attendanceReportDecisionRepository.saveAndFlush(new AttendanceReportDecision(
                    reportId, resultId, "OP-1", "DECIDE", result.decisionState(), next, "tester"));
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowedActionsFollowTheReportLifecycle() {
        String app = app();
        TenantContext.set(app);
        ReportingApi.Details generated = generatedReport();
        String reportId = generated.report().id();
        var row = blockingRow(generated);

        assertThat(generated.allowedActions())
                .contains("DECISION", "BULK_DECISION", "DOWNTIME_DECISION", "DAY_ANOMALY", "HOLIDAY_DECISION", "APPROVE", "EXPORT");

        reportingService.decideDaily(reportId, row.id(),
                new ReportingApi.DecisionRequest(AttendanceDecision.NORMAL_DAY, 480, null, row.version()), "tester");
        reportingService.approve(reportId, "manager");

        var approved = reportingService.get(reportId);
        assertThat(approved.allowedActions()).contains("REOPEN", "EXPORT").doesNotContain("DECISION", "APPROVE");
    }
}
