package com.bemo.hr.reporting.application;

import com.bemo.hr.PostgresIntegrationTest;
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
import com.bemo.hr.reporting.infrastructure.AttendanceReportDecisionRepository;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyResultSnapshotRepository;
import com.bemo.hr.reporting.infrastructure.HolidayProposalRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.infrastructure.IdempotencyKeyRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReportingBulkDecisionConcurrencyTests extends PostgresIntegrationTest {

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
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final TransactionTemplate tx;

    private final List<String> createdApps = new ArrayList<>();
    private final List<String> createdReports = new ArrayList<>();
    private final List<String> createdEmployees = new ArrayList<>();
    private final List<String> createdCategories = new ArrayList<>();
    private final List<String> createdSchedules = new ArrayList<>();
    private final List<String> createdOperationIds = new ArrayList<>();

    @Autowired
    ReportingBulkDecisionConcurrencyTests(ReportingService reportingService,
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
                                          IdempotencyKeyRepository idempotencyKeyRepository,
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
        this.idempotencyKeyRepository = idempotencyKeyRepository;
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
                        attendanceReportDecisionRepository.deleteAll(
                                attendanceReportDecisionRepository.findByReportIdOrderByCreatedAtAsc(reportId));
                        dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(reportId)
                                .forEach(item -> dailyAttendanceResultRepository.deleteById(item.getId()));
                        holidayProposalRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(reportId)
                                .forEach(item -> holidayProposalRepository.deleteById(item.getId()));
                        var anomalies = dayAnomalyRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(reportId);
                        for (var anomaly : anomalies) {
                            dayAnomalyResultSnapshotRepository.deleteAll(
                                    dayAnomalyResultSnapshotRepository.findByAnomalyId(anomaly.getId()));
                        }
                        dayAnomalyRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(reportId)
                                .forEach(item -> dayAnomalyRepository.deleteById(item.getId()));
                    }
                    attendanceReportRepository.deleteAllById(createdReports);
                    createdOperationIds.forEach(operationId ->
                            idempotencyKeyRepository.findByOperationTypeAndOperationId("ATTENDANCE_BULK_DECISION", operationId)
                                    .ifPresent(key -> idempotencyKeyRepository.deleteById(key.getId())));
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
                new TenantApplication("APP-BDC-" + UUID.randomUUID().toString().substring(0, 6),
                        "Bulk Decision Concurrency Test"));
        createdApps.add(created.getId());
        return created.getId();
    }

    private ReportingApi.Details generatedReport() {
        var category = attendanceCategoryRepository.save(new AttendanceCategory("BDC", "Decision Category", 480,
                PayCycle.MONTHLY, AttendanceMode.MANUAL, false, 127, true));
        createdCategories.add(category.getId());
        var employee = employeeRepository.save(new Employee("BDC-E1", "Decision Tester", null, category.getId(),
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

    @Test
    void concurrentBulkDecideWithTheSameOperationIdAppliesExactlyOnce() throws Exception {
        String appId = app();
        TenantContext.set(appId);
        ReportingApi.Details generated = generatedReport();
        String reportId = generated.report().id();
        String statusFilter = generated.dailyResults().stream()
                .filter(row -> row.decision() == null).findFirst().orElseThrow().status().name();

        String operationId = "BULK-CONC-" + UUID.randomUUID().toString().substring(0, 8);
        createdOperationIds.add(operationId);
        var request = new ReportingApi.BulkDecisionRequest(
                AttendanceDecision.NORMAL_DAY, statusFilter, "concurrent", operationId);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger completed = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicReference<ReportingApi.BulkDecisionResponse> winner = new AtomicReference<>();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        List<Thread> threads = List.of(
                decider(appId, reportId, request, ready, start, completed, rejected, winner, unexpected),
                decider(appId, reportId, request, ready, start, completed, rejected, winner, unexpected));
        threads.forEach(Thread::start);

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Thread thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(30));
            assertThat(thread.isAlive()).as("bulk decision worker must finish").isFalse();
        }

        assertThat(unexpected.get()).as("no bulk decision worker fails unexpectedly").isNull();
        assertThat(completed.get()).as("at least one worker applies the decision").isGreaterThanOrEqualTo(1);
        assertThat(completed.get() + rejected.get()).isEqualTo(2);
        assertThat(attendanceReportDecisionRepository.findByReportIdOrderByCreatedAtAsc(reportId))
                .as("the decision history contains exactly one application")
                .hasSize(1);

        ReportingApi.BulkDecisionResponse replay = reportingService.bulkDecide(reportId, request, "tester");
        assertThat(replay).as("a later call with the same operation id replays the winner's result").isEqualTo(winner.get());
        assertThat(attendanceReportDecisionRepository.findByReportIdOrderByCreatedAtAsc(reportId))
                .as("replay does not apply the decision again")
                .hasSize(1);
    }

    private Thread decider(String appId, String reportId, ReportingApi.BulkDecisionRequest request,
                           CountDownLatch ready, CountDownLatch start,
                           AtomicInteger completed, AtomicInteger rejected,
                           AtomicReference<ReportingApi.BulkDecisionResponse> winner,
                           AtomicReference<Throwable> unexpected) {
        return new Thread(() -> {
            TenantContext.set(appId);
            try {
                ready.countDown();
                start.await();
                ReportingApi.BulkDecisionResponse response = reportingService.bulkDecide(reportId, request, "tester");
                winner.compareAndSet(null, response);
                completed.incrementAndGet();
            } catch (BusinessRuleException exception) {
                if ("IDEMPOTENCY_IN_PROGRESS".equals(exception.getCode())) {
                    rejected.incrementAndGet();
                } else {
                    unexpected.set(exception);
                }
            } catch (Throwable throwable) {
                unexpected.set(throwable);
            } finally {
                TenantContext.clear();
            }
        });
    }
}
