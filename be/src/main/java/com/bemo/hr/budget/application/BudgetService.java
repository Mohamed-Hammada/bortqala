package com.bemo.hr.budget.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.budget.api.BudgetApi;
import com.bemo.hr.budget.domain.Budget;
import com.bemo.hr.budget.domain.BudgetPeriodType;
import com.bemo.hr.budget.domain.BudgetRepository;
import com.bemo.hr.budget.domain.Encumbrance;
import com.bemo.hr.budget.domain.EncumbranceRepository;
import com.bemo.hr.budget.domain.EncumbranceStatus;
import com.bemo.hr.organization.domain.Department;
import com.bemo.hr.organization.infrastructure.DepartmentRepository;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.reporting.infrastructure.ExcelExportSupport;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.i18n.TranslationService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final EncumbranceRepository encumbranceRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;
    private final TranslationService translationService;
    private final com.bemo.hr.budget.BudgetRevisionRepository budgetRevisionRepository;
    private final com.bemo.hr.budget.BudgetTransferRepository budgetTransferRepository;

    public BudgetService(BudgetRepository budgetRepository, EncumbranceRepository encumbranceRepository,
                         DepartmentRepository departmentRepository, AuditService auditService,
                         TranslationService translationService,
                         com.bemo.hr.budget.BudgetRevisionRepository budgetRevisionRepository,
                         com.bemo.hr.budget.BudgetTransferRepository budgetTransferRepository) {
        this.budgetRepository = budgetRepository;
        this.encumbranceRepository = encumbranceRepository;
        this.departmentRepository = departmentRepository;
        this.auditService = auditService;
        this.translationService = translationService;
        this.budgetRevisionRepository = budgetRevisionRepository;
        this.budgetTransferRepository = budgetTransferRepository;
    }

    // ─── Budgets ──────────────────────────────────────────────────────

    public List<BudgetApi.BudgetResponse> listBudgets() {
        return budgetRepository.findAllByOrderByFiscalYearDescPeriodMonthAsc().stream()
                .map(this::toBudgetResponse).toList();
    }

    @Transactional
    public BudgetApi.BudgetResponse createBudget(BudgetApi.BudgetPayload payload) {
        validatePeriod(payload);
        requireDepartment(payload.departmentId());
        Budget budget = new Budget(payload.fiscalYear(), payload.periodType(), payload.periodMonth(),
                payload.departmentId().strip(), payload.plannedAmount(), payload.currencyCode(),
                payload.blocking() == null || payload.blocking(), payload.active() == null || payload.active());
        budget.configureRevisionApproval(payload.revisionApprovalRequired() == null || payload.revisionApprovalRequired());
        Budget saved = budgetRepository.save(budget);
        auditService.record("CREATE", "BUDGET", saved.getId(), getCurrentUser(),
                "{\"year\":" + saved.getFiscalYear() + ",\"department\":\"" + saved.getDepartmentId()
                        + "\",\"planned\":" + saved.getPlannedAmount() + "}", null);
        return toBudgetResponse(saved);
    }

    @Transactional
    public BudgetApi.BudgetResponse updateBudget(String id, BudgetApi.BudgetPayload payload) {
        validatePeriod(payload);
        requireDepartment(payload.departmentId());
        Budget budget = requireBudget(id);
        if (budget.getPlannedAmount().compareTo(payload.plannedAmount()) != 0) {
            throw new BusinessRuleException("Budget amount changes require a revision.", "BUDGET_REVISION_REQUIRED", HttpStatus.CONFLICT);
        }
        budget.update(payload.fiscalYear(), payload.periodType(), payload.periodMonth(),
                payload.departmentId().strip(), payload.plannedAmount(), payload.currencyCode(),
                payload.blocking() == null || payload.blocking(), payload.active() == null || payload.active());
        budget.configureRevisionApproval(payload.revisionApprovalRequired() == null || payload.revisionApprovalRequired());
        Budget saved = budgetRepository.save(budget);
        auditService.record("UPDATE", "BUDGET", saved.getId(), getCurrentUser(),
                "{\"year\":" + saved.getFiscalYear() + ",\"planned\":" + saved.getPlannedAmount() + "}", null);
        return toBudgetResponse(saved);
    }

    @Transactional
    public void deleteBudget(String id) {
        Budget budget = requireBudget(id);
        List<Encumbrance> encumbrances = encumbranceRepository.findByBudgetId(id);
        if (encumbrances.stream().anyMatch(item -> item.getStatus() == EncumbranceStatus.ACTIVE)) {
            throw new BusinessRuleException("لا يمكن حذف ميزانية عليها التزامات نشطة.", "BUDGET_HAS_ACTIVE_ENCUMBRANCES", HttpStatus.CONFLICT);
        }
        if (!encumbrances.isEmpty()) {
            throw new BusinessRuleException("لا يمكن حذف ميزانية لها سجل اعتمادات؛ يمكن إلغاء تفعيلها بدلاً من الحذف.", "BUDGET_HAS_ENCUMBRANCES", HttpStatus.CONFLICT);
        }
        budgetRepository.delete(budget);
        auditService.record("DELETE", "BUDGET", id, getCurrentUser(), "{\"year\":" + budget.getFiscalYear() + "}", null);
    }

    // ─── Budget status / encumbrances ─────────────────────────────────

    public List<BudgetApi.BudgetStatusResponse> status(Integer fiscalYear) {
        List<Budget> budgets = fiscalYear == null
                ? budgetRepository.findByActiveTrueOrderByFiscalYearDescPeriodMonthAsc()
                : budgetRepository.findByActiveTrueOrderByFiscalYearDescPeriodMonthAsc().stream()
                        .filter(item -> item.getFiscalYear() == fiscalYear).toList();
        return budgets.stream().map(this::toStatusResponse).toList();
    }

    public List<BudgetApi.EncumbranceResponse> listEncumbrances() {
        return encumbranceRepository.findAllByOrderByCommittedAtDesc().stream()
                .map(this::toEncumbranceResponse).toList();
    }

    public byte[] export(String locale) {
        var options = new ExcelExportOptions(locale, null);
        var messages = ExcelExportSupport.messages(translationService, options);
        var rows = status(null).stream().<List<?>>map(item -> List.of(
                item.departmentName() == null ? "" : item.departmentName(),
                item.fiscalYear(), periodText(item.periodType(), item.periodMonth()),
                item.plannedAmount(), item.committedAmount(), item.actualAmount(),
                item.availableAmount(), item.utilizationPercent() + "%", item.currencyCode(),
                ExcelExportSupport.text(messages, item.blocking() ? "export.value.yes" : "export.value.no"))).toList();
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = ExcelExportSupport.sheet(workbook, ExcelExportSupport.text(messages, "export.sheet.budgets"),
                    options.rightToLeft());
            var headers = List.of("department", "year", "period", "planned", "committed", "actual",
                    "available", "utilization", "currency", "blocking")
                    .stream().map(key -> ExcelExportSupport.text(messages, "export.column." + key)).toList();
            ExcelExportSupport.writeHeader(sheet, headers);
            var styles = ExcelExportSupport.styles(workbook);
            int rowIndex = 1;
            for (var values : rows) ExcelExportSupport.writeRow(sheet, rowIndex++, values, styles);
            ExcelExportSupport.finishTable(sheet, rowIndex - 1, headers.size(), "BudgetsTable", options);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create budget Excel workbook.", exception);
        }
    }

    private String periodText(BudgetPeriodType type, Integer month) {
        return type == BudgetPeriodType.MONTHLY ? type.name() + "-" + month : type.name();
    }

    // ─── Encumbrance lifecycle (invoked by procurement) ───────────────

    /**
     * Reserves budget for a purchase order on issue. Returns {@code null} when no
     * active budget matches the order department/date (no budget control applies).
     */
    @Transactional
    public BudgetApi.EncumbranceResponse encumberForOrder(String purchaseOrderId, String purchaseOrderNumber,
                                                           String departmentId, BigDecimal baseTotalAmount,
                                                           LocalDate poDate, String actor) {
        Budget budget = resolveBudget(departmentId, poDate);
        if (budget == null) return null;
        BigDecimal amount = baseTotalAmount == null ? BigDecimal.ZERO : baseTotalAmount;
        BigDecimal available = availableFor(budget);
        if (amount.compareTo(available) > 0 && budget.isBlocking()) {
            throw new BusinessRuleException("يتجاوز مبلغ أمر الشراء الميزانية المتاحة للقسم (" + amount
                    + " مقابل " + available + " " + budget.getCurrencyCode() + ").", "BUDGET_AVAILABILITY_BLOCKED", HttpStatus.CONFLICT);
        }
        Encumbrance encumbrance = encumbranceRepository.save(new Encumbrance(budget.getId(),
                purchaseOrderId, purchaseOrderNumber, amount, budget.getCurrencyCode()));
        auditService.record("ENCUMBER", "BUDGET", budget.getId(), actor,
                "{\"po\":\"" + purchaseOrderNumber + "\",\"amount\":" + amount + "}", null);
        return toEncumbranceResponse(encumbrance);
    }

    /** Records invoice spend against the order commitment (budget actuals). */
    @Transactional
    public void liquidateForInvoice(String purchaseOrderId, BigDecimal baseNetAmount, String actor) {
        if (purchaseOrderId == null) return;
        encumbranceRepository.findFirstByPurchaseOrderIdAndStatus(purchaseOrderId, EncumbranceStatus.ACTIVE)
                .ifPresent(encumbrance -> {
                    encumbrance.liquidate(baseNetAmount);
                    encumbranceRepository.save(encumbrance);
                    auditService.record("LIQUIDATE", "BUDGET", encumbrance.getBudgetId(), actor,
                            "{\"po\":\"" + encumbrance.getPurchaseOrderNumber() + "\",\"amount\":"
                                    + (baseNetAmount == null ? BigDecimal.ZERO : baseNetAmount) + "}", null);
                });
    }

    /** Returns the un-invoiced remainder of an order commitment to the budget. */
    @Transactional
    public void releaseForReceive(String purchaseOrderId, String actor) {
        releaseCommitment(purchaseOrderId, actor, "RELEASE_RECEIVED");
    }

    /** Returns the whole outstanding commitment when a purchase order is cancelled. */
    @Transactional
    public void releaseForCancel(String purchaseOrderId, String actor) {
        releaseCommitment(purchaseOrderId, actor, "RELEASE_CANCELLED");
    }

    private void releaseCommitment(String purchaseOrderId, String actor, String event) {
        encumbranceRepository.findFirstByPurchaseOrderIdAndStatus(purchaseOrderId, EncumbranceStatus.ACTIVE)
                .ifPresent(encumbrance -> {
                    encumbrance.release(BigDecimal.ZERO);
                    encumbranceRepository.save(encumbrance);
                    auditService.record(event, "BUDGET", encumbrance.getBudgetId(), actor,
                            "{\"po\":\"" + encumbrance.getPurchaseOrderNumber() + "\",\"released\":"
                                    + encumbrance.getReleasedAmount() + "}", null);
                });
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private Budget resolveBudget(String departmentId, LocalDate documentDate) {
        if (departmentId == null || departmentId.isBlank() || documentDate == null) return null;
        return budgetRepository.findByDepartmentIdAndActiveTrue(departmentId.strip()).stream()
                .filter(budget -> budget.getPeriodType() == BudgetPeriodType.MONTHLY
                        ? budget.getFiscalYear() == documentDate.getYear()
                            && budget.getPeriodMonth() != null
                            && budget.getPeriodMonth() == documentDate.getMonthValue()
                        : budget.getFiscalYear() == documentDate.getYear())
                .findFirst().orElse(null);
    }

    private BigDecimal availableFor(Budget budget) {
        List<Encumbrance> encumbrances = encumbranceRepository.findByBudgetId(budget.getId());
        BigDecimal committed = encumbrances.stream()
                .filter(item -> item.getStatus() == EncumbranceStatus.ACTIVE)
                .map(Encumbrance::getCommittedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actual = encumbrances.stream()
                .map(Encumbrance::getLiquidatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return budget.getPlannedAmount().subtract(committed).subtract(actual).max(BigDecimal.ZERO);
    }

    private void validatePeriod(BudgetApi.BudgetPayload payload) {
        BudgetPeriodType type = payload.periodType() == null ? BudgetPeriodType.ANNUAL : payload.periodType();
        if (type == BudgetPeriodType.MONTHLY
                && (payload.periodMonth() == null || payload.periodMonth() < 1 || payload.periodMonth() > 12)) {
            throw new BusinessRuleException("شهر الميزانية يجب أن يكون بين 1 و12 للميزانية الشهرية.", "BUDGET_PERIOD_MONTH_INVALID", HttpStatus.CONFLICT);
        }
    }

    private void requireDepartment(String departmentId) {
        if (departmentId == null || departmentId.isBlank()) {
            throw new BusinessRuleException("يجب اختيار القسم المرتبط بالميزانية.", "BUDGET_DEPARTMENT_REQUIRED", HttpStatus.CONFLICT);
        }
        if (departmentRepository.findById(departmentId.strip()).isEmpty()) {
            throw new BusinessRuleException("القسم المحدد غير موجود في التنظيم الإداري.", "BUDGET_DEPARTMENT_NOT_FOUND", HttpStatus.CONFLICT);
        }
    }

    private Budget requireBudget(String id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Budget not found: " + id, "BUDGET_NOT_FOUND"));
    }

    private Map<String, String> departmentNames(List<String> ids) {
        return ids.stream().distinct().collect(Collectors.toMap(id -> id, this::departmentName));
    }

    private String departmentName(String id) {
        return departmentRepository.findById(id).map(Department::getName).orElse(null);
    }

    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }

    private BudgetApi.BudgetResponse toBudgetResponse(Budget budget) {
        return new BudgetApi.BudgetResponse(budget.getId(), budget.getFiscalYear(), budget.getPeriodType(),
                budget.getPeriodMonth(), budget.getDepartmentId(), departmentName(budget.getDepartmentId()),
                budget.getPlannedAmount(), budget.getCurrencyCode(), budget.isBlocking(), budget.isActive(),
                budget.isRevisionApprovalRequired(), budget.getCurrentRevisionNumber(),
                budget.getCreatedAt(), budget.getUpdatedAt());
    }

    @Transactional
    public com.bemo.hr.budget.BudgetRevision reviseBudget(String budgetId, BigDecimal newAmount, String reason, String requestedBy) {
        if (newAmount == null || newAmount.signum() < 0) {
            throw new BusinessRuleException("Budget revision amount must be non-negative.", "BUDGET_REVISION_AMOUNT_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Budget revision reason is required.", "BUDGET_REVISION_REASON_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        Budget budget = budgetRepository.findByIdForUpdate(budgetId)
                .orElseThrow(() -> new NotFoundException("Budget not found: " + budgetId));
        if (budgetRevisionRepository.existsByBudgetIdAndStatus(budgetId, com.bemo.hr.budget.BudgetRevision.Status.PENDING)) {
            throw new BusinessRuleException("A budget revision is already pending.", "BUDGET_REVISION_PENDING", HttpStatus.CONFLICT);
        }
        List<com.bemo.hr.budget.BudgetRevision> existingRevisions = budgetRevisionRepository.findByBudgetIdOrderByRevisionNumberDesc(budgetId);
        int nextRevNo = existingRevisions.isEmpty() ? 1 : existingRevisions.get(0).getRevisionNumber() + 1;
        com.bemo.hr.budget.BudgetRevision revision = new com.bemo.hr.budget.BudgetRevision(
                budgetId, nextRevNo, budget.getPlannedAmount(), newAmount, reason, requestedBy,
                budget.isRevisionApprovalRequired()
        );
        if (revision.getStatus() == com.bemo.hr.budget.BudgetRevision.Status.APPROVED) {
            budget.applyApprovedRevision(nextRevNo, newAmount);
            budgetRepository.save(budget);
        }
        com.bemo.hr.budget.BudgetRevision saved = budgetRevisionRepository.save(revision);
        auditService.record("REQUEST_REVISION", "BUDGET", budgetId, requestedBy,
                "{\"revision\":" + nextRevNo + ",\"reason\":\"" + reason.replace("\"", "'") + "\"}", null);
        return saved;
    }

    public List<com.bemo.hr.budget.BudgetRevision> listRevisions(String budgetId) {
        requireBudget(budgetId);
        return budgetRevisionRepository.findByBudgetIdOrderByRevisionNumberDesc(budgetId);
    }

    @Transactional
    public com.bemo.hr.budget.BudgetRevision approveRevision(String budgetId, String revisionId, String actor) {
        Budget budget = budgetRepository.findByIdForUpdate(budgetId)
                .orElseThrow(() -> new NotFoundException("Budget not found: " + budgetId));
        com.bemo.hr.budget.BudgetRevision revision = budgetRevisionRepository.findById(revisionId)
                .filter(item -> item.getBudgetId().equals(budgetId))
                .orElseThrow(() -> new NotFoundException("Budget revision not found: " + revisionId));
        if (actor.equals(revision.getRequestedBy())) {
            throw new BusinessRuleException("Requester cannot approve the same budget revision.", "BUDGET_REVISION_SOD", HttpStatus.CONFLICT);
        }
        try { revision.approve(actor); }
        catch (IllegalStateException ex) { throw new BusinessRuleException(ex.getMessage(), "BUDGET_REVISION_STATE_INVALID", HttpStatus.CONFLICT); }
        budget.applyApprovedRevision(revision.getRevisionNumber(), revision.getNewAmount());
        budgetRepository.save(budget);
        com.bemo.hr.budget.BudgetRevision saved = budgetRevisionRepository.save(revision);
        auditService.record("APPROVE_REVISION", "BUDGET", budgetId, actor,
                "{\"revision\":" + revision.getRevisionNumber() + ",\"reason\":\"" + revision.getReason().replace("\"", "'") + "\"}", null);
        return saved;
    }

    @Transactional
    public com.bemo.hr.budget.BudgetRevision rejectRevision(String budgetId, String revisionId, String actor) {
        requireBudget(budgetId);
        com.bemo.hr.budget.BudgetRevision revision = budgetRevisionRepository.findById(revisionId)
                .filter(item -> item.getBudgetId().equals(budgetId))
                .orElseThrow(() -> new NotFoundException("Budget revision not found: " + revisionId));
        try { revision.reject(actor); }
        catch (IllegalStateException ex) { throw new BusinessRuleException(ex.getMessage(), "BUDGET_REVISION_STATE_INVALID", HttpStatus.CONFLICT); }
        com.bemo.hr.budget.BudgetRevision saved = budgetRevisionRepository.save(revision);
        auditService.record("REJECT_REVISION", "BUDGET", budgetId, actor,
                "{\"revision\":" + revision.getRevisionNumber() + ",\"reason\":\"" + revision.getReason().replace("\"", "'") + "\"}", null);
        return saved;
    }

    @Transactional
    public com.bemo.hr.budget.BudgetTransfer createTransfer(String transferNumber, String sourceBudgetId, String targetBudgetId, BigDecimal transferAmount, String reason) {
        com.bemo.hr.budget.BudgetTransfer transfer = new com.bemo.hr.budget.BudgetTransfer(transferNumber, sourceBudgetId, targetBudgetId, transferAmount, reason);
        return budgetTransferRepository.save(transfer);
    }

    @Transactional
    public com.bemo.hr.budget.BudgetTransfer approveTransfer(String transferId) {
        com.bemo.hr.budget.BudgetTransfer transfer = budgetTransferRepository.findById(transferId)
                .orElseThrow(() -> new NotFoundException("Budget transfer not found: " + transferId));
        transfer.approve();

        Budget source = budgetRepository.findById(transfer.getSourceBudgetId())
                .orElseThrow(() -> new NotFoundException("Source budget not found: " + transfer.getSourceBudgetId()));
        Budget target = budgetRepository.findById(transfer.getTargetBudgetId())
                .orElseThrow(() -> new NotFoundException("Target budget not found: " + transfer.getTargetBudgetId()));

        source.updatePlannedAmount(source.getPlannedAmount().subtract(transfer.getTransferAmount()));
        target.updatePlannedAmount(target.getPlannedAmount().add(transfer.getTransferAmount()));

        budgetRepository.save(source);
        budgetRepository.save(target);
        return budgetTransferRepository.save(transfer);
    }

    private BudgetApi.BudgetStatusResponse toStatusResponse(Budget budget) {
        List<Encumbrance> encumbrances = encumbranceRepository.findByBudgetId(budget.getId());
        BigDecimal committed = encumbrances.stream()
                .filter(item -> item.getStatus() == EncumbranceStatus.ACTIVE)
                .map(Encumbrance::getCommittedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actual = encumbrances.stream()
                .map(Encumbrance::getLiquidatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal available = budget.getPlannedAmount().subtract(committed).subtract(actual).max(BigDecimal.ZERO);
        BigDecimal utilization = budget.getPlannedAmount().signum() > 0
                ? committed.add(actual).multiply(new BigDecimal("100"))
                        .divide(budget.getPlannedAmount(), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new BudgetApi.BudgetStatusResponse(budget.getId(), budget.getFiscalYear(), budget.getPeriodType(),
                budget.getPeriodMonth(), budget.getDepartmentId(), departmentName(budget.getDepartmentId()),
                budget.getPlannedAmount(), committed, actual, available, utilization,
                budget.isBlocking(), budget.getCurrencyCode());
    }

    private BudgetApi.EncumbranceResponse toEncumbranceResponse(Encumbrance encumbrance) {
        return new BudgetApi.EncumbranceResponse(encumbrance.getId(), encumbrance.getBudgetId(),
                encumbrance.getPurchaseOrderId(), encumbrance.getPurchaseOrderNumber(),
                encumbrance.getDocumentType(), encumbrance.getStatus().name(),
                encumbrance.getCommittedAmount(), encumbrance.getLiquidatedAmount(),
                encumbrance.getReleasedAmount(), encumbrance.getCurrencyCode(),
                encumbrance.getCommittedAt(), encumbrance.getReleasedAt());
    }
}
