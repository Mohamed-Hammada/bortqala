package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.calendar.infrastructure.ConfirmedHolidayRepository;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import com.bemo.hr.reporting.api.ReportingApi;
import com.bemo.hr.reporting.domain.AttendanceDecision;
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.reporting.domain.DayAnomaly;
import com.bemo.hr.reporting.domain.DayAnomalyDecision;
import com.bemo.hr.reporting.domain.DayAnomalyResultSnapshot;
import com.bemo.hr.reporting.domain.DayAnomalyStatus;
import com.bemo.hr.reporting.domain.DailyAttendanceResult;
import com.bemo.hr.reporting.domain.DailyStatus;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyRepository;
import com.bemo.hr.reporting.infrastructure.DayAnomalyResultSnapshotRepository;
import com.bemo.hr.reporting.infrastructure.HolidayProposalRepository;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingDayAnomalyTests {
    @Mock private AttendanceReportRepository reportRepository;
    @Mock private DailyAttendanceResultRepository resultRepository;
    @Mock private HolidayProposalRepository holidayRepository;
    @Mock private DayAnomalyRepository anomalyRepository;
    @Mock private DayAnomalyResultSnapshotRepository snapshotRepository;
    @Mock private ConfirmedHolidayRepository confirmedHolidayRepository;
    @Mock private AttendanceCategoryRepository categoryRepository;
    @Mock private ScheduleRuleRepository scheduleRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PunchRecordRepository punchRepository;
    @Mock private ReportExporter exporter;
    @Mock private AuditService auditService;
    @Mock private TenantApplicationRepository tenantApplicationRepository;
    private ReportingService service;

    @BeforeEach
    void setUp() {
        service = new ReportingService(reportRepository, resultRepository, holidayRepository, anomalyRepository,
                snapshotRepository, confirmedHolidayRepository, categoryRepository, scheduleRepository,
                employeeRepository, punchRepository, exporter, "Africa/Cairo", auditService,
                tenantApplicationRepository);
    }

    @Test
    void detectsPersistsDecidesAndReversesDeviceOutageWithoutRepeatingThePrompt() {
        LocalDate date = LocalDate.of(2026, 7, 14);
        AttendanceReport report = new AttendanceReport(date, date, PayCycle.MONTHLY, "rules", "tester");
        report.startReview(3);
        List<DailyAttendanceResult> results = List.of(
                result(report.getId(), "1", date, DailyStatus.NO_PUNCH, 0),
                result(report.getId(), "2", date, DailyStatus.NO_PUNCH, 0),
                result(report.getId(), "3", date, DailyStatus.NO_PUNCH, 0),
                result(report.getId(), "4", date, DailyStatus.PRESENT, 2));
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(resultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId())).thenReturn(results);
        when(holidayRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(report.getId())).thenReturn(List.of());
        when(anomalyRepository.findByReportIdAndCategoryIdAndWorkDate(report.getId(), "CAT", date))
                .thenReturn(Optional.empty());
        when(anomalyRepository.save(any(DayAnomaly.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(anomalyRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(report.getId())).thenReturn(List.of());

        service.detectDayAnomalies(report.getId(), "reviewer");

        ArgumentCaptor<DayAnomaly> anomalyCaptor = ArgumentCaptor.forClass(DayAnomaly.class);
        verify(anomalyRepository).save(anomalyCaptor.capture());
        DayAnomaly anomaly = anomalyCaptor.getValue();
        assertThat(anomaly.getAffectedCount()).isEqualTo(3);
        assertThat(anomaly.getTotalEmployeeCount()).isEqualTo(4);
        assertThat(anomaly.getAbsencePercentage()).isEqualByComparingTo("75.00");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DayAnomalyResultSnapshot>> snapshotsCaptor = ArgumentCaptor.forClass(List.class);
        verify(snapshotRepository).saveAll(snapshotsCaptor.capture());
        List<DayAnomalyResultSnapshot> snapshots = snapshotsCaptor.getValue();
        assertThat(snapshots).hasSize(3);

        when(anomalyRepository.findById(anomaly.getId())).thenReturn(Optional.of(anomaly));
        when(snapshotRepository.findByAnomalyId(anomaly.getId())).thenReturn(snapshots);
        when(resultRepository.findAllById(any())).thenReturn(results.subList(0, 3));
        when(anomalyRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(report.getId()))
                .thenReturn(List.of(anomaly));

        var decision = service.decideDayAnomaly(report.getId(), anomaly.getId(),
                new ReportingApi.DayAnomalyDecisionRequest(DayAnomalyDecision.DEVICE_OUTAGE,
                        "انقطاع كهرباء موثق", "op-1"), "manager");

        assertThat(decision.appliedCount()).isEqualTo(3);
        assertThat(anomaly.getStatus()).isEqualTo(DayAnomalyStatus.RESOLVED);
        assertThat(results.subList(0, 3)).allMatch(item -> item.getDecision() == AttendanceDecision.NORMAL_DAY);

        var replay = service.decideDayAnomaly(report.getId(), anomaly.getId(),
                new ReportingApi.DayAnomalyDecisionRequest(DayAnomalyDecision.DEVICE_OUTAGE,
                        "انقطاع كهرباء موثق", "op-1"), "manager");
        assertThat(replay.appliedCount()).isZero();

        var reversal = service.reverseDayAnomaly(report.getId(), anomaly.getId(), "manager");
        assertThat(reversal.appliedCount()).isEqualTo(3);
        assertThat(anomaly.getStatus()).isEqualTo(DayAnomalyStatus.REVERSED);
        assertThat(results.subList(0, 3)).allMatch(item -> item.getDecision() == null && item.isBlocking());
    }

    private DailyAttendanceResult result(String reportId, String employeeId, LocalDate date,
                                         DailyStatus status, int punchCount) {
        return new DailyAttendanceResult(reportId, employeeId, "CAT", date, "E-" + employeeId,
                "عامل " + employeeId, "تشغيل", null, null, punchCount, 480,
                status == DailyStatus.PRESENT ? 480 : 0, 0, 0, 0, status, null, "v1");
    }
}
