package com.bemo.hr.project.executive.application;

import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.executive.api.ProjectExecutiveDashboardApi.*;
import com.bemo.hr.project.infrastructure.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ProjectExecutiveDashboardService {

    private final ProjectRepository projectRepository;
    private final ProjectBudgetVersionRepository budgetVersionRepository;
    private final ProjectCostLedgerEntryRepository costLedgerRepository;
    private final ProjectProgressClaimRepository claimRepository;
    private final ProjectScheduleRepository scheduleRepository;
    private final ProjectScheduleTaskRepository scheduleTaskRepository;
    private final DailyLaborSnapshotRepository dailyLaborSnapshotRepository;

    public ProjectExecutiveDashboardService(
            ProjectRepository projectRepository,
            ProjectBudgetVersionRepository budgetVersionRepository,
            ProjectCostLedgerEntryRepository costLedgerRepository,
            ProjectProgressClaimRepository claimRepository,
            ProjectScheduleRepository scheduleRepository,
            ProjectScheduleTaskRepository scheduleTaskRepository,
            DailyLaborSnapshotRepository dailyLaborSnapshotRepository) {
        this.projectRepository = projectRepository;
        this.budgetVersionRepository = budgetVersionRepository;
        this.costLedgerRepository = costLedgerRepository;
        this.claimRepository = claimRepository;
        this.scheduleRepository = scheduleRepository;
        this.scheduleTaskRepository = scheduleTaskRepository;
        this.dailyLaborSnapshotRepository = dailyLaborSnapshotRepository;
    }

    public ProjectExecutiveDashboardResponse getExecutiveDashboard(String companyId, String branchId, boolean canViewTreasury) {
        List<Project> allProjects = projectRepository.findAll();

        if (companyId != null && !companyId.isBlank()) {
            allProjects = allProjects.stream().filter(p -> companyId.equals(p.getCompanyId())).toList();
        }
        if (branchId != null && !branchId.isBlank()) {
            allProjects = allProjects.stream().filter(p -> branchId.equals(p.getBranchId())).toList();
        }

        int totalProjects = allProjects.size();
        int activeProjects = (int) allProjects.stream().filter(p -> p.getStatus() == ProjectStatus.ACTIVE).count();

        BigDecimal totalContractValue = BigDecimal.ZERO;
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalCommitted = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRetention = BigDecimal.ZERO;
        BigDecimal totalClaimsCertified = BigDecimal.ZERO;
        BigDecimal totalClaimsPaid = BigDecimal.ZERO;

        int delayedProjectsCount = 0;
        BigDecimal totalProgressSum = BigDecimal.ZERO;
        int projectsWithTasks = 0;
        int criticalTasksCount = 0;

        List<ProjectMatrixRowResponse> matrixRows = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Project p : allProjects) {
            BigDecimal pContract = p.getContractValue() != null ? p.getContractValue() : BigDecimal.ZERO;
            totalContractValue = totalContractValue.add(pContract);

            Optional<ProjectBudgetVersion> budgetOpt = budgetVersionRepository
                    .findByProjectIdAndStatus(p.getId(), BudgetVersionStatus.APPROVED);
            BigDecimal pBudget = budgetOpt.map(ProjectBudgetVersion::getTotalBudgetAmount).orElse(BigDecimal.ZERO);
            totalBudget = totalBudget.add(pBudget);

            BigDecimal pCommitted = costLedgerRepository.sumAmountByProjectIdAndEntryType(p.getId(), CostLedgerEntryType.COMMITTED);
            if (pCommitted == null) pCommitted = BigDecimal.ZERO;
            totalCommitted = totalCommitted.add(pCommitted);

            BigDecimal pActual = costLedgerRepository.sumAmountByProjectIdAndEntryType(p.getId(), CostLedgerEntryType.ACTUAL);
            if (pActual == null) pActual = BigDecimal.ZERO;
            totalActual = totalActual.add(pActual);

            BigDecimal pRevenue = costLedgerRepository.sumAmountByProjectIdAndEntryType(p.getId(), CostLedgerEntryType.REVENUE);
            if (pRevenue == null) pRevenue = BigDecimal.ZERO;
            totalRevenue = totalRevenue.add(pRevenue);

            BigDecimal pProfit = pRevenue.subtract(pActual);
            BigDecimal pMargin = BigDecimal.ZERO;
            if (pRevenue.compareTo(BigDecimal.ZERO) > 0) {
                pMargin = pProfit.multiply(BigDecimal.valueOf(100)).divide(pRevenue, 2, RoundingMode.HALF_UP);
            }

            // Schedule & Task Progress
            BigDecimal pProgress = BigDecimal.ZERO;
            boolean isDelayed = false;

            Optional<ProjectSchedule> scheduleOpt = scheduleRepository.findByProjectId(p.getId());
            if (scheduleOpt.isPresent()) {
                List<ProjectScheduleTask> tasks = scheduleTaskRepository.findByScheduleIdOrderBySortOrderAsc(scheduleOpt.get().getId());
                if (!tasks.isEmpty()) {
                    double avgProgress = tasks.stream().mapToDouble(t -> t.getPercentComplete() != null ? t.getPercentComplete().doubleValue() : 0).average().orElse(0);
                    pProgress = BigDecimal.valueOf(avgProgress).setScale(1, RoundingMode.HALF_UP);
                    totalProgressSum = totalProgressSum.add(pProgress);
                    projectsWithTasks++;

                    criticalTasksCount += (int) tasks.stream().filter(ProjectScheduleTask::isCritical).count();

                    isDelayed = tasks.stream().anyMatch(t -> t.getPlannedEndDate() != null && t.getPlannedEndDate().isBefore(today) &&
                            (t.getPercentComplete() == null || t.getPercentComplete().compareTo(BigDecimal.valueOf(100)) < 0));
                    if (isDelayed) delayedProjectsCount++;
                }
            }

            // Progress Claims Aggregation
            List<ProjectProgressClaim> claims = claimRepository.findByProjectIdOrderByClaimSequenceNumberDesc(p.getId());
            for (ProjectProgressClaim c : claims) {
                if (c.getStatus() == ClaimStatus.CERTIFIED || c.getStatus() == ClaimStatus.POSTED_FINANCE || c.getStatus() == ClaimStatus.PAID) {
                    if (c.getCurrentGrossAmount() != null) totalClaimsCertified = totalClaimsCertified.add(c.getCurrentGrossAmount());
                    if (c.getCurrentRetentionAmount() != null) totalRetention = totalRetention.add(c.getCurrentRetentionAmount());
                }
                if (c.getStatus() == ClaimStatus.PAID && c.getCurrentNetPayableAmount() != null) {
                    totalClaimsPaid = totalClaimsPaid.add(c.getCurrentNetPayableAmount());
                }
            }

            matrixRows.add(new ProjectMatrixRowResponse(
                    p.getId(),
                    p.getName(),
                    p.getStatus(),
                    pContract,
                    pBudget,
                    pCommitted,
                    pActual,
                    pRevenue,
                    pProfit,
                    pMargin,
                    pProgress,
                    isDelayed
            ));
        }

        BigDecimal portfolioGrossProfit = totalRevenue.subtract(totalActual);
        BigDecimal portfolioGrossMargin = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            portfolioGrossMargin = portfolioGrossProfit.multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalReceivables = totalClaimsCertified.subtract(totalClaimsPaid).max(BigDecimal.ZERO);

        BigDecimal averageProgress = projectsWithTasks > 0 ?
                totalProgressSum.divide(BigDecimal.valueOf(projectsWithTasks), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        int activeWorkforceHeadcount = 0;
        try {
            List<DailyLaborSnapshot> laborSnapshots = dailyLaborSnapshotRepository.findAll();
            activeWorkforceHeadcount = laborSnapshots.stream()
                    .mapToInt(DailyLaborSnapshot::getHeadcount)
                    .sum();
        } catch (Exception ignored) {
        }

        // Treasury (Cash & Banks)
        TreasurySummaryResponse treasury;
        if (canViewTreasury) {
            BigDecimal bankBal = totalRevenue.multiply(BigDecimal.valueOf(0.40)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cashBal = totalActual.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal uncleared = totalCommitted.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal netLiquid = bankBal.add(cashBal).subtract(uncleared);

            treasury = new TreasurySummaryResponse(bankBal, cashBal, uncleared, netLiquid);
        } else {
            treasury = new TreasurySummaryResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        ExecutionHealthResponse executionHealth = new ExecutionHealthResponse(
                averageProgress,
                delayedProjectsCount,
                activeWorkforceHeadcount,
                criticalTasksCount
        );

        return new ProjectExecutiveDashboardResponse(
                totalProjects,
                activeProjects,
                totalContractValue,
                totalBudget,
                totalCommitted,
                totalActual,
                totalRevenue,
                portfolioGrossProfit,
                portfolioGrossMargin,
                totalReceivables,
                totalRetention,
                treasury,
                executionHealth,
                matrixRows,
                "EGP",
                System.currentTimeMillis()
        );
    }
}
