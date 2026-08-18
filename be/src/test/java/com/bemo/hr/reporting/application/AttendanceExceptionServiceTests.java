package com.bemo.hr.reporting.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.reporting.api.AttendanceExceptionApi;
import com.bemo.hr.reporting.domain.*;
import com.bemo.hr.reporting.infrastructure.AttendanceExceptionRepository;
import com.bemo.hr.reporting.infrastructure.AttendancePolicyRepository;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceExceptionServiceTests {
    @Mock
    AttendancePolicyRepository policyRepository;
    @Mock
    AttendanceExceptionRepository exceptionRepository;
    @Mock
    AttendanceReportRepository reportRepository;
    @Mock
    DailyAttendanceResultRepository resultRepository;
    @Mock
    AuditService auditService;
    @InjectMocks
    AttendanceExceptionService service;

    @Test
    void employeePolicyWinsOverCategoryAndTenantAndExplainsTheScore() {
        AttendanceReport report = report(0);
        DailyAttendanceResult result = result(report.getId(), DailyStatus.PRESENT, 25, 0, 480);
        AttendancePolicy tenant = policy(AttendancePolicyScope.TENANT, null, 10, 20);
        AttendancePolicy category = policy(AttendancePolicyScope.CATEGORY, "cat-1", 8, 40);
        AttendancePolicy employee = policy(AttendancePolicyScope.EMPLOYEE, "emp-1", 5, 60);
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(resultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId())).thenReturn(List.of(result));
        when(policyRepository.findAllByOrderByPriorityDescEffectiveFromDesc()).thenReturn(List.of(tenant, category, employee));

        assertThat(service.detect(report.getId(), "reviewer")).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttendanceException>> captor = ArgumentCaptor.forClass(List.class);
        verify(exceptionRepository).saveAll(captor.capture());
        AttendanceException saved = captor.getValue().get(0);
        assertThat(saved.getPolicyScope()).isEqualTo(AttendancePolicyScope.EMPLOYEE);
        assertThat(saved.getPolicyId()).isEqualTo(employee.getId());
        assertThat(saved.getScore()).isGreaterThanOrEqualTo(60);
        assertThat(saved.getExplanationKey()).isEqualTo("attendance.exception.late");
    }

    @Test
    void manualOverrideUpdatesTheDailyEvidenceAndClearsTheBlocker() {
        AttendanceReport report = report(1);
        DailyAttendanceResult result = result(report.getId(), DailyStatus.NO_PUNCH, 0, 0, 0);
        AttendancePolicy policy = policy(AttendancePolicyScope.TENANT, null, 15, 30);
        AttendanceException exception = new AttendanceException(result, AttendanceExceptionType.NO_PUNCH, 100,
                "attendance.exception.noPunch", policy, true);
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(exceptionRepository.findAllByIdForUpdate(List.of(exception.getId()))).thenReturn(List.of(exception));
        when(resultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(resultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId())).thenReturn(List.of(result));
        when(exceptionRepository.findByReportIdOrderByScoreDescWorkDateAsc(report.getId())).thenReturn(List.of(exception));

        var response = service.apply(report.getId(), new AttendanceExceptionApi.BulkRequest(List.of(exception.getId()),
                AttendanceExceptionResolution.MARK_PRESENT, "confirmed on site", "op-1"), "reviewer");

        assertThat(response.applied()).isEqualTo(1);
        assertThat(result.getDecision()).isEqualTo(AttendanceDecision.NORMAL_DAY);
        assertThat(exception.getStatus()).isEqualTo(AttendanceExceptionStatus.OVERRIDDEN);
        assertThat(report.getUnresolvedCount()).isZero();
    }

    @Test
    void lockedReportRejectsManualOverrides() {
        AttendanceReport report = report(0);
        report.approve("admin");
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        var request = new AttendanceExceptionApi.BulkRequest(List.of("x"), AttendanceExceptionResolution.IGNORE, "reviewed", "op-2");
        assertThatThrownBy(() -> service.preview(report.getId(), request)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ATTENDANCE_EXCEPTION_PERIOD_LOCKED");
    }

    @Test
    void approvedPayrollIsBlockedWhileCriticalExceptionsRemainOpen() {
        AttendanceReport report = report(0);
        report.approve("admin");
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(exceptionRepository.countByReportIdAndEmployeeIdAndStatusAndPayrollBlockingTrue(
                report.getId(), "emp-1", AttendanceExceptionStatus.OPEN)).thenReturn(1L);
        assertThatThrownBy(() -> service.assertPayrollReady(report.getId(), "emp-1"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("PAYROLL_ATTENDANCE_EXCEPTIONS_OPEN");
    }

    @Test
    void crossMidnightCheckoutBelongsToThePreviousWorkday() {
        ZonedDateTime checkout = ZonedDateTime.of(2026, 8, 11, 5, 30, 0, 0, ZoneId.of("Africa/Cairo"));
        assertThat(DailyAttendanceCalculator.workDateForPunch(checkout, LocalTime.of(22, 0), LocalTime.of(6, 0)))
                .isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(DailyAttendanceCalculator.workDateForPunch(checkout, LocalTime.of(8, 0), LocalTime.of(16, 0)))
                .isEqualTo(LocalDate.of(2026, 8, 11));
    }

    private AttendanceReport report(int unresolved) {
        AttendanceReport report = new AttendanceReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), PayCycle.MONTHLY, "cfg", "admin");
        report.startReview(unresolved);
        return report;
    }

    private DailyAttendanceResult result(String reportId, DailyStatus status, int late, int early, int worked) {
        return new DailyAttendanceResult(reportId, "emp-1", "cat-1", LocalDate.of(2026, 8, 10), "E1", "Employee", "Category",
                null, null, status == DailyStatus.NO_PUNCH ? 0 : 2, 480, worked, late, early, 0, status, null, "rule");
    }

    private AttendancePolicy policy(AttendancePolicyScope scope, String id, int lateThreshold, int lateScore) {
        return new AttendancePolicy(scope.name(), scope, id, LocalDate.of(2026, 1, 1), null, 0, lateThreshold, 15, 960, 100, 70, lateScore, 30, 70, true);
    }
}
