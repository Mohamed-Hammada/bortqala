package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.calendar.infrastructure.ConfirmedHolidayRepository;
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
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyResultSnapshotRepository;
import com.bemo.hr.reporting.infrastructure.HolidayProposalRepository;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServicePeriodTests {
    @Mock private AttendanceReportRepository attendanceReportRepository;
    @Mock private DailyAttendanceResultRepository dailyAttendanceResultRepository;
    @Mock private HolidayProposalRepository holidayProposalRepository;
    @Mock private DayAnomalyRepository dayAnomalyRepository;
    @Mock private DayAnomalyResultSnapshotRepository dayAnomalyResultSnapshotRepository;
    @Mock private ConfirmedHolidayRepository confirmedHolidayRepository;
    @Mock private AttendanceCategoryRepository attendanceCategoryRepository;
    @Mock private ScheduleRuleRepository scheduleRuleRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PunchRecordRepository punchRecordRepository;
    @Mock private ReportExporter reportExporter;
    @Mock private com.bemo.hr.audit.application.AuditService auditService;
    @Mock private TenantApplicationRepository tenantApplicationRepository;
    @Mock private com.bemo.hr.reporting.infrastructure.AttendanceReportDecisionRepository attendanceReportDecisionRepository;

    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(dayAnomalyRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(any()))
                .thenReturn(List.of());
        reportingService = new ReportingService(attendanceReportRepository, dailyAttendanceResultRepository,
                holidayProposalRepository, dayAnomalyRepository, dayAnomalyResultSnapshotRepository,
                confirmedHolidayRepository, attendanceCategoryRepository, scheduleRuleRepository, employeeRepository,
                punchRecordRepository, reportExporter, "Africa/Cairo", auditService, tenantApplicationRepository,
                attendanceReportDecisionRepository,
                new com.bemo.hr.shared.idempotency.application.IdempotencyService(
                        org.mockito.Mockito.mock(com.bemo.hr.shared.idempotency.infrastructure.IdempotencyKeyRepository.class)));
    }

    @Test
    void exposesPeriodsForBothPayCyclesAndHidesOnlyTheExactExistingPeriod() {
        var monthly = category("ADMIN", PayCycle.MONTHLY);
        var halfMonthly = category("DAILY", PayCycle.HALF_MONTHLY);
        var existingFirstHalf = new AttendanceReport(LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 15), PayCycle.HALF_MONTHLY, "config", "tester");
        when(attendanceCategoryRepository.findByScopeIn(any())).thenReturn(List.of(monthly, halfMonthly));
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
        when(attendanceCategoryRepository.findByScopeIn(any())).thenReturn(List.of(monthly));
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

    @Test
    void createReplaysTheExistingReportForTheSameInputPeriod() {
        var start = LocalDate.of(2026, 1, 1);
        var end = LocalDate.of(2026, 1, 31);
        var existing = new AttendanceReport(start, end, PayCycle.MONTHLY, "config", "tester");
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(PayCycle.MONTHLY, start, end))
                .thenReturn(Optional.of(existing));
        when(dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(any()))
                .thenReturn(List.of());
        when(holidayProposalRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(any()))
                .thenReturn(List.of());

        var replay = reportingService.create(
                new ReportingApi.CreateRequest(start, end, PayCycle.MONTHLY), "tester");

        assertThat(replay.report().id()).isEqualTo(existing.getId());
        verify(attendanceReportRepository, never()).save(any(AttendanceReport.class));
    }

    @Test
    void previewReturnsScopeCountsCoverageAndTheExistingReportLink() {
        var start = LocalDate.of(2026, 1, 1);
        var end = LocalDate.of(2026, 1, 5);
        var monthly = category("ADMIN", PayCycle.MONTHLY);
        var employee = new Employee("E-1", "Tester", null, monthly.getId(), EmploymentType.FIXED,
                LocalDate.of(2026, 1, 1), null, true);
        var schedule = new ScheduleRule(monthly.getId(), "Standard", LocalDate.of(2025, 1, 1), null,
                LocalTime.of(8, 0), 480, 15);
        var existing = new AttendanceReport(start, end, PayCycle.MONTHLY, "config", "tester");
        when(attendanceCategoryRepository.findByScopeIn(any())).thenReturn(List.of(monthly));
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        when(scheduleRuleRepository.findAll()).thenReturn(List.of(schedule));
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(PayCycle.MONTHLY, start, end))
                .thenReturn(Optional.of(existing));
        when(attendanceReportRepository.findAllByOrderByPeriodStartDesc()).thenReturn(List.of(existing));

        var preview = reportingService.preview(start, end, PayCycle.MONTHLY);

        assertThat(preview.employeeCount()).isEqualTo(1);
        assertThat(preview.workdays()).isEqualTo(5);
        assertThat(preview.scheduleCoverageCount()).isEqualTo(5);
        assertThat(preview.categories()).singleElement().satisfies(category ->
                assertThat(category.categoryName()).isEqualTo("ADMIN"));
        assertThat(preview.existingReportId()).isEqualTo(existing.getId());
    }

    @Test
    void previewListsOverlappingReportsThatAreNotExactMatches() {
        var start = LocalDate.of(2026, 1, 10);
        var end = LocalDate.of(2026, 1, 20);
        var monthly = category("ADMIN", PayCycle.MONTHLY);
        var overlapping = new AttendanceReport(LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 31), PayCycle.MONTHLY, "config", "tester");
        when(attendanceCategoryRepository.findByScopeIn(any())).thenReturn(List.of(monthly));
        when(employeeRepository.findAll()).thenReturn(List.of());
        when(scheduleRuleRepository.findAll()).thenReturn(List.of());
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(PayCycle.MONTHLY, start, end))
                .thenReturn(Optional.empty());
        when(attendanceReportRepository.findAllByOrderByPeriodStartDesc()).thenReturn(List.of(overlapping));

        var preview = reportingService.preview(start, end, PayCycle.MONTHLY);

        assertThat(preview.existingReportId()).isNull();
        assertThat(preview.overlappingReportIds()).containsExactly(overlapping.getId());
    }

    private AttendanceCategory category(String code, PayCycle payCycle) {
        return new AttendanceCategory(code, code, 480, payCycle, AttendanceMode.MANUAL,
                false, 127, true);
    }
}