package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.reporting.api.DashboardApi;
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.reporting.domain.DailyAttendanceResult;
import com.bemo.hr.reporting.domain.DailyStatus;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * WP-08: peak clock-in analytics — first-punch hour-of-day buckets per category.
 * Fixtures use Africa/Cairo (UTC+3, no DST in range) so local hour = UTC hour + 3.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServicePeakClockInTests {
    @Mock private AttendanceCategoryRepository attendanceCategoryRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private AttendanceReportRepository attendanceReportRepository;
    @Mock private DailyAttendanceResultRepository dailyAttendanceResultRepository;
    @Mock private ImportBatchRepository importBatchRepository;
    @Mock private PunchRecordRepository punchRecordRepository;
    @Mock private SalaryPaymentRepository salaryPaymentRepository;
    @Mock private OperationsService operationsService;
    @Mock private AttendanceReportRefreshService attendanceReportRefreshService;

    private DashboardService service;
    private ZoneId cairo;

    @BeforeEach
    void setUp() {
        service = new DashboardService(attendanceCategoryRepository, employeeRepository,
                attendanceReportRepository, dailyAttendanceResultRepository,
                importBatchRepository, punchRecordRepository, salaryPaymentRepository,
                operationsService, attendanceReportRefreshService, "Africa/Cairo");
        cairo = ZoneId.of("Africa/Cairo");
        // Declared once: un-stubbed months resolve to "no report" (capped window may probe extra months).
        lenient().when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                any(), any(), any())).thenReturn(Optional.empty());
    }

    private AttendanceReport reportFor(YearMonth month) {
        return new AttendanceReport(month.atDay(1), month.atEndOfMonth(), PayCycle.MONTHLY, "v1", "operator");
    }

    private void mockReport(YearMonth month, List<DailyAttendanceResult> rows) {
        var report = reportFor(month);
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                PayCycle.MONTHLY, month.atDay(1), month.atEndOfMonth()))
                .thenReturn(Optional.of(report));
        when(dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId()))
                .thenReturn(rows);
    }

    private DailyAttendanceResult row(String categoryId, int localHour, LocalDate date) {
        Instant firstPunch = date.atTime(localHour, 15).toInstant(ZoneOffset.ofHours(3));
        return new DailyAttendanceResult("r", "e1", categoryId, date, "E-1", "n", categoryId,
                firstPunch, null, 1, 480, 480, 0, 0, 0, DailyStatus.PRESENT, null, "v1");
    }

    @Test
    void bucketsFirstPunchHoursPerCategoryAcrossReportedMonths() {
        var anchor = YearMonth.now(cairo);
        mockReport(anchor, List.of(
                row("SECURITY", 6, anchor.atDay(2)), // DBG
                row("SECURITY", 6, anchor.atDay(3)),
                row("ADMIN", 8, anchor.atDay(2))));
        mockReport(anchor.minusMonths(1), List.of(
                row("SECURITY", 7, anchor.minusMonths(1).atDay(5)),
                row("ADMIN", 6, anchor.minusMonths(1).atDay(6))));

        var buckets = service.clockInHistogram(3, null);

        assertThat(buckets).hasSize(24);
        // Cairo local 06:00 → UTC 03:00; zone-correct bucketing across month boundary:
        // h6 = Aug SECURITY ×2 + Jul ADMIN ×1 · h7 = Jul SECURITY ×1 · h8 = Aug ADMIN ×1.
        assertThat(buckets.get(6).countsByCategory()).containsEntry("ADMIN", 1L).containsEntry("SECURITY", 2L);
        assertThat(buckets.get(7).countsByCategory()).containsEntry("SECURITY", 1L);
        assertThat(buckets.get(8).countsByCategory()).containsEntry("ADMIN", 1L);
        long total = buckets.stream().mapToLong(b -> b.countsByCategory().values().stream().mapToLong(Long::longValue).sum()).sum();
        assertThat(total).isEqualTo(5L);
    }

    @Test
    void categoryFilterNarrowsBuckets() {
        var anchor = YearMonth.now(cairo);
        mockReport(anchor, List.of(row("SECURITY", 6, anchor.atDay(2)), row("ADMIN", 9, anchor.atDay(2))));

        var buckets = service.clockInHistogram(1, "ADMIN");

        long securityCount = buckets.get(6).countsByCategory().getOrDefault("SECURITY", 0L);
        assertThat(securityCount).isZero();
        assertThat(buckets.get(9).countsByCategory()).containsEntry("ADMIN", 1L);
    }

    @Test
    void bucketsShiftWithConfiguredCompanyZone() {
        var tokyo = new DashboardService(attendanceCategoryRepository, employeeRepository,
                attendanceReportRepository, dailyAttendanceResultRepository,
                importBatchRepository, punchRecordRepository, salaryPaymentRepository,
                operationsService, attendanceReportRefreshService, "Asia/Tokyo");
        var anchor = YearMonth.now(cairo);
        LocalDate date = anchor.atDay(2);
        // 06:15 at UTC+3 (June Cairo) == 03:15 UTC == 12:15 in Tokyo (UTC+9).
        mockReport(anchor, List.of(row("SECURITY", 6, date)));

        int cairoHour = hourWithPunches(service.clockInHistogram(1, null));
        int tokyoHour = hourWithPunches(tokyo.clockInHistogram(1, null));
        org.junit.jupiter.api.Assertions.assertEquals(6, cairoHour);
        org.junit.jupiter.api.Assertions.assertEquals(12, tokyoHour);
    }

    private int hourWithPunches(java.util.List<com.bemo.hr.reporting.api.DashboardApi.ClockInBucket> buckets) {
        return buckets.stream()
                .filter(bucket -> bucket.countsByCategory().values().stream()
                        .mapToLong(Long::longValue).sum() > 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected one bucket with punches"))
                .hour();
    }

    @Test
    void missingReportsAndPunchlessRowsAreSkippedAndMonthsAreCapped() {
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                any(), any(), any())).thenReturn(Optional.empty());

        assertThat(service.clockInHistogram(0, null)).hasSize(24);
        var capped = service.clockInHistogram(99, null);
        assertThat(capped).hasSize(24);
        assertThat(capped.stream().allMatch(b -> b.countsByCategory().isEmpty())).isTrue();
    }
}
