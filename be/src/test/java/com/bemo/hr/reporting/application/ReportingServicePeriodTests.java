package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.calendar.infrastructure.ConfirmedHolidayRepository;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import com.bemo.hr.reporting.api.ReportingApi;
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.reporting.infrastructure.HolidayProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServicePeriodTests {
    @Mock private AttendanceReportRepository attendanceReportRepository;
    @Mock private DailyAttendanceResultRepository dailyAttendanceResultRepository;
    @Mock private HolidayProposalRepository holidayProposalRepository;
    @Mock private ConfirmedHolidayRepository confirmedHolidayRepository;
    @Mock private AttendanceCategoryRepository attendanceCategoryRepository;
    @Mock private ScheduleRuleRepository scheduleRuleRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PunchRecordRepository punchRecordRepository;
    @Mock private ReportExporter reportExporter;
    @Mock private com.bemo.hr.audit.application.AuditService auditService;

    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        reportingService = new ReportingService(attendanceReportRepository, dailyAttendanceResultRepository,
                holidayProposalRepository, confirmedHolidayRepository, attendanceCategoryRepository,
                scheduleRuleRepository, employeeRepository, punchRecordRepository, reportExporter, "Africa/Cairo", auditService);
    }

    @Test
    void exposesPeriodsForBothPayCyclesAndHidesOnlyTheExactExistingPeriod() {
        var monthly = category("ADMIN", PayCycle.MONTHLY);
        var halfMonthly = category("DAILY", PayCycle.HALF_MONTHLY);
        var existingFirstHalf = new AttendanceReport(LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 15), PayCycle.HALF_MONTHLY, "config", "tester");
        when(attendanceCategoryRepository.findAll()).thenReturn(List.of(monthly, halfMonthly));
        when(attendanceReportRepository.findByPeriodStartBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(existingFirstHalf));

        var options = reportingService.availablePeriods(2026);

        assertThat(options).hasSize(35);
        assertThat(options).anyMatch(option -> option.month() == 1 && option.kind() == ReportingApi.PeriodKind.MONTHLY);
        assertThat(options).noneMatch(option -> option.month() == 1 && option.kind() == ReportingApi.PeriodKind.FIRST_HALF);
        assertThat(options).anyMatch(option -> option.month() == 1 && option.kind() == ReportingApi.PeriodKind.SECOND_HALF);
    }

    @Test
    void createsAUserSelectedCrossMonthPeriodForTheExplicitPayCycle() {
        var start = LocalDate.of(2026, 1, 20);
        var end = LocalDate.of(2026, 2, 10);
        var monthly = category("ADMIN", PayCycle.MONTHLY);
        when(attendanceReportRepository.existsByPayCycleAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                PayCycle.MONTHLY, end, start)).thenReturn(false);
        when(attendanceCategoryRepository.findAll()).thenReturn(List.of(monthly));
        when(scheduleRuleRepository.findAll()).thenReturn(List.of());
        when(employeeRepository.findAll()).thenReturn(List.of());
        when(confirmedHolidayRepository.findByWorkDateBetween(start, end)).thenReturn(List.of());
        when(punchRecordRepository.findInRange(any(), any())).thenReturn(List.of());
        when(attendanceReportRepository.save(any(AttendanceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(any()))
                .thenReturn(List.of());
        when(holidayProposalRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(any()))
                .thenReturn(List.of());

        var details = reportingService.create(
                new ReportingApi.CreateRequest(start, end, PayCycle.MONTHLY), "tester");

        assertThat(details.report().periodStart()).isEqualTo(start);
        assertThat(details.report().periodEnd()).isEqualTo(end);
        assertThat(details.report().payCycle()).isEqualTo(PayCycle.MONTHLY);
    }

    private AttendanceCategory category(String code, PayCycle payCycle) {
        return new AttendanceCategory(code, code, 480, payCycle, AttendanceMode.MANUAL,
                false, 127, true);
    }
}
