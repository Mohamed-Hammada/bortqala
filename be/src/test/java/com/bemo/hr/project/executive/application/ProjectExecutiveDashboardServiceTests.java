package com.bemo.hr.project.executive.application;

import com.bemo.hr.finance.application.TreasuryPositionService;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.executive.api.ProjectExecutiveDashboardApi.*;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectExecutiveDashboardServiceTests {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectBudgetVersionRepository budgetVersionRepository;

    @Mock
    private ProjectCostLedgerEntryRepository costLedgerRepository;

    @Mock
    private ProjectProgressClaimRepository claimRepository;

    @Mock
    private ProjectScheduleRepository scheduleRepository;

    @Mock
    private ProjectScheduleTaskRepository scheduleTaskRepository;

    @Mock
    private DailyLaborSnapshotRepository dailyLaborSnapshotRepository;

    @Mock
    private TreasuryPositionService treasuryPositionService;

    private ProjectExecutiveDashboardService service;

    private Project p1;
    private Project p2;

    @BeforeEach
    void setUp() {
        service = new ProjectExecutiveDashboardService(
                projectRepository,
                budgetVersionRepository,
                costLedgerRepository,
                claimRepository,
                scheduleRepository,
                scheduleTaskRepository,
                dailyLaborSnapshotRepository,
                treasuryPositionService
        );
        lenient().when(treasuryPositionService.totalBankBalance()).thenReturn(BigDecimal.ZERO);
        lenient().when(treasuryPositionService.totalCashBalance()).thenReturn(BigDecimal.ZERO);
        lenient().when(costLedgerRepository.sumAmountByProjectIdInAndEntryType(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        p1 = new Project(
                "PRJ-001",
                "برج النيل الإداري",
                "Nile Tower",
                "Commercial Tower",
                "c-1",
                "b-1",
                "owner-1",
                "pm-1",
                "Cairo",
                "CNT-101",
                BigDecimal.valueOf(50000000), // 50M
                "EGP",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 12, 31),
                true
        );
        p1.activate();

        p2 = new Project(
                "PRJ-002",
                "مجمع المروج السكني",
                "Al-Moroj Compound",
                "Residential",
                "c-1",
                "b-1",
                "owner-2",
                "pm-2",
                "Giza",
                "CNT-102",
                BigDecimal.valueOf(30000000), // 30M
                "EGP",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2028, 2, 28),
                true
        );
        p2.activate();
    }

    @Test
    void getExecutiveDashboard_computesPortfolioTotalsAndMargin() {
        when(projectRepository.findAll()).thenReturn(List.of(p1, p2));

        ProjectBudgetVersion b1 = new ProjectBudgetVersion(p1.getId(), 1, "V1", "Notes");
        b1.updateTotalBudget(BigDecimal.valueOf(40000000));
        when(budgetVersionRepository.findByProjectIdAndStatus(p1.getId(), BudgetVersionStatus.APPROVED))
                .thenReturn(Optional.of(b1));
        when(budgetVersionRepository.findByProjectIdAndStatus(p2.getId(), BudgetVersionStatus.APPROVED))
                .thenReturn(Optional.empty());

        List<String> bothProjectIds = List.of(p1.getId(), p2.getId());
        List<ProjectCostLedgerEntryRepository.ProjectAmountByType> committedRows =
                List.of(row(p1.getId(), BigDecimal.valueOf(10000000)), row(p2.getId(), BigDecimal.valueOf(5000000)));
        List<ProjectCostLedgerEntryRepository.ProjectAmountByType> actualRows =
                List.of(row(p1.getId(), BigDecimal.valueOf(15000000)), row(p2.getId(), BigDecimal.valueOf(5000000)));
        List<ProjectCostLedgerEntryRepository.ProjectAmountByType> revenueRows =
                List.of(row(p1.getId(), BigDecimal.valueOf(25000000)), row(p2.getId(), BigDecimal.valueOf(10000000)));
        when(costLedgerRepository.sumAmountByProjectIdInAndEntryType(bothProjectIds, CostLedgerEntryType.COMMITTED))
                .thenReturn(committedRows);
        when(costLedgerRepository.sumAmountByProjectIdInAndEntryType(bothProjectIds, CostLedgerEntryType.ACTUAL))
                .thenReturn(actualRows);
        when(costLedgerRepository.sumAmountByProjectIdInAndEntryType(bothProjectIds, CostLedgerEntryType.REVENUE))
                .thenReturn(revenueRows);

        when(scheduleRepository.findByProjectId(p1.getId())).thenReturn(Optional.empty());
        when(scheduleRepository.findByProjectId(p2.getId())).thenReturn(Optional.empty());
        when(claimRepository.findByProjectIdOrderByClaimSequenceNumberDesc(p1.getId())).thenReturn(List.of());
        when(claimRepository.findByProjectIdOrderByClaimSequenceNumberDesc(p2.getId())).thenReturn(List.of());

        ProjectExecutiveDashboardResponse res = service.getExecutiveDashboard(null, null, true);

        assertThat(res).isNotNull();
        assertThat(res.totalProjects()).isEqualTo(2);
        assertThat(res.activeProjects()).isEqualTo(2);

        // Total Contract = 50M + 30M = 80M
        assertThat(res.totalContractValue()).isEqualTo(BigDecimal.valueOf(80000000));
        // Total Budget = 40M + 0 = 40M
        assertThat(res.totalBudget()).isEqualTo(BigDecimal.valueOf(40000000));
        // Total Committed = 10M + 5M = 15M
        assertThat(res.totalCommitted()).isEqualTo(BigDecimal.valueOf(15000000));
        // Total Actual = 15M + 5M = 20M
        assertThat(res.totalActualCost()).isEqualTo(BigDecimal.valueOf(20000000));
        // Total Revenue = 25M + 10M = 35M
        assertThat(res.totalRevenue()).isEqualTo(BigDecimal.valueOf(35000000));

        // Portfolio Gross Profit = 35M - 20M = 15M
        assertThat(res.portfolioGrossProfit()).isEqualTo(BigDecimal.valueOf(15000000));
        // Portfolio Margin % = (15M / 35M) * 100 = 42.86%
        assertThat(res.portfolioGrossMarginPercent()).isEqualTo(BigDecimal.valueOf(42.86));

        assertThat(res.projects()).hasSize(2);
    }

    @Test
    void getExecutiveDashboard_withoutTreasuryAccess_masksTreasury() {
        when(projectRepository.findAll()).thenReturn(List.of(p1));
        when(budgetVersionRepository.findByProjectIdAndStatus(p1.getId(), BudgetVersionStatus.APPROVED))
                .thenReturn(Optional.empty());
        when(scheduleRepository.findByProjectId(p1.getId())).thenReturn(Optional.empty());
        when(claimRepository.findByProjectIdOrderByClaimSequenceNumberDesc(p1.getId())).thenReturn(List.of());

        ProjectExecutiveDashboardResponse res = service.getExecutiveDashboard(null, null, false);

        assertThat(res).isNotNull();
        assertThat(res.treasury().totalBankBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(res.treasury().netLiquidCapital()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Treasury figures come from TreasuryPositionService, not a ratio of revenue/actual/committed")
    void getExecutiveDashboard_withTreasuryAccess_usesRealBalancesNotFabricatedRatios() {
        when(projectRepository.findAll()).thenReturn(List.of(p1));
        when(budgetVersionRepository.findByProjectIdAndStatus(p1.getId(), BudgetVersionStatus.APPROVED))
                .thenReturn(Optional.empty());
        when(scheduleRepository.findByProjectId(p1.getId())).thenReturn(Optional.empty());
        when(claimRepository.findByProjectIdOrderByClaimSequenceNumberDesc(p1.getId())).thenReturn(List.of());
        List<ProjectCostLedgerEntryRepository.ProjectAmountByType> revenueRows =
                List.of(row(p1.getId(), BigDecimal.valueOf(1_000_000))); // would have produced bankBal=400000 under the old *0.40 formula
        List<ProjectCostLedgerEntryRepository.ProjectAmountByType> actualRows =
                List.of(row(p1.getId(), BigDecimal.valueOf(1_000_000))); // would have produced cashBal=50000 under the old *0.05 formula
        List<ProjectCostLedgerEntryRepository.ProjectAmountByType> committedRows =
                List.of(row(p1.getId(), BigDecimal.valueOf(1_000_000)));
        when(costLedgerRepository.sumAmountByProjectIdInAndEntryType(List.of(p1.getId()), CostLedgerEntryType.REVENUE))
                .thenReturn(revenueRows);
        when(costLedgerRepository.sumAmountByProjectIdInAndEntryType(List.of(p1.getId()), CostLedgerEntryType.ACTUAL))
                .thenReturn(actualRows);
        when(costLedgerRepository.sumAmountByProjectIdInAndEntryType(List.of(p1.getId()), CostLedgerEntryType.COMMITTED))
                .thenReturn(committedRows);
        when(treasuryPositionService.totalBankBalance()).thenReturn(BigDecimal.valueOf(777_777));
        when(treasuryPositionService.totalCashBalance()).thenReturn(BigDecimal.valueOf(12_345));

        ProjectExecutiveDashboardResponse res = service.getExecutiveDashboard(null, null, true);

        // Real values from the injected service, unrelated to revenue/actual/committed magnitude —
        // proves the fabricated *0.40 / *0.05 ratios are gone.
        assertThat(res.treasury().totalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(777_777));
        assertThat(res.treasury().totalCashOnHand()).isEqualByComparingTo(BigDecimal.valueOf(12_345));
        assertThat(res.treasury().netLiquidCapital()).isEqualByComparingTo(BigDecimal.valueOf(790_122));
    }

    private static ProjectCostLedgerEntryRepository.ProjectAmountByType row(String projectId, BigDecimal total) {
        ProjectCostLedgerEntryRepository.ProjectAmountByType mockRow =
                org.mockito.Mockito.mock(ProjectCostLedgerEntryRepository.ProjectAmountByType.class);
        lenient().when(mockRow.getProjectId()).thenReturn(projectId);
        lenient().when(mockRow.getTotal()).thenReturn(total);
        return mockRow;
    }
}
