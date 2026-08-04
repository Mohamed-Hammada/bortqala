package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.calendar.domain.ConfirmedHoliday;
import com.bemo.hr.calendar.infrastructure.ConfirmedHolidayRepository;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.domain.ScheduleRule;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import com.bemo.hr.reporting.api.ReportingApi;
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.reporting.domain.DayAnomaly;
import com.bemo.hr.reporting.domain.DayAnomalyResultSnapshot;
import com.bemo.hr.reporting.domain.HolidayProposal;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyResultSnapshotRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.reporting.infrastructure.HolidayProposalRepository;
import com.bemo.hr.shared.api.TransitionResponse;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingTransitionTests {

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

    private ReportingService service() {
        return new ReportingService(attendanceReportRepository, dailyAttendanceResultRepository,
                holidayProposalRepository, dayAnomalyRepository, dayAnomalyResultSnapshotRepository,
                confirmedHolidayRepository, attendanceCategoryRepository, scheduleRuleRepository,
                employeeRepository, punchRecordRepository, reportExporter, "Africa/Cairo",
                auditService, tenantApplicationRepository, attendanceReportDecisionRepository,
                new com.bemo.hr.shared.idempotency.application.IdempotencyService(
                        org.mockito.Mockito.mock(com.bemo.hr.shared.idempotency.infrastructure.IdempotencyKeyRepository.class)));
    }

    private static AttendanceReport inReviewReport() {
        AttendanceReport report = new AttendanceReport(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), PayCycle.MONTHLY, "cfg-v1", "admin");
        report.startReview(0);
        return report;
    }

    private static AttendanceReport approvedReport() {
        AttendanceReport report = inReviewReport();
        report.approve("admin");
        return report;
    }

    @Test
    void approveRejectsAnEmptyReport() {
        AttendanceReport report = inReviewReport();
        when(attendanceReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(dailyAttendanceResultRepository.countByReportId(report.getId())).thenReturn(0L);

        assertThatThrownBy(() -> service().approve(report.getId(), "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("empty report");
    }

    @Test
    void approveReturnsSharedTransitionMetadata() {
        AttendanceReport report = inReviewReport();
        when(attendanceReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(dailyAttendanceResultRepository.countByReportId(report.getId())).thenReturn(5L);

        TransitionResponse response = service().approve(report.getId(), "admin");

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.allowedActions()).containsExactly("REOPEN", "EXPORT");
    }

    @Test
    void approveIsIdempotentForAlreadyApprovedReports() {
        AttendanceReport report = approvedReport();
        when(attendanceReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        TransitionResponse response = service().approve(report.getId(), "admin");

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.allowedActions()).containsExactly("REOPEN", "EXPORT");
    }

    @Test
    void reopenMovesBackToReviewWithApproveExportActions() {
        AttendanceReport report = approvedReport();
        when(attendanceReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        TransitionResponse response = service().reopen(report.getId());

        assertThat(response.status()).isEqualTo("IN_REVIEW");
        assertThat(response.allowedActions()).containsExactly("APPROVE", "EXPORT");
    }
}
