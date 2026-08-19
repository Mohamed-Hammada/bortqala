package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.CostControlApi.*;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ProjectCostControlService {

    private final ProjectRepository projectRepository;
    private final WbsNodeRepository wbsNodeRepository;
    private final ProjectBudgetVersionRepository budgetVersionRepository;
    private final ProjectBudgetLineRepository budgetLineRepository;
    private final ProjectCostLedgerEntryRepository costLedgerRepository;
    private final ProjectForecastEacRepository forecastEacRepository;
    private final AuditService auditService;

    public ProjectCostControlService(
            ProjectRepository projectRepository,
            WbsNodeRepository wbsNodeRepository,
            ProjectBudgetVersionRepository budgetVersionRepository,
            ProjectBudgetLineRepository budgetLineRepository,
            ProjectCostLedgerEntryRepository costLedgerRepository,
            ProjectForecastEacRepository forecastEacRepository,
            AuditService auditService) {
        this.projectRepository = projectRepository;
        this.wbsNodeRepository = wbsNodeRepository;
        this.budgetVersionRepository = budgetVersionRepository;
        this.budgetLineRepository = budgetLineRepository;
        this.costLedgerRepository = costLedgerRepository;
        this.forecastEacRepository = forecastEacRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public CostControlSummaryResponse getSummary(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND"));

        Optional<ProjectBudgetVersion> approvedVersion = budgetVersionRepository
                .findByProjectIdAndStatus(projectId, BudgetVersionStatus.APPROVED);

        BigDecimal totalBudget = approvedVersion.map(ProjectBudgetVersion::getTotalBudgetAmount).orElse(BigDecimal.ZERO);
        BigDecimal totalCommitted = costLedgerRepository.sumAmountByProjectIdAndEntryType(projectId, CostLedgerEntryType.COMMITTED);
        if (totalCommitted == null) totalCommitted = BigDecimal.ZERO;
        BigDecimal totalActual = costLedgerRepository.sumAmountByProjectIdAndEntryType(projectId, CostLedgerEntryType.ACTUAL);
        if (totalActual == null) totalActual = BigDecimal.ZERO;
        BigDecimal totalRevenue = costLedgerRepository.sumAmountByProjectIdAndEntryType(projectId, CostLedgerEntryType.REVENUE);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        BigDecimal currentGrossProfit = totalRevenue.subtract(totalActual);
        BigDecimal currentGrossMargin = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            currentGrossMargin = currentGrossProfit.multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP);
        }

        List<ProjectForecastEac> forecastList = forecastEacRepository.findByProjectId(projectId);
        BigDecimal forecastEac = forecastList.stream()
                .map(ProjectForecastEac::getEstimateAtCompletion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (forecastEac.compareTo(BigDecimal.ZERO) == 0 && totalActual.compareTo(BigDecimal.ZERO) > 0) {
            forecastEac = totalActual.add(totalCommitted);
        }

        BigDecimal forecastVac = totalBudget.subtract(forecastEac);
        BigDecimal contractValue = project.getContractValue() != null ? project.getContractValue() : BigDecimal.ZERO;
        BigDecimal forecastProfit = contractValue.subtract(forecastEac);
        BigDecimal forecastMargin = BigDecimal.ZERO;
        if (contractValue.compareTo(BigDecimal.ZERO) > 0) {
            forecastMargin = forecastProfit.multiply(BigDecimal.valueOf(100))
                    .divide(contractValue, 2, RoundingMode.HALF_UP);
        }

        List<CostCategoryBreakdownResponse> categoryBreakdowns = new ArrayList<>();
        for (CostCategory cat : CostCategory.values()) {
            BigDecimal catBudget = BigDecimal.ZERO;
            if (approvedVersion.isPresent()) {
                List<ProjectBudgetLine> lines = budgetLineRepository.findByBudgetVersionIdOrderBySortOrderAsc(approvedVersion.get().getId());
                catBudget = lines.stream()
                        .filter(l -> l.getCostCategory() == cat)
                        .map(ProjectBudgetLine::getBudgetAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            BigDecimal catCommitted = costLedgerRepository.sumAmountByProjectIdAndCategoryAndEntryType(projectId, cat, CostLedgerEntryType.COMMITTED);
            if (catCommitted == null) catCommitted = BigDecimal.ZERO;
            BigDecimal catActual = costLedgerRepository.sumAmountByProjectIdAndCategoryAndEntryType(projectId, cat, CostLedgerEntryType.ACTUAL);
            if (catActual == null) catActual = BigDecimal.ZERO;
            BigDecimal catVariance = catBudget.subtract(catActual).subtract(catCommitted);

            categoryBreakdowns.add(new CostCategoryBreakdownResponse(cat, catBudget, catCommitted, catActual, catVariance));
        }

        return new CostControlSummaryResponse(
                project.getId(),
                project.getName(),
                contractValue,
                project.getCurrencyCode(),
                totalBudget,
                totalCommitted,
                totalActual,
                totalRevenue,
                currentGrossProfit,
                currentGrossMargin,
                forecastEac,
                forecastVac,
                forecastProfit,
                forecastMargin,
                categoryBreakdowns
        );
    }

    // ─── Budget Versions ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProjectBudgetVersionResponse> listBudgetVersions(String projectId) {
        return budgetVersionRepository.findByProjectIdOrderByVersionNumberDesc(projectId).stream()
                .map(this::mapBudgetVersionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectBudgetVersionResponse getBudgetVersion(String versionId) {
        ProjectBudgetVersion version = budgetVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("BUDGET_VERSION_NOT_FOUND"));
        List<ProjectBudgetLineResponse> lines = budgetLineRepository.findByBudgetVersionIdOrderBySortOrderAsc(versionId).stream()
                .map(this::mapBudgetLine)
                .toList();

        return new ProjectBudgetVersionResponse(
                version.getId(),
                version.getProjectId(),
                version.getVersionNumber(),
                version.getVersionName(),
                version.getStatus(),
                version.getApprovedByUserId(),
                version.getApprovedAt(),
                version.getTotalBudgetAmount(),
                version.getNotes(),
                lines.size(),
                lines
        );
    }

    public ProjectBudgetVersionResponse createBudgetVersion(String projectId, CreateBudgetVersionRequest req, String userId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND"));

        List<ProjectBudgetVersion> existing = budgetVersionRepository.findByProjectIdOrderByVersionNumberDesc(projectId);
        int nextVerNum = existing.isEmpty() ? 1 : existing.get(0).getVersionNumber() + 1;

        ProjectBudgetVersion version = new ProjectBudgetVersion(
                projectId,
                nextVerNum,
                req.versionName(),
                req.notes()
        );
        version = budgetVersionRepository.save(version);

        BigDecimal totalBudget = BigDecimal.ZERO;
        int sort = 1;

        if (req.initFromWbs()) {
            List<WbsNode> wbsNodes = wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(projectId);
            for (WbsNode n : wbsNodes) {
                if (n.getNodeType() == WbsNodeType.BOQ_ITEM || n.getNodeType() == WbsNodeType.WORK_PACKAGE) {
                    ProjectBudgetLine line = new ProjectBudgetLine(
                            version.getId(),
                            projectId,
                            n.getId(),
                            n.getCostCodeId(),
                            CostCategory.MATERIAL,
                            n.getName(),
                            n.getPlannedQuantity(),
                            n.getUnitOfMeasure(),
                            n.getUnitRate(),
                            sort++
                    );
                    budgetLineRepository.save(line);
                    totalBudget = totalBudget.add(line.getBudgetAmount());
                }
            }
        } else if (req.lines() != null) {
            for (SaveBudgetLineRequest l : req.lines()) {
                ProjectBudgetLine line = new ProjectBudgetLine(
                        version.getId(),
                        projectId,
                        l.wbsNodeId(),
                        l.costCodeId(),
                        l.costCategory(),
                        l.description(),
                        l.budgetQuantity(),
                        l.unitOfMeasure(),
                        l.budgetUnitRate(),
                        l.sortOrder() > 0 ? l.sortOrder() : sort++
                );
                budgetLineRepository.save(line);
                totalBudget = totalBudget.add(line.getBudgetAmount());
            }
        }

        version.updateTotalBudget(totalBudget);
        budgetVersionRepository.save(version);

        auditService.record("BUDGET_VERSION_CREATE", "PROJECT_BUDGET_VERSION", version.getId(), userId,
                "Created budget version " + version.getVersionName() + " amount=" + totalBudget, null);

        return getBudgetVersion(version.getId());
    }

    public ProjectBudgetVersionResponse approveBudgetVersion(String versionId, String userId) {
        ProjectBudgetVersion version = budgetVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("BUDGET_VERSION_NOT_FOUND"));

        // Supersede prior approved version if any
        budgetVersionRepository.findByProjectIdAndStatus(version.getProjectId(), BudgetVersionStatus.APPROVED)
                .ifPresent(ProjectBudgetVersion::supersede);

        version.approve(userId);
        budgetVersionRepository.save(version);

        // Sync lines into ProjectForecastEac
        List<ProjectBudgetLine> lines = budgetLineRepository.findByBudgetVersionIdOrderBySortOrderAsc(versionId);
        for (ProjectBudgetLine line : lines) {
            if (line.getWbsNodeId() != null) {
                Optional<ProjectForecastEac> forecastOpt = forecastEacRepository
                        .findByProjectIdAndWbsNodeId(version.getProjectId(), line.getWbsNodeId());

                if (forecastOpt.isPresent()) {
                    ProjectForecastEac eac = forecastOpt.get();
                    eac.updateActualsAndCommitments(line.getBudgetAmount(), null, null);
                    forecastEacRepository.save(eac);
                } else {
                    ProjectForecastEac eac = new ProjectForecastEac(
                            version.getProjectId(),
                            line.getWbsNodeId(),
                            line.getCostCodeId(),
                            line.getCostCategory(),
                            line.getBudgetAmount(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            line.getBudgetAmount(), // Default ETC = Budget
                            "Baseline from budget V" + version.getVersionNumber()
                    );
                    forecastEacRepository.save(eac);
                }
            }
        }

        auditService.record("BUDGET_VERSION_APPROVE", "PROJECT_BUDGET_VERSION", version.getId(), userId,
                "Approved budget version " + version.getVersionName(), null);

        return getBudgetVersion(version.getId());
    }

    // ─── Cost Ledger Entries ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProjectCostLedgerEntryResponse> listCostLedgerEntries(String projectId) {
        Map<String, String> wbsCodeMap = wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(projectId).stream()
                .collect(Collectors.toMap(WbsNode::getId, WbsNode::getWbsCode, (a, b) -> a));

        return costLedgerRepository.findByProjectIdOrderByEntryDateDesc(projectId).stream()
                .map(e -> mapCostLedgerEntry(e, wbsCodeMap))
                .toList();
    }

    public ProjectCostLedgerEntryResponse recordCostLedgerEntry(String projectId, RecordCostLedgerEntryRequest req, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND"));

        ProjectCostLedgerEntry entry = new ProjectCostLedgerEntry(
                projectId,
                req.wbsNodeId(),
                req.costCodeId(),
                req.costCategory(),
                req.entryType(),
                req.sourceModule(),
                req.sourceDocumentId(),
                req.sourceDocumentNumber(),
                req.entryDate(),
                req.description(),
                req.quantity(),
                req.unitRate(),
                req.amount(),
                req.currencyCode() != null ? req.currencyCode() : project.getCurrencyCode()
        );
        entry = costLedgerRepository.save(entry);

        // Update ProjectForecastEac if WBS linked
        if (req.wbsNodeId() != null) {
            Optional<ProjectForecastEac> eacOpt = forecastEacRepository
                    .findByProjectIdAndWbsNodeId(projectId, req.wbsNodeId());

            if (eacOpt.isPresent()) {
                ProjectForecastEac eac = eacOpt.get();
                if (req.entryType() == CostLedgerEntryType.ACTUAL) {
                    BigDecimal newActual = eac.getActualCostToDate().add(req.amount());
                    BigDecimal newEtc = eac.getEstimateToComplete().subtract(req.amount()).max(BigDecimal.ZERO);
                    eac.updateActualsAndCommitments(null, newActual, null);
                    eac.updateForecast(newEtc, null);
                } else if (req.entryType() == CostLedgerEntryType.COMMITTED) {
                    BigDecimal newCommitted = eac.getCommittedCost().add(req.amount());
                    eac.updateActualsAndCommitments(null, null, newCommitted);
                }
                forecastEacRepository.save(eac);
            }
        }

        auditService.record("COST_LEDGER_ENTRY", "PROJECT_COST_LEDGER", entry.getId(), userId,
                "Recorded cost ledger " + entry.getEntryType() + " amount=" + entry.getAmount() + " source=" + entry.getSourceModule(), null);

        Map<String, String> wbsCodeMap = wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(projectId).stream()
                .collect(Collectors.toMap(WbsNode::getId, WbsNode::getWbsCode, (a, b) -> a));

        return mapCostLedgerEntry(entry, wbsCodeMap);
    }

    // ─── Forecast EAC / ETC ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProjectForecastEacResponse> listForecastEac(String projectId) {
        List<WbsNode> wbsNodes = wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(projectId);
        Map<String, WbsNode> nodeMap = wbsNodes.stream().collect(Collectors.toMap(WbsNode::getId, n -> n, (a, b) -> a));

        return forecastEacRepository.findByProjectId(projectId).stream()
                .map(e -> {
                    WbsNode node = e.getWbsNodeId() != null ? nodeMap.get(e.getWbsNodeId()) : null;
                    return new ProjectForecastEacResponse(
                            e.getId(),
                            e.getProjectId(),
                            e.getWbsNodeId(),
                            node != null ? node.getWbsCode() : "—",
                            node != null ? node.getName() : "—",
                            e.getCostCodeId(),
                            e.getCostCategory(),
                            e.getBudgetAmount(),
                            e.getActualCostToDate(),
                            e.getCommittedCost(),
                            e.getEstimateToComplete(),
                            e.getEstimateAtCompletion(),
                            e.getVarianceAtCompletion(),
                            e.getForecastProfitMarginPercent(),
                            e.getNotes()
                    );
                })
                .toList();
    }

    public ProjectForecastEacResponse updateForecastEac(String projectId, UpdateForecastEacRequest req, String userId) {
        ProjectForecastEac eac = forecastEacRepository.findByProjectIdAndWbsNodeId(projectId, req.wbsNodeId())
                .orElseThrow(() -> new NotFoundException("FORECAST_EAC_NOT_FOUND"));

        eac.updateForecast(req.estimateToComplete(), req.notes());
        eac = forecastEacRepository.save(eac);

        auditService.record("FORECAST_EAC_UPDATE", "PROJECT_FORECAST_EAC", eac.getId(), userId,
                "Updated ETC=" + eac.getEstimateToComplete() + " EAC=" + eac.getEstimateAtCompletion(), null);

        Optional<WbsNode> node = wbsNodeRepository.findById(eac.getWbsNodeId());

        return new ProjectForecastEacResponse(
                eac.getId(),
                eac.getProjectId(),
                eac.getWbsNodeId(),
                node.map(WbsNode::getWbsCode).orElse("—"),
                node.map(WbsNode::getName).orElse("—"),
                eac.getCostCodeId(),
                eac.getCostCategory(),
                eac.getBudgetAmount(),
                eac.getActualCostToDate(),
                eac.getCommittedCost(),
                eac.getEstimateToComplete(),
                eac.getEstimateAtCompletion(),
                eac.getVarianceAtCompletion(),
                eac.getForecastProfitMarginPercent(),
                eac.getNotes()
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private ProjectBudgetVersionResponse mapBudgetVersionSummary(ProjectBudgetVersion v) {
        int count = budgetLineRepository.findByBudgetVersionIdOrderBySortOrderAsc(v.getId()).size();
        return new ProjectBudgetVersionResponse(
                v.getId(),
                v.getProjectId(),
                v.getVersionNumber(),
                v.getVersionName(),
                v.getStatus(),
                v.getApprovedByUserId(),
                v.getApprovedAt(),
                v.getTotalBudgetAmount(),
                v.getNotes(),
                count,
                List.of()
        );
    }

    private ProjectBudgetLineResponse mapBudgetLine(ProjectBudgetLine l) {
        return new ProjectBudgetLineResponse(
                l.getId(),
                l.getBudgetVersionId(),
                l.getProjectId(),
                l.getWbsNodeId(),
                l.getCostCodeId(),
                l.getCostCategory(),
                l.getDescription(),
                l.getBudgetQuantity(),
                l.getUnitOfMeasure(),
                l.getBudgetUnitRate(),
                l.getBudgetAmount(),
                l.getSortOrder()
        );
    }

    private ProjectCostLedgerEntryResponse mapCostLedgerEntry(ProjectCostLedgerEntry e, Map<String, String> wbsCodeMap) {
        String wbsCode = e.getWbsNodeId() != null ? wbsCodeMap.getOrDefault(e.getWbsNodeId(), "—") : "—";
        return new ProjectCostLedgerEntryResponse(
                e.getId(),
                e.getProjectId(),
                e.getWbsNodeId(),
                wbsCode,
                e.getCostCodeId(),
                e.getCostCategory(),
                e.getEntryType(),
                e.getSourceModule(),
                e.getSourceDocumentId(),
                e.getSourceDocumentNumber(),
                e.getEntryDate(),
                e.getDescription(),
                e.getQuantity(),
                e.getUnitRate(),
                e.getAmount(),
                e.getCurrencyCode(),
                e.getPostedAt()
        );
    }
}
