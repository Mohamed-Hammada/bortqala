package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.payroll.domain.PaymentMethod;
import com.bemo.hr.payroll.domain.PaymentStatus;
import com.bemo.hr.payroll.domain.SalaryPayment;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTrendsTests {
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
    void computesPerMonthTrendsForReportedPeriodAndZerosForMissing() {
        var anchor = YearMonth.now(ZoneId.of("Africa/Cairo"));
        var lastMonth = anchor.minusMonths(1);
        var report = new AttendanceReport(lastMonth.atDay(1), lastMonth.atEndOfMonth(),
                PayCycle.MONTHLY, "v1", "operator");

        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                PayCycle.MONTHLY, anchor.atDay(1), anchor.atEndOfMonth()))
                .thenReturn(Optional.empty());
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                PayCycle.MONTHLY, lastMonth.atDay(1), lastMonth.atEndOfMonth()))
                .thenReturn(Optional.of(report));

        var rows = List.of(
                result(lastMonth.atDay(1), DailyStatus.PRESENT, 30),
                result(lastMonth.atDay(2), DailyStatus.NO_PUNCH, 0));
        when(dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId()))
                .thenReturn(rows);

        var paid = payment(lastMonth, "e1", "5000", "4500", PaymentStatus.PAID, "P-1");
        var pending = payment(lastMonth, "e2", "3000", "2700", PaymentStatus.PENDING, "P-2");
        when(salaryPaymentRepository.findByPeriodYearAndPeriodMonthOrderByCreatedAtDesc(
                lastMonth.getYear(), lastMonth.getMonthValue()))
                .thenReturn(List.of(paid, pending));

        var trends = service.trends(2);
        assertThat(trends).hasSize(2);

        var target = trends.get(0);
        assertThat(target.label()).isEqualTo(lastMonth.toString());
        assertThat(target.year()).isEqualTo(lastMonth.getYear());
        assertThat(target.month()).isEqualTo(lastMonth.getMonthValue());
        assertThat(target.scheduledEmployeeDays()).isEqualTo(2);
        assertThat(target.presentEmployeeDays()).isEqualTo(1);
        assertThat(target.attendanceRate()).isEqualTo(50.0);
        assertThat(target.exceptionDays()).isEqualTo(1);
        assertThat(target.overtimeMinutes()).isEqualTo(30);
        assertThat(target.paidCount()).isEqualTo(1);
        assertThat(target.pendingCount()).isEqualTo(1);
        assertThat(target.totalGross()).isEqualByComparingTo(new BigDecimal("8000"));
        assertThat(target.totalPaid()).isEqualByComparingTo(new BigDecimal("4500"));

        var current = trends.get(1);
        assertThat(current.label()).isEqualTo(anchor.toString());
        assertThat(current.scheduledEmployeeDays()).isZero();
        assertThat(current.presentEmployeeDays()).isZero();
        assertThat(current.attendanceRate()).isZero();
    }

    @Test
    void capsTrendMonthsBetweenOneAndTwentyFour() {
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());

        assertThat(service.trends(0)).hasSize(1);
        assertThat(service.trends(99)).hasSize(24);
    }

    private DailyAttendanceResult result(LocalDate date, DailyStatus status, int overtimeMinutes) {
        return new DailyAttendanceResult("report-1", "e1", "CAT", date, "E-1", "عامل 1", "تشغيل",
                null, null, 2, 480, status == DailyStatus.PRESENT ? 480 : 0, 0, 0,
                overtimeMinutes, status, null, "v1");
    }

    private SalaryPayment payment(YearMonth period, String employeeId, String gross, String net,
                                  PaymentStatus status, String reference) {
        return new SalaryPayment(employeeId, "report-1", period.getYear(), period.getMonthValue(),
                "MONTHLY", period.atDay(1), period.atEndOfMonth(), new BigDecimal(gross),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(net), status,
                status == PaymentStatus.PAID ? Instant.now() : null,
                status == PaymentStatus.PAID ? PaymentMethod.BANK_TRANSFER : null,
                reference, null, "operator");
    }
}
