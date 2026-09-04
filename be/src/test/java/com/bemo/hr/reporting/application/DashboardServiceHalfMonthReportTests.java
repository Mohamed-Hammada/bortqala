package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.reporting.domain.ReportStatus;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceHalfMonthReportTests {
    @Mock
    private AttendanceCategoryRepository attendanceCategoryRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private AttendanceReportRepository attendanceReportRepository;
    @Mock
    private DailyAttendanceResultRepository dailyAttendanceResultRepository;
    @Mock
    private ImportBatchRepository importBatchRepository;
    @Mock
    private PunchRecordRepository punchRecordRepository;
    @Mock
    private SalaryPaymentRepository salaryPaymentRepository;
    @Mock
    private OperationsService operationsService;
    @Mock
    private AttendanceReportRefreshService attendanceReportRefreshService;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(attendanceCategoryRepository, employeeRepository,
                attendanceReportRepository, dailyAttendanceResultRepository,
                importBatchRepository, punchRecordRepository, salaryPaymentRepository,
                operationsService, attendanceReportRefreshService, "Africa/Cairo");
    }

    @Test
    void surfacesHalfMonthReportWhenMonthlyReportIsMissing() {
        var period = YearMonth.of(2026, 8);
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                PayCycle.MONTHLY, period.atDay(1), period.atEndOfMonth()))
                .thenReturn(Optional.empty());
        var firstHalf = new AttendanceReport(period.atDay(1), period.atDay(15),
                PayCycle.HALF_MONTHLY, "v1", "operator");
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                PayCycle.HALF_MONTHLY, period.atDay(1), period.atDay(15)))
                .thenReturn(Optional.of(firstHalf));
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                PayCycle.HALF_MONTHLY, period.atDay(16), period.atEndOfMonth()))
                .thenReturn(Optional.empty());
        when(attendanceReportRefreshService.needsRefresh(any(), anyBoolean()))
                .thenReturn(false);
        when(importBatchRepository.findAll()).thenReturn(List.of());
        when(importBatchRepository.findAllByOrderByImportedAtDesc()).thenReturn(List.of());
        when(punchRecordRepository.summarizeUnmatched()).thenReturn(List.of());
        when(operationsService.countStockMovements()).thenReturn(0L);
        when(operationsService.countInventoryItems()).thenReturn(0L);
        when(operationsService.countLowStockItems()).thenReturn(0L);
        when(operationsService.countNegativeStockItems()).thenReturn(0L);
        when(operationsService.countPartnerLedgerEntries()).thenReturn(0L);
        when(operationsService.countActiveParties()).thenReturn(0L);
        when(employeeRepository.findAll()).thenReturn(List.of());
        when(attendanceCategoryRepository.findByScopeIn(any())).thenReturn(List.of());

        var response = service.dashboard(2026, 8);

        assertThat(response.reportStatus()).isNull();
        assertThat(response.reportId()).isNull();
        assertThat(response.halfMonthReports()).hasSize(1);
        assertThat(response.halfMonthReports().get(0).firstHalf()).isTrue();
        assertThat(response.halfMonthReports().get(0).reportId()).isEqualTo(firstHalf.getId());
    }

    @Test
    void reportsBothHalfsWhenBothExist() {
        var period = YearMonth.of(2026, 8);
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                PayCycle.MONTHLY, period.atDay(1), period.atEndOfMonth()))
                .thenReturn(Optional.empty());
        var firstHalf = new AttendanceReport(period.atDay(1), period.atDay(15),
                PayCycle.HALF_MONTHLY, "v1", "operator");
        var secondHalf = new AttendanceReport(period.atDay(16), period.atEndOfMonth(),
                PayCycle.HALF_MONTHLY, "v1", "operator");
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                PayCycle.HALF_MONTHLY, period.atDay(1), period.atDay(15)))
                .thenReturn(Optional.of(firstHalf));
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                PayCycle.HALF_MONTHLY, period.atDay(16), period.atEndOfMonth()))
                .thenReturn(Optional.of(secondHalf));
        when(attendanceReportRefreshService.needsRefresh(any(), anyBoolean()))
                .thenReturn(false);
        when(importBatchRepository.findAll()).thenReturn(List.of());
        when(importBatchRepository.findAllByOrderByImportedAtDesc()).thenReturn(List.of());
        when(punchRecordRepository.summarizeUnmatched()).thenReturn(List.of());
        when(operationsService.countStockMovements()).thenReturn(0L);
        when(operationsService.countInventoryItems()).thenReturn(0L);
        when(operationsService.countLowStockItems()).thenReturn(0L);
        when(operationsService.countNegativeStockItems()).thenReturn(0L);
        when(operationsService.countPartnerLedgerEntries()).thenReturn(0L);
        when(operationsService.countActiveParties()).thenReturn(0L);
        when(employeeRepository.findAll()).thenReturn(List.of());
        when(attendanceCategoryRepository.findByScopeIn(any())).thenReturn(List.of());

        var response = service.dashboard(2026, 8);

        assertThat(response.halfMonthReports()).hasSize(2);
        assertThat(response.halfMonthReports()).extracting(r -> (Object) r.firstHalf())
                .containsExactlyInAnyOrder(true, false);
        assertThat(response.halfMonthReports().get(0).status()).isIn(
                ReportStatus.DRAFT, ReportStatus.IN_REVIEW, ReportStatus.APPROVED, ReportStatus.EXPORTED);
    }
}