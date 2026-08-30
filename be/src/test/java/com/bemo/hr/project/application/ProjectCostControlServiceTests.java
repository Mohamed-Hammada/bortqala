package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.CostControlApi.*;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectCostControlServiceTests {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WbsNodeRepository wbsNodeRepository;

    @Mock
    private ProjectBudgetVersionRepository budgetVersionRepository;

    @Mock
    private ProjectBudgetLineRepository budgetLineRepository;

    @Mock
    private ProjectCostLedgerEntryRepository costLedgerRepository;

    @Mock
    private ProjectForecastEacRepository forecastEacRepository;

    @Mock
    private AuditService auditService;

    private ProjectCostControlService service;

    private Project project;
    private ProjectBudgetVersion version;

    @BeforeEach
    void setUp() {
        service = new ProjectCostControlService(
                projectRepository,
                wbsNodeRepository,
                budgetVersionRepository,
                budgetLineRepository,
                costLedgerRepository,
                forecastEacRepository,
                auditService
        );

        project = new Project(
                "PRJ-303",
                "مشروع إنشاء مستشفى الأمل",
                "Al-Amal Hospital Construction",
                "مشروع طبي متكامل",
                "c-1",
                "b-1",
                "owner-1",
                "pm-1",
                "القاهرة الجديدة",
                "CNT-999",
                BigDecimal.valueOf(100000000), // 100M Contract Value
                "EGP",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 12, 31),
                true
        );

        version = new ProjectBudgetVersion(
                project.getId(),
                1,
                "Baseline Initial Budget V1",
                "Approved baseline"
        );
        version.updateTotalBudget(BigDecimal.valueOf(80000000)); // 80M Budget
        version.approve("pm-1");
    }

    @Test
    void getSummary_computesMarginAndEacVariance() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(budgetVersionRepository.findByProjectIdAndStatus(project.getId(), BudgetVersionStatus.APPROVED))
                .thenReturn(Optional.of(version));
        when(budgetLineRepository.findByBudgetVersionIdOrderBySortOrderAsc(version.getId()))
                .thenReturn(List.of());

        when(costLedgerRepository.sumAmountByProjectIdAndEntryType(project.getId(), CostLedgerEntryType.COMMITTED))
                .thenReturn(BigDecimal.valueOf(20000000)); // 20M Committed
        when(costLedgerRepository.sumAmountByProjectIdAndEntryType(project.getId(), CostLedgerEntryType.ACTUAL))
                .thenReturn(BigDecimal.valueOf(30000000)); // 30M Actual
        when(costLedgerRepository.sumAmountByProjectIdAndEntryType(project.getId(), CostLedgerEntryType.REVENUE))
                .thenReturn(BigDecimal.valueOf(40000000)); // 40M Revenue

        ProjectForecastEac eacItem = new ProjectForecastEac(
                project.getId(),
                "wbs-1",
                "cc-1",
                CostCategory.MATERIAL,
                BigDecimal.valueOf(80000000),
                BigDecimal.valueOf(30000000),
                BigDecimal.valueOf(20000000),
                BigDecimal.valueOf(45000000), // ETC = 45M -> EAC = 75M
                "Forecast"
        );
        when(forecastEacRepository.findByProjectId(project.getId())).thenReturn(List.of(eacItem));

        CostControlSummaryResponse summary = service.getSummary(project.getId());

        assertThat(summary).isNotNull();
        assertThat(summary.totalBudget()).isEqualTo(BigDecimal.valueOf(80000000));
        assertThat(summary.totalActualCost()).isEqualTo(BigDecimal.valueOf(30000000));
        assertThat(summary.totalRecognizedRevenue()).isEqualTo(BigDecimal.valueOf(40000000));

        // Current Gross Profit = 40M - 30M = 10M
        assertThat(summary.currentGrossProfit()).isEqualTo(BigDecimal.valueOf(10000000));
        // Current Gross Margin % = (10M / 40M) * 100 = 25.00%
        assertThat(summary.currentGrossMarginPercent()).isEqualTo(BigDecimal.valueOf(25.00).setScale(2));

        // Forecast EAC = 30M + 45M = 75M
        assertThat(summary.forecastEac()).isEqualTo(BigDecimal.valueOf(75000000).setScale(2));
        // Forecast VAC = 80M Budget - 75M EAC = +5M favorable variance
        assertThat(summary.forecastVac()).isEqualTo(BigDecimal.valueOf(5000000).setScale(2));
        // Forecast Profit = 100M Contract - 75M EAC = 25M
        assertThat(summary.forecastProfit()).isEqualTo(BigDecimal.valueOf(25000000).setScale(2));
        // Forecast Margin % = (25M / 100M) * 100 = 25.00%
        assertThat(summary.forecastMarginPercent()).isEqualTo(BigDecimal.valueOf(25.00).setScale(2));
    }

    @Test
    void createBudgetVersion_withWbsInit() {
        WbsNode boqNode = new WbsNode(
                project.getId(),
                null,
                "01.01",
                "/01.01",
                "أعمال الخرسانة المسلحة",
                "RC Works",
                "Description",
                WbsNodeType.BOQ_ITEM,
                1,
                1,
                "M3",
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2500),
                "cc-1",
                null,
                null,
                WbsNodeStatus.PLANNED
        );

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(budgetVersionRepository.findByProjectIdOrderByVersionNumberDesc(project.getId())).thenReturn(List.of());
        when(budgetVersionRepository.save(any(ProjectBudgetVersion.class))).thenAnswer(i -> i.getArgument(0));
        when(budgetVersionRepository.findById(anyString())).thenReturn(Optional.of(version));
        when(wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(project.getId())).thenReturn(List.of(boqNode));

        CreateBudgetVersionRequest req = new CreateBudgetVersionRequest(
                "Initial Baseline",
                "Budget draft",
                true,
                null
        );

        ProjectBudgetVersionResponse res = service.createBudgetVersion(project.getId(), req, "pm-1");

        assertThat(res).isNotNull();
        verify(budgetLineRepository, atLeastOnce()).save(any(ProjectBudgetLine.class));
        verify(auditService).record(eq("BUDGET_VERSION_CREATE"), eq("PROJECT_BUDGET_VERSION"), any(), eq("pm-1"), any(), isNull());
    }

    @Test
    void recordCostLedgerEntry_updatesActualCostAndEtc() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(costLedgerRepository.save(any(ProjectCostLedgerEntry.class))).thenAnswer(i -> i.getArgument(0));

        ProjectForecastEac eac = new ProjectForecastEac(
                project.getId(),
                "wbs-1",
                "cc-1",
                CostCategory.MATERIAL,
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(100000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(400000),
                "Initial"
        );
        when(forecastEacRepository.findByProjectIdAndWbsNodeId(project.getId(), "wbs-1"))
                .thenReturn(Optional.of(eac));
        when(wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(project.getId())).thenReturn(List.of());

        RecordCostLedgerEntryRequest req = new RecordCostLedgerEntryRequest(
                "wbs-1",
                "cc-1",
                CostCategory.MATERIAL,
                CostLedgerEntryType.ACTUAL,
                "PROCUREMENT",
                "inv-1",
                "INV-2026-001",
                LocalDate.of(2026, 6, 1),
                "Ready mix concrete invoice",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1500),
                BigDecimal.valueOf(150000), // 150,000 Actual Incurred
                "EGP"
        );

        ProjectCostLedgerEntryResponse res = service.recordCostLedgerEntry(project.getId(), req, "buyer-1");

        assertThat(res).isNotNull();
        assertThat(eac.getActualCostToDate()).isEqualTo(BigDecimal.valueOf(250000)); // 100k + 150k
        assertThat(eac.getEstimateToComplete()).isEqualTo(BigDecimal.valueOf(250000)); // 400k - 150k
        verify(forecastEacRepository).save(eac);
    }
}
