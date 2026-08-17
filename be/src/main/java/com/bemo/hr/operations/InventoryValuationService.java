package com.bemo.hr.operations;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.domain.*;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.numbering.DocumentNumberService;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryValuationService {
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final InventoryValuationPolicyRepository inventoryValuationPolicyRepository;
    private final InventoryCostLayerRepository inventoryCostLayerRepository;
    private final InventoryMovementCostRepository inventoryMovementCostRepository;
    private final InventoryRevaluationRepository inventoryRevaluationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final FiscalPeriodGuard fiscalPeriodGuard;
    private final DocumentNumberService documentNumberService;
    private final AuditService auditService;
    @Value("${hr.company-zone:Africa/Cairo}")
    private String companyZone;

    public BigDecimal getItemUnitCost(String itemId) {
        BigDecimal qty = stockMovementRepository.balance(itemId);
        if (qty == null || qty.signum() <= 0) return BigDecimal.ZERO;
        return inventoryValue(itemId).divide(qty, 4, RoundingMode.HALF_UP);
    }

    public OperationsApi.ValuationPolicyView policy() {
        return toPolicy(inventoryValuationPolicyRepository.findByAppId(TenantContext.require()).orElse(null));
    }

    @Transactional
    public OperationsApi.ValuationPolicyView updatePolicy(OperationsApi.ValuationPolicyRequest request, String actor) {
        InventoryValuationPolicy policy = inventoryValuationPolicyRepository.findByAppId(TenantContext.require())
                .orElseGet(InventoryValuationPolicy::new);
        if (request.version() != null && request.version() != policy.getVersion()) {
            throw conflict("Inventory valuation settings changed. Refresh and retry.", "INV_VAL_POLICY_VERSION_CONFLICT");
        }
        validateAccounts(request);
        policy.update(request.valuationMethod(), request.inventoryAccountId(), request.receiptOffsetAccountId(),
                request.cogsAccountId(), request.adjustmentAccountId(), request.glPostingEnabled(),
                request.allowBackdatedPosting());
        policy = inventoryValuationPolicyRepository.save(policy);
        auditService.record("INVENTORY_VALUATION_POLICY_UPDATE", "INVENTORY_VALUATION_POLICY", policy.getId(), actor,
                "{\"method\":\"" + policy.getValuationMethod() + "\",\"glPostingEnabled\":" + policy.isGlPostingEnabled() + "}", null);
        return toPolicy(policy);
    }

    @Transactional
    public InventoryMovementCost valueMovement(StockMovement movement, BigDecimal requestedUnitCost, String actor) {
        InventoryMovementCost replay = inventoryMovementCostRepository.findByMovementId(movement.getId()).orElse(null);
        if (replay != null) return replay;
        inventoryItemRepository.findByIdForUpdate(movement.getItemId())
                .orElseThrow(() -> conflict("Inventory item not found.", "OPS_ITEM_NOT_FOUND"));
        InventoryValuationPolicy policy = inventoryValuationPolicyRepository.findByAppId(TenantContext.require())
                .orElseGet(InventoryValuationPolicy::new);
        if (!policy.isAllowBackdatedPosting()
                && inventoryMovementCostRepository.existsByItemIdAndOccurredAtAfter(movement.getItemId(), movement.getOccurredAt())) {
            throw conflict("Backdated inventory posting requires an enabled policy and a controlled rebuild.", "INV_VAL_BACKDATED_BLOCKED");
        }
        BigDecimal quantity = movement.getQuantityDelta();
        CostResult result = quantity.signum() > 0
                ? valueReceipt(policy, movement, requestedUnitCost)
                : valueIssue(policy, movement, quantity.abs());
        FiscalPeriod period = result.totalCost().signum() == 0
                ? null
                : fiscalPeriodGuard.requireOpen(localDate(movement.getOccurredAt()));
        BigDecimal valueEffect = result.totalCost().setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(quantity.signum()));
        InventoryMovementCost cost = new InventoryMovementCost(movement.getId(), movement.getItemId(),
                policy.getValuationMethod(), quantity, result.unitCost(), valueEffect, result.explanation(), movement.getOccurredAt());
        if (policy.isGlPostingEnabled() && valueEffect.signum() != 0) {
            cost.linkJournal(postMovementJournal(policy, movement, valueEffect, period, actor));
        }
        cost = inventoryMovementCostRepository.save(cost);
        auditService.record("INVENTORY_MOVEMENT_VALUED", "STOCK_MOVEMENT", movement.getId(), actor,
                "{\"method\":\"" + policy.getValuationMethod() + "\",\"valueEffect\":" + valueEffect + "}", null);
        return cost;
    }

    public OperationsApi.ValuationReport report() {
        List<InventoryItem> items = inventoryItemRepository.findAllByOrderByNameAsc();
        Map<String, InventoryItem> itemMap = items.stream().collect(Collectors.toMap(InventoryItem::getId, Function.identity()));
        List<OperationsApi.ItemValuationView> itemViews = items.stream().map(item -> itemValuation(item)).toList();
        List<OperationsApi.MovementCostView> costs = inventoryMovementCostRepository.findAllByOrderByOccurredAtDesc().stream()
                .map(cost -> toMovementCost(cost, itemMap.get(cost.getItemId()))).toList();
        BigDecimal total = itemViews.stream().map(OperationsApi.ItemValuationView::inventoryValue)
                .reduce(ZERO_MONEY, BigDecimal::add);
        return new OperationsApi.ValuationReport(policy(), total, itemViews, costs);
    }

    public OperationsApi.MovementCostView movementCost(String movementId) {
        InventoryMovementCost cost = inventoryMovementCostRepository.findByMovementId(movementId)
                .orElseThrow(() -> new BusinessRuleException("Movement cost was not found.", "INV_VAL_MOVEMENT_COST_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toMovementCost(cost, inventoryItemRepository.findById(cost.getItemId()).orElse(null));
    }

    @Transactional
    public OperationsApi.RevaluationView revalue(OperationsApi.RevaluationRequest request, String actor) {
        InventoryRevaluation replay = inventoryRevaluationRepository.findByOperationId(request.operationId()).orElse(null);
        if (replay != null) return toRevaluation(replay);
        InventoryItem item = inventoryItemRepository.findByIdForUpdate(request.itemId())
                .orElseThrow(() -> conflict("Inventory item not found.", "OPS_ITEM_NOT_FOUND"));
        InventoryValuationPolicy policy = inventoryValuationPolicyRepository.findByAppId(TenantContext.require())
                .orElseGet(InventoryValuationPolicy::new);
        FiscalPeriod period = fiscalPeriodGuard.requireAdjustment(localDate(request.occurredAt()));
        BigDecimal quantity = stockMovementRepository.balance(item.getId());
        if (quantity.signum() <= 0)
            throw conflict("Only positive on-hand inventory can be revalued.", "INV_VAL_REVALUE_NO_STOCK");
        BigDecimal oldValue = inventoryValue(item.getId());
        BigDecimal newValue = quantity.multiply(request.newUnitCost()).setScale(2, RoundingMode.HALF_UP);
        InventoryRevaluation revaluation = new InventoryRevaluation(item.getId(), request.operationId(), quantity,
                oldValue, newValue, request.reason(), request.occurredAt(), actor);
        if (revaluation.getValueDifference().signum() == 0) {
            throw conflict("The requested revaluation does not change inventory value.", "INV_VAL_REVALUE_NO_CHANGE");
        }
        if (policy.getValuationMethod() == InventoryValuationPolicy.Method.FIFO) {
            inventoryCostLayerRepository.findOpenForUpdate(item.getId()).forEach(layer -> layer.revalue(request.newUnitCost()));
        }
        if (policy.isGlPostingEnabled()) {
            revaluation.linkJournal(postRevaluationJournal(policy, item, revaluation.getValueDifference(),
                    localDate(request.occurredAt()), period, actor));
        }
        revaluation = inventoryRevaluationRepository.save(revaluation);
        auditService.record("INVENTORY_REVALUATION", "INVENTORY_ITEM", item.getId(), actor,
                "{\"operationId\":\"" + request.operationId() + "\",\"difference\":" + revaluation.getValueDifference() + "}", null);
        return toRevaluation(revaluation);
    }

    private CostResult valueReceipt(InventoryValuationPolicy policy, StockMovement movement, BigDecimal requestedUnitCost) {
        BigDecimal unitCost = requestedUnitCost;
        if (unitCost == null || unitCost.signum() <= 0) {
            BigDecimal priorStock = stockMovementRepository.balance(movement.getItemId())
                    .subtract(movement.getQuantityDelta());
            unitCost = currentAverage(movement.getItemId(), priorStock);
        }
        if (unitCost.signum() <= 0 && policy.isGlPostingEnabled()) {
            throw conflict("A positive unit cost is required for the first receipt or positive adjustment.", "INV_VAL_UNIT_COST_REQUIRED");
        }
        unitCost = unitCost.setScale(6, RoundingMode.HALF_UP);
        if (policy.getValuationMethod() == InventoryValuationPolicy.Method.FIFO) {
            inventoryCostLayerRepository.save(new InventoryCostLayer(movement.getItemId(), movement.getId(),
                    movement.getOccurredAt(), movement.getQuantityDelta(), unitCost));
        }
        return new CostResult(unitCost, movement.getQuantityDelta().multiply(unitCost),
                policy.getValuationMethod() + " receipt: " + movement.getQuantityDelta() + " × " + unitCost
                        + (unitCost.signum() == 0 ? " (cost pending until valuation policy is configured)" : ""));
    }

    private CostResult valueIssue(InventoryValuationPolicy policy, StockMovement movement, BigDecimal requestedQuantity) {
        BigDecimal priorStock = stockMovementRepository.balance(movement.getItemId()).add(requestedQuantity);
        if (priorStock.compareTo(requestedQuantity) < 0) {
            throw conflict("Inventory issue would create a negative stock balance.", "INV_VAL_NEGATIVE_STOCK");
        }
        if (policy.getValuationMethod() == InventoryValuationPolicy.Method.WEIGHTED_AVERAGE) {
            BigDecimal unitCost = currentAverage(movement.getItemId(), priorStock);
            if (unitCost.signum() <= 0)
                throw conflict("Inventory has no valued cost available for issue.", "INV_VAL_COST_UNAVAILABLE");
            return new CostResult(unitCost, requestedQuantity.multiply(unitCost),
                    "WEIGHTED_AVERAGE issue: " + requestedQuantity + " × current average " + unitCost);
        }
        BigDecimal remaining = requestedQuantity;
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder explanation = new StringBuilder("FIFO issue: ");
        for (InventoryCostLayer layer : inventoryCostLayerRepository.findOpenForUpdate(movement.getItemId())) {
            if (remaining.signum() == 0) break;
            BigDecimal consumed = layer.consume(remaining);
            total = total.add(consumed.multiply(layer.getUnitCost()));
            remaining = remaining.subtract(consumed);
            explanation.append(consumed).append(" @ ").append(layer.getUnitCost()).append("; ");
        }
        if (remaining.signum() > 0) {
            BigDecimal openingCost = currentAverage(movement.getItemId(), priorStock);
            if (openingCost.signum() <= 0)
                throw conflict("FIFO layers do not cover the requested issue.", "INV_VAL_FIFO_LAYER_SHORTAGE");
            total = total.add(remaining.multiply(openingCost));
            explanation.append(remaining).append(" @ opening cost ").append(openingCost);
        }
        BigDecimal unitCost = total.divide(requestedQuantity, 6, RoundingMode.HALF_UP);
        return new CostResult(unitCost, total, explanation.toString());
    }

    private BigDecimal currentAverage(String itemId, BigDecimal quantityBase) {
        if (quantityBase == null || quantityBase.signum() <= 0) return BigDecimal.ZERO;
        return inventoryValue(itemId).divide(quantityBase, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal inventoryValue(String itemId) {
        return inventoryMovementCostRepository.inventoryValue(itemId)
                .add(inventoryRevaluationRepository.revaluationValue(itemId));
    }

    private String postMovementJournal(InventoryValuationPolicy policy, StockMovement movement, BigDecimal valueEffect,
                                       FiscalPeriod period, String actor) {
        String offset = movement.getOperationType().contains("ADJUSTMENT")
                ? policy.getAdjustmentAccountId()
                : movement.getOperationType().equals("CUSTOMER_RETURN") ? policy.getCogsAccountId()
                  : valueEffect.signum() > 0 ? policy.getReceiptOffsetAccountId() : policy.getCogsAccountId();
        return postJournal(policy.getInventoryAccountId(), offset, valueEffect, movement.getPartyId(),
                "Inventory movement " + movement.getId(), movement.getReferenceCode(), localDate(movement.getOccurredAt()), period, actor);
    }

    private String postRevaluationJournal(InventoryValuationPolicy policy, InventoryItem item, BigDecimal difference,
                                          LocalDate date, FiscalPeriod period, String actor) {
        return postJournal(policy.getInventoryAccountId(), policy.getAdjustmentAccountId(), difference, null,
                "Inventory revaluation " + item.getCode(), item.getCode(), date, period, actor);
    }

    private String postJournal(String inventoryAccountId, String offsetAccountId, BigDecimal inventoryEffect,
                               String partyId, String description, String reference, LocalDate date,
                               FiscalPeriod period, String actor) {
        requirePostingAccount(inventoryAccountId);
        requirePostingAccount(offsetAccountId);
        BigDecimal amount = inventoryEffect.abs().setScale(2, RoundingMode.HALF_UP);
        JournalEntry entry = new JournalEntry(documentNumberService.next("INVENTORY_VALUATION", "INV", date),
                date, description, reference, period.getId());
        entry.setCurrency("EGP");
        entry.assignCreator(actor);
        entry.approve("SYSTEM_APPROVER");
        entry.post(actor);
        entry = journalEntryRepository.save(entry);
        boolean increase = inventoryEffect.signum() > 0;
        journalEntryLineRepository.save(new JournalEntryLine(entry.getId(), inventoryAccountId, partyId,
                increase ? amount : ZERO_MONEY, increase ? ZERO_MONEY : amount, description));
        journalEntryLineRepository.save(new JournalEntryLine(entry.getId(), offsetAccountId, partyId,
                increase ? ZERO_MONEY : amount, increase ? amount : ZERO_MONEY, description));
        return entry.getId();
    }

    private void validateAccounts(OperationsApi.ValuationPolicyRequest request) {
        if (!request.glPostingEnabled()) return;
        requirePostingAccount(request.inventoryAccountId());
        requirePostingAccount(request.receiptOffsetAccountId());
        requirePostingAccount(request.cogsAccountId());
        requirePostingAccount(request.adjustmentAccountId());
    }

    private Account requirePostingAccount(String id) {
        if (id == null || id.isBlank())
            throw conflict("All inventory GL accounts are required when posting is enabled.", "INV_VAL_GL_ACCOUNTS_REQUIRED");
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> conflict("Configured inventory GL account was not found.", "INV_VAL_GL_ACCOUNT_NOT_FOUND"));
        if (account.isHeader() || !account.isActive())
            throw conflict("Inventory GL accounts must be active posting accounts.", "INV_VAL_GL_ACCOUNT_NOT_POSTING");
        return account;
    }

    private OperationsApi.ItemValuationView itemValuation(InventoryItem item) {
        BigDecimal onHand = stockMovementRepository.balance(item.getId());
        BigDecimal valuedQuantity = inventoryMovementCostRepository.valuedQuantity(item.getId());
        BigDecimal value = inventoryValue(item.getId());
        BigDecimal average = onHand.signum() == 0 ? BigDecimal.ZERO : value.divide(onHand, 6, RoundingMode.HALF_UP);
        return new OperationsApi.ItemValuationView(item.getId(), item.getCode(), item.getName(), onHand,
                valuedQuantity, value, average, onHand.subtract(valuedQuantity));
    }

    private OperationsApi.MovementCostView toMovementCost(InventoryMovementCost cost, InventoryItem item) {
        return new OperationsApi.MovementCostView(cost.getId(), cost.getMovementId(), cost.getItemId(),
                item == null ? "—" : item.getCode(), item == null ? "—" : item.getName(), cost.getValuationMethod(),
                cost.getQuantityEffect(), cost.getUnitCost(), cost.getValueEffect(), cost.getJournalEntryId(),
                cost.getExplanation(), cost.getOccurredAt(), cost.getCreatedAt());
    }

    private OperationsApi.ValuationPolicyView toPolicy(InventoryValuationPolicy policy) {
        if (policy == null)
            return new OperationsApi.ValuationPolicyView(null, InventoryValuationPolicy.Method.WEIGHTED_AVERAGE,
                    null, null, null, null, false, false, 0, null, null);
        return new OperationsApi.ValuationPolicyView(policy.getId(), policy.getValuationMethod(),
                policy.getInventoryAccountId(), policy.getReceiptOffsetAccountId(), policy.getCogsAccountId(),
                policy.getAdjustmentAccountId(), policy.isGlPostingEnabled(), policy.isAllowBackdatedPosting(),
                policy.getVersion(), policy.getCreatedAt(), policy.getUpdatedAt());
    }

    private OperationsApi.RevaluationView toRevaluation(InventoryRevaluation value) {
        return new OperationsApi.RevaluationView(value.getId(), value.getItemId(), value.getOperationId(),
                value.getQuantityOnHand(), value.getOldValue(), value.getNewValue(), value.getValueDifference(),
                value.getReason(), value.getJournalEntryId(), value.getOccurredAt(), value.getCreatedBy(), value.getCreatedAt());
    }

    private LocalDate localDate(Instant instant) {
        return instant.atZone(ZoneId.of(companyZone)).toLocalDate();
    }

    private BusinessRuleException conflict(String message, String code) {
        return new BusinessRuleException(message, code, HttpStatus.CONFLICT);
    }

    private record CostResult(BigDecimal unitCost, BigDecimal totalCost, String explanation) {
    }
}
