package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.DailyReportApi.*;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectDailyReportServiceTests {

    @Mock
    private ProjectDailyReportRepository dailyReportRepository;

    @Mock
    private DailyWorkProgressLineRepository progressLineRepository;

    @Mock
    private DailyLaborSnapshotRepository laborSnapshotRepository;

    @Mock
    private DailyEquipmentLogRepository equipmentLogRepository;

    @Mock
    private DailyMaterialConsumptionRepository materialConsumptionRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WbsNodeRepository wbsNodeRepository;

    @Mock
    private AuditService auditService;

    private ProjectDailyReportService service;

    private Project project;
    private WbsNode wbsNode;

    @BeforeEach
    void setUp() {
        service = new ProjectDailyReportService(
                dailyReportRepository,
                progressLineRepository,
                laborSnapshotRepository,
                equipmentLogRepository,
                materialConsumptionRepository,
                projectRepository,
                wbsNodeRepository,
                auditService
        );

        project = new Project(
                "PRJ-001",
                "برج النخيل",
                "Palm Tower",
                "وصف المشروع",
                "c-1",
                "b-1",
                "party-1",
                "pm-1",
                "القاهرة",
                "CNT-101",
                BigDecimal.valueOf(50000000),
                "EGP",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                true
        );

        wbsNode = new WbsNode(
                project.getId(),
                null,
                "1.1",
                "/1.1",
                "حفر الموقع",
                "Excavation",
                "أعمال الحفر",
                WbsNodeType.BOQ_ITEM,
                1,
                0,
                "م3",
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(150),
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1),
                WbsNodeStatus.PLANNED
        );
    }

    @Test
    void createDailyReport_savesReportAndChildLines() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(dailyReportRepository.findByProjectIdAndReportDateAndShift(any(), any(), any())).thenReturn(Optional.empty());
        when(dailyReportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(wbsNodeRepository.findById(wbsNode.getId())).thenReturn(Optional.of(wbsNode));
        when(progressLineRepository.sumApprovedQuantityBeforeDate(any(), any(), any())).thenReturn(BigDecimal.valueOf(200));

        Long reportDateEpoch = LocalDate.of(2026, 3, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        CreateDailyReportRequest req = new CreateDailyReportRequest(
                reportDateEpoch,
                ReportShift.DAY,
                WeatherCondition.SUNNY,
                BigDecimal.valueOf(28.5),
                "سير العمل ممتاز",
                "لا توجد معوقات",
                "تم تنفيذ التوجيه الصباحي HSE",
                List.of(new CreateWorkProgressLineRequest(wbsNode.getId(), BigDecimal.valueOf(100), "القطاع أ", "ملاحظات")),
                List.of(new CreateLaborSnapshotRequest(wbsNode.getId(), null, "حدادة", LaborSourceType.DIRECT_EMPLOYEE, null, 10, BigDecimal.valueOf(8.0), "تشغيل حديد")),
                List.of(new CreateEquipmentLogRequest(wbsNode.getId(), "حفار 20 طن", "EQ-01", EquipmentSiteStatus.WORKING, BigDecimal.valueOf(7.5), BigDecimal.valueOf(0.5), BigDecimal.valueOf(60), "أحمد", "تشغيل مستمر")),
                List.of(new CreateMaterialConsumptionRequest(wbsNode.getId(), "سولار", "لتر", BigDecimal.valueOf(60), "DN-101", "supplier-1", "استهلاك حفار"))
        );

        DailyReportResponse response = service.createDailyReport(project.getId(), req, "user-1");

        assertThat(response).isNotNull();
        assertThat(response.reportNumber()).contains("DPR-PRJ-001");
        assertThat(response.status()).isEqualTo(DailyReportStatus.DRAFT);
        assertThat(response.shift()).isEqualTo(ReportShift.DAY);

        verify(progressLineRepository).save(any(DailyWorkProgressLine.class));
        verify(laborSnapshotRepository).save(any(DailyLaborSnapshot.class));
        verify(equipmentLogRepository).save(any(DailyEquipmentLog.class));
        verify(materialConsumptionRepository).save(any(DailyMaterialConsumption.class));
    }

    @Test
    void createDailyReport_duplicateDateAndShift_throwsException() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        ProjectDailyReport existing = new ProjectDailyReport(project.getId(), "DPR-01", LocalDate.of(2026, 3, 1),
                ReportShift.DAY, WeatherCondition.SUNNY, BigDecimal.valueOf(25), "u-1", "", "", "");
        when(dailyReportRepository.findByProjectIdAndReportDateAndShift(any(), any(), any())).thenReturn(Optional.of(existing));

        Long reportDateEpoch = LocalDate.of(2026, 3, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        CreateDailyReportRequest req = new CreateDailyReportRequest(
                reportDateEpoch,
                ReportShift.DAY,
                WeatherCondition.SUNNY,
                BigDecimal.valueOf(25),
                "", "", "", List.of(), List.of(), List.of(), List.of()
        );

        assertThatThrownBy(() -> service.createDailyReport(project.getId(), req, "user-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DPR_ALREADY_EXISTS_FOR_DATE_SHIFT");
    }

    @Test
    void submitAndApproveLifecycle_updatesWbsNodeProgress() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        ProjectDailyReport report = new ProjectDailyReport(project.getId(), "DPR-PRJ-001-2026-03-01-DAY",
                LocalDate.of(2026, 3, 1), ReportShift.DAY, WeatherCondition.SUNNY, BigDecimal.valueOf(25),
                "u-1", "", "", "");
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyWorkProgressLine line = new DailyWorkProgressLine(
                report.getId(),
                wbsNode.getId(),
                wbsNode.getWbsCode(),
                wbsNode.getName(),
                wbsNode.getUnitOfMeasure(),
                BigDecimal.valueOf(900),
                BigDecimal.valueOf(100),
                wbsNode.getPlannedQuantity(),
                "موقع 1",
                ""
        );
        when(progressLineRepository.findByDailyReportIdOrderByWbsCodeAsc(report.getId())).thenReturn(List.of(line));
        when(wbsNodeRepository.findById(wbsNode.getId())).thenReturn(Optional.of(wbsNode));
        when(progressLineRepository.sumApprovedQuantityUpToDate(eq(project.getId()), eq(wbsNode.getId()), any()))
                .thenReturn(BigDecimal.valueOf(1000));

        // Submit
        DailyReportResponse submitted = service.submitDailyReport(project.getId(), report.getId(), "u-1");
        assertThat(submitted.status()).isEqualTo(DailyReportStatus.SUBMITTED);

        // Approve
        DailyReportResponse approved = service.approveDailyReport(project.getId(), report.getId(), "pm-1");
        assertThat(approved.status()).isEqualTo(DailyReportStatus.APPROVED);
        assertThat(approved.approverUserId()).isEqualTo("pm-1");

        // WBS Node reached planned 1000 quantity -> status COMPLETED
        assertThat(wbsNode.getStatus()).isEqualTo(WbsNodeStatus.COMPLETED);
        verify(wbsNodeRepository).save(wbsNode);
    }

    @Test
    void reopenDailyReport_setsStatusReopened() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        ProjectDailyReport report = new ProjectDailyReport(project.getId(), "DPR-01", LocalDate.of(2026, 3, 1),
                ReportShift.DAY, WeatherCondition.SUNNY, BigDecimal.valueOf(25), "u-1", "", "", "");
        report.submit("u-1");
        report.approve("pm-1");
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportResponse reopened = service.reopenDailyReport(project.getId(), report.getId(), "خطأ في كميات الحفر", "pm-1");

        assertThat(reopened.status()).isEqualTo(DailyReportStatus.REOPENED);
    }
}
