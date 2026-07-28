package com.bemo.hr.operations;

import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationsService {
    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    private final EmployeeAdvanceEntryRepository employeeAdvanceEntryRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final OperationsExcelExporter operationsExcelExporter;
    private final com.bemo.hr.audit.application.AuditService auditService;

    public byte[] export(com.bemo.hr.reporting.application.ExcelExportOptions options) {
        return operationsExcelExporter.export(snapshot(), options);
    }

    public long countStockMovements() {
        return stockMovementRepository.count();
    }

    public long countInventoryItems() {
        return inventoryItemRepository.count();
    }

    public long countLowStockItems() {
        return inventoryItemRepository.findAll().stream()
                .filter(item -> stockMovementRepository.balance(item.getId()).compareTo(BigDecimal.ZERO) == 0)
                .count();
    }

    public long countNegativeStockItems() {
        return inventoryItemRepository.findAll().stream()
                .filter(item -> stockMovementRepository.balance(item.getId()).compareTo(BigDecimal.ZERO) < 0)
                .count();
    }

    public long countPartnerLedgerEntries() {
        return partnerLedgerEntryRepository.count();
    }

    public long countActiveParties() {
        return businessPartyRepository.findAllByOrderByNameAsc().stream()
                .filter(BusinessParty::isActive)
                .count();
    }

    public OperationsApi.Snapshot snapshot() {
        var items = inventoryItemRepository.findAllByOrderByNameAsc();
        var itemMap = items.stream().collect(Collectors.toMap(InventoryItem::getId, Function.identity()));
        var parties = businessPartyRepository.findAllByOrderByNameAsc();
        var partyMap = parties.stream().collect(Collectors.toMap(BusinessParty::getId, Function.identity()));
        var employees = employeeRepository.findAllByOrderByFullNameAsc();
        var employeeMap = employees.stream().collect(Collectors.toMap(com.bemo.hr.employee.domain.Employee::getId, Function.identity()));
        var categories = itemCategoryRepository.findAll().stream().collect(Collectors.toMap(ItemCategory::getId, Function.identity()));
        var uoms = unitOfMeasureRepository.findAll().stream().collect(Collectors.toMap(UnitOfMeasure::getId, Function.identity()));

        var itemViews = items.stream().map(item -> itemView(item, stockMovementRepository.balance(item.getId()), categories, uoms)).toList();
        var movements = stockMovementRepository.findAllByOrderByOccurredAtDesc().stream().map(movement -> {
            var item = itemMap.get(movement.getItemId());
            var party = partyMap.get(movement.getPartyId());
            return new OperationsApi.StockMovementView(movement.getId(), movement.getItemId(),
                    item == null ? "—" : item.getCode(), item == null ? "—" : item.getName(), movement.getPartyId(),
                    party == null ? null : party.getName(), movement.getOperationType(), movement.getDocumentType(),
                    movement.getQuantityDelta(),
                    movement.getLossPercentage(), movement.getReferenceCode(), movement.getNote(), movement.getReason(),
                    movement.getOccurredAt(),
                    movement.getCreatedBy(), movement.getCreatedAt());
        }).toList();
        var balances = parties.stream().map(party -> new OperationsApi.PartyBalance(party.getId(), party.getCode(),
                party.getName(), party.getPartyType(), partnerLedgerEntryRepository.balance(party.getId()))).toList();
        var ledger = partnerLedgerEntryRepository.findAllByOrderByOccurredAtDesc().stream().map(entry -> {
            var party = partyMap.get(entry.getPartyId());
            return new OperationsApi.LedgerView(entry.getId(), entry.getPartyId(), party == null ? "—" : party.getName(),
                    entry.getEntryType(), entry.getAmountDelta(), entry.getReferenceCode(), entry.getNote(),
                    entry.getOccurredAt(), entry.getCreatedBy(), entry.getCreatedAt());
        }).toList();
        var advances = employeeAdvanceEntryRepository.findAllByOrderByOccurredAtDesc().stream().map(entry -> {
            var employee = employeeMap.get(entry.getEmployeeId());
            return new OperationsApi.AdvanceView(entry.getId(), entry.getEmployeeId(),
                    employee == null ? "—" : employee.getEmployeeCode(), employee == null ? "—" : employee.getFullName(),
                    entry.getAmountDelta(), employeeAdvanceEntryRepository.balance(entry.getEmployeeId()), entry.getEntryType(),
                    entry.getNote(), entry.getOccurredAt(), entry.getCreatedBy(), entry.getCreatedAt());
        }).toList();
        return new OperationsApi.Snapshot(itemViews, movements, balances, ledger, advances);
    }

    public List<OperationsApi.ItemCategoryView> listItemCategories() {
        return itemCategoryRepository.findByActiveTrueAndAppIdOrderByNameAsc(getCurrentAppId())
                .stream().map(this::categoryView).toList();
    }

    @Transactional
    public OperationsApi.ItemCategoryView createItemCategory(OperationsApi.ItemCategoryRequest request) {
        if (itemCategoryRepository.findByNameAndAppId(request.name().strip(), getCurrentAppId()).isPresent()) {
            throw new BusinessRuleException("Item category already exists.");
        }
        var entity = itemCategoryRepository.save(new ItemCategory(request.name(), request.description()));
        return categoryView(entity);
    }

    public List<OperationsApi.UnitOfMeasureView> listUnitOfMeasures() {
        return unitOfMeasureRepository.findByActiveTrueAndAppIdOrderByNameAsc(getCurrentAppId())
                .stream().map(this::uomView).toList();
    }

    @Transactional
    public OperationsApi.UnitOfMeasureView createUnitOfMeasure(OperationsApi.UnitOfMeasureRequest request) {
        if (unitOfMeasureRepository.findByNameAndAppId(request.name().strip(), getCurrentAppId()).isPresent()) {
            throw new BusinessRuleException("Unit of measure already exists.");
        }
        var entity = unitOfMeasureRepository.save(new UnitOfMeasure(request.name(), request.abbreviation(), request.description()));
        return uomView(entity);
    }

    public List<OperationsApi.NegativeBalanceView> getNegativeBalances() {
        var items = inventoryItemRepository.findAll();
        return items.stream()
                .map(item -> Map.entry(item, stockMovementRepository.balance(item.getId())))
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) < 0)
                .map(e -> new OperationsApi.NegativeBalanceView(
                        e.getKey().getId(), e.getKey().getCode(), e.getKey().getName(), e.getValue()))
                .toList();
    }

    @Transactional
    public OperationsApi.Snapshot createStockAdjustment(OperationsApi.AdjustmentRequest request, String actor) {
        if (request.quantityDelta().signum() == 0) {
            throw new BusinessRuleException("Adjustment quantity cannot be zero.");
        }
        requireItem(request.itemId());
        var sm = stockMovementRepository.save(new StockMovement(
                request.itemId(), null, "ADJUSTMENT", request.quantityDelta(),
                null, request.referenceCode(), null, request.occurredAt(), actor));
        sm.assignDocument("ADJUSTMENT", request.reason());
        auditService.record("STOCK_ADJUSTMENT", "STOCK_ITEM", request.itemId(), actor,
                "Stock adjustment qty: " + request.quantityDelta() + " reason: " + request.reason(), null);
        return snapshot();
    }

    @Transactional
    public OperationsApi.ItemView createItem(OperationsApi.ItemRequest request) {
        if (inventoryItemRepository.existsByCodeIgnoreCase(request.code())) throw new BusinessRuleException("Item code already exists.");
        var item = inventoryItemRepository.save(new InventoryItem(request.code(), request.name(), request.itemType(), request.unitCode()));
        if (request.categoryId() != null || request.uomId() != null) {
            item.assignMasterData(request.categoryId(), request.uomId());
        }
        return itemView(item, BigDecimal.ZERO, Map.of(), Map.of());
    }

    @Transactional
    public OperationsApi.ItemView updateItem(String id, OperationsApi.ItemRequest request) {
        var item = requireItem(id);
        if (request.version() == null || request.version() != item.getVersion()) throw new BusinessRuleException("This item changed. Refresh and retry.");
        if (inventoryItemRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) throw new BusinessRuleException("Item code already exists.");
        item.update(request.code(), request.name(), request.itemType(), request.unitCode(), request.active());
        if (request.categoryId() != null || request.uomId() != null) {
            item.assignMasterData(request.categoryId(), request.uomId());
        }
        return itemView(item, stockMovementRepository.balance(id), Map.of(), Map.of());
    }

    @Transactional
    public OperationsApi.Snapshot recordTransaction(OperationsApi.TransactionRequest request, String actor) {
        if (request.quantityDelta().signum() == 0 && request.amountDelta().signum() == 0) {
            throw new BusinessRuleException("Quantity and amount cannot both be zero.");
        }
        if (request.quantityDelta().signum() < 0) {
            throw new BusinessRuleException("Quantity must be a positive number.");
        }
        if (request.lossPercentage() != null && (request.lossPercentage().signum() < 0
                || request.lossPercentage().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BusinessRuleException("Loss percentage must be between 0 and 100.");
        }
        if (request.quantityDelta().signum() != 0) {
            if (request.itemId() == null || request.itemId().isBlank()) throw new BusinessRuleException("An inventory item is required for quantity movement.");
            requireItem(request.itemId());
            BigDecimal qty = request.quantityDelta().abs();
            String op = request.operationType() == null ? "" : request.operationType().toUpperCase();
            if (op.equals("PROCESSING_INTAKE") || op.equals("PROCESSING_DELIVERY")
                || op.equals("EXPORT_SALE") || op.equals("SORTING_SALE") || op.equals("DISPOSAL")) {
                qty = qty.negate();
            }
            var sm = stockMovementRepository.save(new StockMovement(request.itemId(), normalizeId(request.partyId()), request.operationType(),
                    qty, request.lossPercentage(), request.referenceCode(), request.note(), request.occurredAt(), actor));
            auditService.record("STOCK_MOVEMENT", "STOCK_ITEM", request.itemId(), actor, "Recorded stock movement " + op + " qty: " + qty, null);
        }
        if (request.amountDelta().signum() != 0) {
            var partyId = normalizeId(request.partyId());
            if (partyId == null) throw new BusinessRuleException("A business party is required for a financial movement.");
            requireParty(partyId);
            partnerLedgerEntryRepository.save(new PartnerLedgerEntry(partyId, request.operationType(), request.amountDelta(),
                    request.referenceCode(), request.note(), request.occurredAt(), actor));
            auditService.record("PARTNER_LEDGER_ENTRY", "BUSINESS_PARTY", partyId, actor, "Recorded partner financial entry amount: " + request.amountDelta(), null);
        }
        return snapshot();
    }

    @Transactional
    public OperationsApi.Snapshot recordAdvance(OperationsApi.AdvanceRequest request, String actor) {
        if (request.amountDelta().signum() == 0) throw new BusinessRuleException("Advance amount cannot be zero.");
        var employee = employeeRepository.findById(request.employeeId()).orElseThrow(() -> new NotFoundException("Employee not found."));
        var category = attendanceCategoryRepository.findById(employee.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Employee category not found."));
        if (!category.isAllowsEmployeeAdvances()) throw new BusinessRuleException("This employee category does not allow advances.");
        var adv = employeeAdvanceEntryRepository.save(new EmployeeAdvanceEntry(employee.getId(), request.amountDelta(),
                request.entryType(), request.note(), request.occurredAt(), actor));
        auditService.record("EMPLOYEE_ADVANCE", "EMPLOYEE", employee.getId(), actor, "Recorded advance for " + employee.getFullName() + " amount: " + request.amountDelta(), null);
        return snapshot();
    }

    public BigDecimal getAdvanceBalance(String employeeId) {
        return employeeAdvanceEntryRepository.balance(employeeId);
    }

    @Transactional
    public void recordAdvanceSettlement(String employeeId, BigDecimal amount, String note, java.time.Instant occurredAt, String actor) {
        employeeAdvanceEntryRepository.save(new EmployeeAdvanceEntry(employeeId, amount, "SETTLEMENT", note, occurredAt, actor));
    }

    private InventoryItem requireItem(String id) {
        return inventoryItemRepository.findById(id).orElseThrow(() -> new NotFoundException("Inventory item not found."));
    }
    private BusinessParty requireParty(String id) {
        return businessPartyRepository.findById(id).orElseThrow(() -> new NotFoundException("Business party not found."));
    }
    private String normalizeId(String id) { return id == null || id.isBlank() ? null : id; }
    private OperationsApi.ItemView itemView(InventoryItem item, BigDecimal balance) {
        return itemView(item, balance, itemCategoryRepository.findAll().stream().collect(Collectors.toMap(ItemCategory::getId, Function.identity())),
                unitOfMeasureRepository.findAll().stream().collect(Collectors.toMap(UnitOfMeasure::getId, Function.identity())));
    }
    private OperationsApi.ItemView itemView(InventoryItem item, BigDecimal balance,
                                            Map<String, ItemCategory> categoryMap,
                                            Map<String, UnitOfMeasure> uomMap) {
        var cat = item.getCategoryId() == null ? null : categoryMap.get(item.getCategoryId());
        var uom = item.getUomId() == null ? null : uomMap.get(item.getUomId());
        return new OperationsApi.ItemView(item.getId(), item.getCode(), item.getName(), item.getItemType(), item.getUnitCode(),
                item.getCategoryId(), cat == null ? null : cat.getName(),
                item.getUomId(), uom == null ? null : uom.getName(),
                item.isActive(), balance, item.getVersion(), item.getCreatedAt(), item.getUpdatedAt());
    }
    private OperationsApi.ItemCategoryView categoryView(ItemCategory c) {
        return new OperationsApi.ItemCategoryView(c.getId(), c.getName(), c.getDescription(), c.isActive(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
    private OperationsApi.UnitOfMeasureView uomView(UnitOfMeasure u) {
        return new OperationsApi.UnitOfMeasureView(u.getId(), u.getName(), u.getAbbreviation(), u.getDescription(), u.isActive(),
                u.getCreatedAt(), u.getUpdatedAt());
    }
    private String getCurrentAppId() {
        return TenantContext.require();
    }
}
