package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.operations.PartnerLedgerEntry;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.project.domain.CostCategory;
import com.bemo.hr.project.domain.CostLedgerEntryType;
import com.bemo.hr.project.domain.ProjectCostLedgerEntry;
import com.bemo.hr.project.infrastructure.ProjectCostLedgerEntryRepository;
import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkforceProjectIntegrationTests {

    @Mock
    private WorkforceSettlementPeriodRepository periodRepository;
    @Mock
    private WorkerSettlementRepository workerSettlementRepository;
    @Mock
    private ContractorSettlementRepository contractorSettlementRepository;
    @Mock
    private ContractorSettlementLineRepository contractorSettlementLineRepository;
    @Mock
    private ContractorSettlementAdjustmentRepository contractorSettlementAdjustmentRepository;
    @Mock
    private WorkforceSettlementIssueRepository issueRepository;
    @Mock
    private ManualAttendanceEntryRepository attendanceRepository;
    @Mock
    private WorkerAssignmentRepository assignmentRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private ContractorRepository contractorRepository;
    @Mock
    private WorkforceAdvanceRepository advanceRepository;
    @Mock
    private WorkforceAdvancePolicyRepository advancePolicyRepository;
    @Mock
    private PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    @Mock
    private ProjectCostLedgerEntryRepository projectCostLedgerEntryRepository;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private WorkforceExcelExportService excelExportService;
    @Mock
    private AuditService auditService;
    @Mock
    private PlatformTransactionManager platformTransactionManager;

    private WorkforceSettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new WorkforceSettlementService(
                periodRepository,
                workerSettlementRepository,
                contractorSettlementRepository,
                contractorSettlementLineRepository,
                contractorSettlementAdjustmentRepository,
                issueRepository,
                attendanceRepository,
                assignmentRepository,
                workerRepository,
                contractorRepository,
                advanceRepository,
                advancePolicyRepository,
                partnerLedgerEntryRepository,
                projectCostLedgerEntryRepository,
                idempotencyService,
                excelExportService,
                auditService,
                platformTransactionManager
        );
    }

    @Test
    void getProjectLaborCostReportAggregatesLinesCorrectly() {
        String projectId = "proj-101";

        ContractorSettlementLine line1 = new ContractorSettlementLine(
                "cs-1", "w-1", projectId, "wbs-1", "CC-LABOR",
                BigDecimal.valueOf(10), BigDecimal.valueOf(200), BigDecimal.valueOf(2000),
                BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(2100), "{}"
        );
        ContractorSettlementLine line2 = new ContractorSettlementLine(
                "cs-1", "w-2", projectId, "wbs-2", "CC-LABOR",
                BigDecimal.valueOf(15), BigDecimal.valueOf(250), BigDecimal.valueOf(3750),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(3750), "{}"
        );

        when(contractorSettlementLineRepository.findByProjectId(projectId)).thenReturn(List.of(line1, line2));

        Worker w1 = mock(Worker.class);
        when(w1.getCode()).thenReturn("WRK-001");
        when(w1.getFullName()).thenReturn("Ahmed Hassan");
        when(w1.getContractorId()).thenReturn("cnt-1");
        when(workerRepository.findById("w-1")).thenReturn(Optional.of(w1));

        Worker w2 = mock(Worker.class);
        when(w2.getCode()).thenReturn("WRK-002");
        when(w2.getFullName()).thenReturn("Mahmoud Ali");
        when(w2.getContractorId()).thenReturn("cnt-1");
        when(workerRepository.findById("w-2")).thenReturn(Optional.of(w2));

        Contractor cnt = mock(Contractor.class);
        when(cnt.getName()).thenReturn("Al-Ahram Contracting");
        when(contractorRepository.findById("cnt-1")).thenReturn(Optional.of(cnt));

        WorkforceApi.ProjectLaborCostReportResponse report = settlementService.getProjectLaborCostReport(projectId, null);

        assertThat(report).isNotNull();
        assertThat(report.projectId()).isEqualTo(projectId);
        assertThat(report.totalWorkersCount()).isEqualTo(2);
        assertThat(report.totalAttendanceDays()).isEqualByComparingTo(BigDecimal.valueOf(25));
        assertThat(report.totalGrossLaborCost()).isEqualByComparingTo(BigDecimal.valueOf(5750));
        assertThat(report.totalOvertimeAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(report.totalNetLaborCost()).isEqualByComparingTo(BigDecimal.valueOf(5850));
        assertThat(report.items()).hasSize(2);
        assertThat(report.items().get(0).workerName()).isEqualTo("Ahmed Hassan");
        assertThat(report.items().get(0).contractorName()).isEqualTo("Al-Ahram Contracting");
    }

    @Test
    void workerAssignmentAndManualAttendanceSupportProjectDimensions() {
        WorkerAssignment assignment = new WorkerAssignment(
                "dsp-1", "w-1", "req-1", "cnt-1",
                "proj-101", "wbs-site-prep", "CC-01", "Zone A",
                java.time.LocalDate.parse("2026-08-01"), java.time.LocalDate.parse("2026-08-15"),
                BigDecimal.valueOf(250), BigDecimal.valueOf(8)
        );

        assertThat(assignment.getProjectId()).isEqualTo("proj-101");
        assertThat(assignment.getWbsNodeId()).isEqualTo("wbs-site-prep");
        assertThat(assignment.getCostCodeId()).isEqualTo("CC-01");
        assertThat(assignment.getSiteLocation()).isEqualTo("Zone A");

        ManualAttendanceEntry entry = new ManualAttendanceEntry(
                "w-1", "proj-101", "wbs-site-prep", "CC-01",
                "2026-08-05", BigDecimal.ONE, "08:00", "17:00",
                BigDecimal.valueOf(8), BigDecimal.valueOf(1), BigDecimal.ZERO,
                BigDecimal.valueOf(250), "MANUAL", "Site installation"
        );

        assertThat(entry.getProjectId()).isEqualTo("proj-101");
        assertThat(entry.getWbsNodeId()).isEqualTo("wbs-site-prep");
        assertThat(entry.getCostCodeId()).isEqualTo("CC-01");
        assertThat(entry.getAttendanceValue()).isEqualByComparingTo(BigDecimal.ONE);
    }
}
