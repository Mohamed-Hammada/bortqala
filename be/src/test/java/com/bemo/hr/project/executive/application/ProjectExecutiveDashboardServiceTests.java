package com.bemo.hr.project.executive.application;

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
                dailyLaborSnapshotRepository
        );

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

        when(costLedgerRepository.sumAmountByProjectIdAndEntryType(p1.getId(), CostLedgerEntryType.COMMITTED))
                .thenReturn(BigDecimal.valueOf(10000000));
        when(costLedgerRepository.sumAmountByProjectIdAndEntryType(p1.getId(), CostLedgerEntryType.ACTUAL))
                .thenReturn(BigDecimal.valueOf(15000000));
        when(costLedgerRepository.sumAmountByProjectIdAndEntryType(p1.getId(), CostLedgerEntryType.REVENUE))
                .thenReturn(BigDecimal.valueOf(25000000));

        when(costLedgerRepository.sumAmountByProjectIdAndEntryType(p2.getId(), CostLedgerEntryType.COMMITTED))
                .thenReturn(BigDecimal.valueOf(5000000));
        when(costLedgerRepository.sumAmountByProjectIdAndEntryType(p2.getId(), CostLedgerEntryType.ACTUAL))
                .thenReturn(BigDecimal.valueOf(5000000));
        when(costLedgerRepository.sumAmountByProjectIdAndEntryType(p2.getId(), CostLedgerEntryType.REVENUE))
                .thenReturn(BigDecimal.valueOf(10000000));

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
}
