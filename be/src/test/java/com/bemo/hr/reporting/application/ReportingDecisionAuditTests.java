package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.calendar.infrastructure.ConfirmedHolidayRepository;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import com.bemo.hr.reporting.api.ReportingApi;
import com.bemo.hr.reporting.domain.AttendanceDecision;
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.reporting.domain.DailyAttendanceResult;
import com.bemo.hr.reporting.domain.DailyStatus;
import com.bemo.hr.reporting.domain.HolidayProposal;
import com.bemo.hr.reporting.domain.HolidayProposalStatus;
import com.bemo.hr.reporting.infrastructure.AttendanceReportDecisionRepository;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyResultSnapshotRepository;
import com.bemo.hr.reporting.infrastructure.HolidayProposalRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import com.bemo.hr.shared.idempotency.infrastructure.IdempotencyKeyRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingDecisionAuditTests {
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
    @Mock private AuditService auditService;
    @Mock private TenantApplicationRepository tenantApplicationRepository;
    @Mock private AttendanceReportDecisionRepository attendanceReportDecisionRepository;

    private ReportingService service;

    @BeforeEach
    void setUp() {
        lenient().when(dayAnomalyRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(any())).thenReturn(List.of());
        lenient().when(holidayProposalRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(any())).thenReturn(List.of());
        lenient().when(attendanceReportDecisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ReportingService(attendanceReportRepository, dailyAttendanceResultRepository,
                holidayProposalRepository, dayAnomalyRepository, dayAnomalyResultSnapshotRepository,
                confirmedHolidayRepository, attendanceCategoryRepository, scheduleRuleRepository, employeeRepository,
                punchRecordRepository, reportExporter, "Africa/Cairo", auditService, tenantApplicationRepository,
                attendanceReportDecisionRepository,
                new IdempotencyService(org.mockito.Mockito.mock(IdempotencyKeyRepository.class)),
                org.mockito.Mockito.mock(AttendanceExceptionService.class),
                org.mockito.Mockito.mock(com.bemo.hr.reporting.infrastructure.AttendanceExceptionRepository.class));
    }

    @Test
    void decideDailyPersistsDecisionAndRecordsAuditEvent() {
        var report = report(0);
        var result = blockingResult(report.getId());
        when(attendanceReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(dailyAttendanceResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId()))
                .thenReturn(List.of(result));

        service.decideDaily(report.getId(), result.getId(),
                new ReportingApi.DecisionRequest(AttendanceDecision.NORMAL_DAY, null, "تمت المراجعة", 0L), "reviewer");

        assertThat(result.getDecision()).isEqualTo(AttendanceDecision.NORMAL_DAY);
        assertThat(result.getDecidedBy()).isEqualTo("reviewer");
        assertThat(report.getUnresolvedCount()).isZero();
        verify(auditService).record(eq("DECIDE"), eq("ATTENDANCE_DAILY_RESULT"), eq(result.getId()), eq("reviewer"),
                contains("\"decision\":\"NORMAL_DAY\""), eq(null));
    }

    @Test
    void decideDailyUsesExpectedMinutesForManualEntryNormalDay() {
        var report = report(0);
        var result = blockingResult(report.getId());
        when(attendanceReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(dailyAttendanceResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId()))
                .thenReturn(List.of(result));

        service.decideDaily(report.getId(), result.getId(),
                new ReportingApi.DecisionRequest(AttendanceDecision.NORMAL_DAY, null, null, null), "reviewer");

        assertThat(result.getManualWorkedMinutes()).isEqualTo(result.getExpectedMinutes());
    }

    @Test
    void decideDailyRejectsStaleVersion() {
        var report = report(0);
        var result = blockingResult(report.getId());
        when(attendanceReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(dailyAttendanceResultRepository.findById(result.getId())).thenReturn(Optional.of(result));

        assertThatThrownBy(() -> service.decideDaily(report.getId(), result.getId(),
                new ReportingApi.DecisionRequest(AttendanceDecision.NORMAL_DAY, 480, null, 999L), "reviewer"))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("RPT_VERSION_CONFLICT"));
        assertThat(result.getDecision()).isNull();
    }

    @Test
    void decideDailyRejectsNonBlockingRows() {
        var report = report(0);
        var decided = presentResult(report.getId());
        when(attendanceReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(dailyAttendanceResultRepository.findById(decided.getId())).thenReturn(Optional.of(decided));

        assertThatThrownBy(() -> service.decideDaily(report.getId(), decided.getId(),
                new ReportingApi.DecisionRequest(AttendanceDecision.DEDUCT, 0, null, null), "reviewer"))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("RPT_ROW_NO_DECISION_REQUIRED"));
    }

    @Test
    void decideHolidayRecordsAuditEvent() {
        var report = report(1);
        var proposal = new HolidayProposal(report.getId(), "cat-1", "فئة", LocalDate.of(2026, 8, 1), 1);
        when(attendanceReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(holidayProposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId()))
                .thenReturn(List.of());

        service.decideHoliday(report.getId(), proposal.getId(),
                new ReportingApi.HolidayDecisionRequest(HolidayProposalStatus.REJECTED, null, "غير مؤكدة"), "reviewer");

        assertThat(proposal.getStatus()).isEqualTo(HolidayProposalStatus.REJECTED);
        verify(auditService).record(eq("HOLIDAY_DECISION"), eq("ATTENDANCE_HOLIDAY_PROPOSAL"), eq(proposal.getId()),
                eq("reviewer"), contains("\"status\":\"REJECTED\""), eq(null));
        verify(confirmedHolidayRepository, never()).findByCategoryIdAndWorkDate(any(), any());
    }

    private AttendanceReport report(int unresolved) {
        var report = new AttendanceReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15),
                com.bemo.hr.employee.domain.PayCycle.HALF_MONTHLY, "cfg", "creator");
        report.startReview(unresolved);
        return report;
    }

    private DailyAttendanceResult blockingResult(String reportId) {
        return new DailyAttendanceResult(reportId, "emp-1", "cat-1", LocalDate.of(2026, 8, 1),
                "QA-EMP-0807", "موظف اختبار", "فئة", null, null, 0, 480, 0, 0, 0, 0,
                DailyStatus.MANUAL_ENTRY, "Manual attendance confirmation is required.", "v1");
    }

    private DailyAttendanceResult presentResult(String reportId) {
        return new DailyAttendanceResult(reportId, "emp-1", "cat-1", LocalDate.of(2026, 8, 1),
                "QA-EMP-0807", "موظف اختبار", "فئة", null, null, 2, 480, 460, 5, 0, 0,
                DailyStatus.PRESENT, null, "v1");
    }
}
