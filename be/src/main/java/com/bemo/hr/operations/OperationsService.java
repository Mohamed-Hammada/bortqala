package com.bemo.hr.operations;

import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationsService {
    public record ValuedMovement(String movementId, BigDecimal unitCost, BigDecimal totalCost, String journalEntryId) { }
    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryMovementCostRepository inventoryMovementCostRepository;
    private final PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    private final EmployeeAdvanceEntryRepository employeeAdvanceEntryRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final UnitConversionRepository unitConversionRepository;
    private final OperationsExcelExporter operationsExcelExporter;
    private final com.bemo.hr.audit.application.AuditService auditService;
    private final InventoryValuationService inventoryValuationService;
    private final com.bemo.hr.operations.application.WarehouseInventoryService warehouseInventoryService;
    private final com.bemo.hr.operations.application.ItemLotSerialService itemLotSerialService;

    public byte[] export(com.bemo.hr.reporting.application.ExcelExportOptions options) {
        return operationsExcelExporter.export(snapshot(), inventoryValuationService.report(), options);
    }

    public long countStockMovements() {
        return stockMovementRepository.count();
    }

    public long countInventoryItems() {
        return inventoryItemRepository.count();
    }

    public List<OperationsApi.ReorderAlertView> reorderAlerts() {
        return inventoryItemRepository.findAllByOrderByNameAsc().stream()
                .filter(InventoryItem::isActive)
                .map(item -> {
                    BigDecimal balance = stockMovementRepository.balance(item.getId());
                    return new OperationsApi.ReorderAlertView(item.getId(), item.getCode(), item.getName(), balance,
                            item.getReorderPoint(), item.getReorderQuantity(), item.getReorderPoint().subtract(balance).max(BigDecimal.ZERO));
                })
                .filter(alert -> alert.reorderPoint().signum() > 0)
                .filter(alert -> alert.currentBalance().compareTo(alert.reorderPoint()) <= 0)
                .toList();
    }

    public OperationsApi.ItemView inventoryItem(String id) {
        return itemView(requireItem(id), stockMovementRepository.balance(id));
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
                    movement.getPurchaseOrderNo(), movement.getReceiptNo(), movement.getDeliveryNoteNo(),
                    movement.getInvoiceNo(), movement.getVoucherNo(), movement.getExternalRef(), movement.getWarehouse(),
                    movement.getAttachmentName(), movement.getAttachmentContentType(), movement.getAttachmentSize(),
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
            throw new BusinessRuleException("Item category already exists.", "OPS_ITEM_CATEGORY_EXISTS", HttpStatus.CONFLICT);
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
            throw new BusinessRuleException("Unit of measure already exists.", "OPS_UOM_EXISTS", HttpStatus.CONFLICT);
        }
        var entity = unitOfMeasureRepository.save(new UnitOfMeasure(request.name(), request.abbreviation(), request.description()));
        return uomView(entity);
    }

    public List<OperationsApi.UnitConversionView> listUnitConversions() {
        var conversions = unitConversionRepository.findAllByOrderByFromUomId();
        var uoms = unitOfMeasureRepository.findAll().stream().collect(Collectors.toMap(UnitOfMeasure::getId, Function.identity()));
        return conversions.stream().map(c -> {
            var from = uoms.get(c.getFromUomId());
            var to = uoms.get(c.getToUomId());
            return new OperationsApi.UnitConversionView(c.getId(), c.getFromUomId(),
                    from == null ? "—" : from.getName(),
                    c.getToUomId(), to == null ? "—" : to.getName(),
                    c.getFactor(), c.getCreatedAt());
        }).toList();
    }

    @Transactional
    public OperationsApi.UnitConversionView createUnitConversion(OperationsApi.UnitConversionRequest request, String actor) {
        if (unitConversionRepository.findByFromUomIdAndToUomId(request.fromUomId(), request.toUomId()).isPresent()) {
            throw new BusinessRuleException("Conversion already exists between these units.", "OPS_CONVERSION_EXISTS", HttpStatus.CONFLICT);
        }
        if (request.factor().signum() <= 0) {
            throw new BusinessRuleException("Conversion factor must be positive.", "OPS_CONVERSION_FACTOR_POSITIVE", HttpStatus.CONFLICT);
        }
        var entity = unitConversionRepository.save(new UnitConversion(request.fromUomId(), request.toUomId(), request.factor(), actor));
        var uoms = unitOfMeasureRepository.findAll().stream().collect(Collectors.toMap(UnitOfMeasure::getId, Function.identity()));
        var from = uoms.get(entity.getFromUomId());
        var to = uoms.get(entity.getToUomId());
        auditService.record("CREATE", "UNIT_CONVERSION", entity.getId(), actor,
                "Conversion: " + (from == null ? entity.getFromUomId() : from.getName())
                        + " -> " + (to == null ? entity.getToUomId() : to.getName()) + " = " + entity.getFactor(), null);
        return new OperationsApi.UnitConversionView(entity.getId(), entity.getFromUomId(),
                from == null ? "—" : from.getName(),
                entity.getToUomId(), to == null ? "—" : to.getName(),
                entity.getFactor(), entity.getCreatedAt());
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
        if (!request.approved()) {
            throw new BusinessRuleException("An authorized approval is required for inventory adjustments.", "OPS_ADJUSTMENT_APPROVAL_REQUIRED", HttpStatus.CONFLICT);
        }
        if (request.quantityDelta().signum() == 0) {
            throw new BusinessRuleException("Adjustment quantity cannot be zero.", "OPS_ADJUSTMENT_QTY_ZERO", HttpStatus.CONFLICT);
        }
        requireItem(request.itemId());
        if (stockMovementRepository.balance(request.itemId()).add(request.quantityDelta()).signum() < 0) {
            throw new BusinessRuleException("Inventory adjustment cannot create a negative balance.", "OPS_ADJUSTMENT_NEGATIVE_BALANCE", HttpStatus.CONFLICT);
        }
        var sm = stockMovementRepository.save(new StockMovement(
                request.itemId(), null, "ADJUSTMENT", request.quantityDelta(),
                null, request.referenceCode(), null, request.occurredAt(), actor));
        sm.assignDocument("ADJUSTMENT", request.reason());
        inventoryValuationService.valueMovement(sm, null, actor);
        auditService.record("STOCK_ADJUSTMENT", "STOCK_ITEM", request.itemId(), actor,
                "Stock adjustment qty: " + request.quantityDelta() + " reason: " + request.reason(), null);
        return snapshot();
    }

    @Transactional
    public OperationsApi.ItemView createItem(OperationsApi.ItemRequest request) {
        if (inventoryItemRepository.existsByCodeIgnoreCase(request.code())) throw new BusinessRuleException("Item code already exists.", "OPS_ITEM_CODE_EXISTS", HttpStatus.CONFLICT);
        var item = inventoryItemRepository.save(new InventoryItem(request.code(), request.name(), request.itemType(), request.unitCode()));
        item.configureReorder(request.reorderPoint(), request.reorderQuantity());
        if (request.categoryId() != null || request.uomId() != null) {
            item.assignMasterData(request.categoryId(), request.uomId());
        }
        return itemView(item, BigDecimal.ZERO, Map.of(), Map.of());
    }

    @Transactional
    public OperationsApi.ItemView updateItem(String id, OperationsApi.ItemRequest request) {
        var item = requireItem(id);
        if (request.version() == null || request.version() != item.getVersion()) throw new BusinessRuleException("This item changed. Refresh and retry.", "OPS_ITEM_VERSION_CONFLICT", HttpStatus.CONFLICT);
        if (inventoryItemRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) throw new BusinessRuleException("Item code already exists.", "OPS_ITEM_CODE_EXISTS", HttpStatus.CONFLICT);
        item.update(request.code(), request.name(), request.itemType(), request.unitCode(), request.active());
        item.configureReorder(request.reorderPoint(), request.reorderQuantity());
        if (request.categoryId() != null || request.uomId() != null) {
            item.assignMasterData(request.categoryId(), request.uomId());
        }
        return itemView(item, stockMovementRepository.balance(id), Map.of(), Map.of());
    }

    @Transactional
    public OperationsApi.Snapshot recordTransaction(OperationsApi.TransactionRequest request, String actor) {
        if (request.quantityDelta().signum() == 0 && request.amountDelta().signum() == 0) {
            throw new BusinessRuleException("Quantity and amount cannot both be zero.", "OPS_MOVEMENT_QTY_AMOUNT_ZERO", HttpStatus.CONFLICT);
        }
        if (request.quantityDelta().signum() < 0) {
            throw new BusinessRuleException("Quantity must be a positive number.", "OPS_MOVEMENT_QTY_POSITIVE", HttpStatus.CONFLICT);
        }
        if (request.lossPercentage() != null && (request.lossPercentage().signum() < 0
                || request.lossPercentage().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BusinessRuleException("Loss percentage must be between 0 and 100.", "OPS_MOVEMENT_LOSS_PERCENT_RANGE", HttpStatus.CONFLICT);
        }
        String op = request.operationType() == null ? "" : request.operationType().toUpperCase();
        validateDocumentReferences(request, op);
        if (request.quantityDelta().signum() != 0) {
            if (request.itemId() == null || request.itemId().isBlank()) throw new BusinessRuleException("An inventory item is required for quantity movement.", "OPS_MOVEMENT_ITEM_REQUIRED", HttpStatus.CONFLICT);
            requireItem(request.itemId());
            BigDecimal qty = request.quantityDelta().abs();
            if (op.equals("PROCESSING_INTAKE") || op.equals("PROCESSING_DELIVERY")
                || op.equals("EXPORT_SALE") || op.equals("SORTING_SALE") || op.equals("DISPOSAL")) {
                qty = qty.negate();
            }
            var sm = stockMovementRepository.save(new StockMovement(request.itemId(), normalizeId(request.partyId()), request.operationType(),
                    qty, request.lossPercentage(), request.referenceCode(), request.note(), request.occurredAt(), actor));
            sm.assignDocument(request.documentType() != null && !request.documentType().isBlank()
                    ? request.documentType().strip().toUpperCase()
                    : defaultDocumentType(op), request.reason());
            sm.assignReferences(request.purchaseOrderNo(), request.receiptNo(), request.deliveryNoteNo(), request.invoiceNo(),
                    request.voucherNo(), request.externalRef(), request.warehouse(), request.attachmentName(),
                    request.attachmentContentType(), request.attachmentSize());
            inventoryValuationService.valueMovement(sm, request.unitCost(), actor);
            auditService.record("STOCK_MOVEMENT", "STOCK_ITEM", request.itemId(), actor, "Recorded stock movement " + op + " qty: " + qty, null);
        }
        if (request.amountDelta().signum() != 0) {
            var partyId = normalizeId(request.partyId());
            if (partyId == null) throw new BusinessRuleException("A business party is required for a financial movement.", "OPS_MOVEMENT_PARTY_REQUIRED", HttpStatus.CONFLICT);
            requireParty(partyId);
            partnerLedgerEntryRepository.save(new PartnerLedgerEntry(partyId, request.operationType(), request.amountDelta(),
                    request.referenceCode(), request.note(), request.occurredAt(), actor));
            auditService.record("PARTNER_LEDGER_ENTRY", "BUSINESS_PARTY", partyId, actor, "Recorded partner financial entry amount: " + request.amountDelta(), null);
        }
        return snapshot();
    }

    @Transactional
    public void recordGoodsReceipt(String itemId, String supplierId, BigDecimal acceptedQuantity,
                                   BigDecimal unitCost, String grnNumber, String note, Instant occurredAt, String actor) {
        recordGoodsReceipt(itemId, supplierId, null, acceptedQuantity, unitCost, grnNumber, note, occurredAt, actor);
    }

    @Transactional
    public void recordGoodsReceipt(String itemId, String supplierId, String warehouseId, BigDecimal acceptedQuantity,
                                   BigDecimal unitCost, String grnNumber, String note, Instant occurredAt, String actor) {
        recordGoodsReceipt(itemId, supplierId, warehouseId, acceptedQuantity, unitCost, grnNumber, null, note, occurredAt, actor);
    }

    @Transactional
    public void recordGoodsReceipt(String itemId, String supplierId, String warehouseId, BigDecimal acceptedQuantity,
                                   BigDecimal unitCost, String grnNumber, String lotNumber, String note, Instant occurredAt, String actor) {
        requireItem(itemId);
        if (acceptedQuantity == null || acceptedQuantity.signum() <= 0) {
            throw new BusinessRuleException("Accepted goods-receipt quantity must be positive.", "OPS_GRN_ACCEPTED_POSITIVE", HttpStatus.CONFLICT);
        }
        var movement = stockMovementRepository.save(new StockMovement(itemId, normalizeId(supplierId),
                "PURCHASE_RECEIPT", acceptedQuantity, null, grnNumber, note, occurredAt, actor));
        movement.assignDocument("GOODS_RECEIPT", "Accepted quantity posted from supplier receipt");
        inventoryValuationService.valueMovement(movement, unitCost, actor);
        if (warehouseId != null && !warehouseId.isBlank()) {
            warehouseInventoryService.receiveAvailableStock(warehouseId, itemId, acceptedQuantity);
            if (lotNumber != null && !lotNumber.isBlank()) {
                itemLotSerialService.receive(itemId, warehouseId, lotNumber, null, acceptedQuantity, grnNumber, null, null);
            }
        }
        auditService.record("STOCK_MOVEMENT", "STOCK_ITEM", itemId, actor,
                "Goods receipt " + grnNumber + " accepted qty: " + acceptedQuantity, null);
    }

    @Transactional
    public void recordSupplierReturn(String itemId, String supplierId, BigDecimal returnQuantity,
                                     BigDecimal unitCost, String returnNumber, String note, Instant occurredAt, String actor) {
        requireItem(itemId);
        if (returnQuantity == null || returnQuantity.signum() <= 0) {
            throw new BusinessRuleException("Supplier return quantity must be positive.", "OPS_RETURN_QUANTITY_POSITIVE", HttpStatus.CONFLICT);
        }
        BigDecimal currentBalance = stockMovementRepository.balance(itemId);
        if (currentBalance.subtract(returnQuantity).signum() < 0) {
            throw new BusinessRuleException("Supplier return cannot create a negative stock balance.", "OPS_RETURN_NEGATIVE_BALANCE", HttpStatus.CONFLICT);
        }
        var movement = stockMovementRepository.save(new StockMovement(itemId, normalizeId(supplierId),
                "SUPPLIER_RETURN", returnQuantity.negate(), null, returnNumber, note, occurredAt, actor));
        movement.assignDocument("SUPPLIER_RETURN", "Returned stock to supplier");
        inventoryValuationService.valueMovement(movement, unitCost, actor);
        auditService.record("STOCK_MOVEMENT", "STOCK_ITEM", itemId, actor,
                "Supplier return " + returnNumber + " return qty: " + returnQuantity, null);
    }

    @Transactional
    public BigDecimal recordProductionIssue(String itemId, BigDecimal quantity, String orderNumber, String note, Instant occurredAt, String actor) {
        requireItem(itemId);
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("Production issue quantity must be positive.", "OPS_PROD_ISSUE_QUANTITY_POSITIVE", HttpStatus.CONFLICT);
        }
        BigDecimal currentBalance = stockMovementRepository.balance(itemId);
        if (currentBalance.subtract(quantity).signum() < 0) {
            throw new BusinessRuleException("Production issue cannot exceed available inventory balance.", "OPS_PROD_ISSUE_NEGATIVE_BALANCE", HttpStatus.CONFLICT);
        }
        var movement = stockMovementRepository.save(new StockMovement(itemId, null,
                "PRODUCTION_ISSUE", quantity.negate(), null, orderNumber, note, occurredAt, actor));
        movement.assignDocument("PRODUCTION_ISSUE", "Issued raw materials for work order");
        var movementCost = inventoryValuationService.valueMovement(movement, null, actor);
        auditService.record("STOCK_MOVEMENT", "STOCK_ITEM", itemId, actor,
                "Production issue " + orderNumber + " qty: " + quantity, null);
        return movementCost.getValueEffect().abs();
    }

    @Transactional
    public void recordProductionReceipt(String itemId, BigDecimal quantity, BigDecimal unitCost, String orderNumber, String note, Instant occurredAt, String actor) {
        requireItem(itemId);
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("Production receipt quantity must be positive.", "OPS_PROD_RECEIPT_QUANTITY_POSITIVE", HttpStatus.CONFLICT);
        }
        var movement = stockMovementRepository.save(new StockMovement(itemId, null,
                "PRODUCTION_RECEIPT", quantity, null, orderNumber, note, occurredAt, actor));
        movement.assignDocument("PRODUCTION_RECEIPT", "Finished goods receipt from work order");
        inventoryValuationService.valueMovement(movement, unitCost, actor);
        auditService.record("STOCK_MOVEMENT", "STOCK_ITEM", itemId, actor,
                "Production receipt " + orderNumber + " qty: " + quantity, null);
    }

    @Transactional
    public OperationsApi.Snapshot recordAdvance(OperationsApi.AdvanceRequest request, String actor) {
        if (request.amountDelta().signum() == 0) throw new BusinessRuleException("Advance amount cannot be zero.", "OPS_ADVANCE_AMOUNT_ZERO", HttpStatus.CONFLICT);
        var employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found.", "HRCFG_EMPLOYEE_NOT_FOUND"));
        var category = attendanceCategoryRepository.findById(employee.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Employee category not found.", "HRCFG_EMPLOYEE_CATEGORY_NOT_FOUND"));
        if (!category.isAllowsEmployeeAdvances()) {
            throw new BusinessRuleException("This employee category does not allow advances.", "OPS_ADVANCE_CATEGORY_NOT_ALLOWED", HttpStatus.CONFLICT);
        }
        employeeAdvanceEntryRepository.save(new EmployeeAdvanceEntry(employee.getId(), request.amountDelta(),
                request.entryType(), request.note(), request.occurredAt(), actor));
        auditService.record("EMPLOYEE_ADVANCE", "EMPLOYEE", employee.getId(), actor,
                "Recorded advance for " + employee.getFullName() + " amount: " + request.amountDelta(), null);
        return snapshot();
    }

    @Transactional
    public void recordAdvanceIssuance(String employeeId, BigDecimal amount, String entryType, String note,
                                      Instant occurredAt, String actor) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("يجب أن يكون مبلغ السلفة أكبر من صفر.", "ADVANCE_AMOUNT_POSITIVE_REQUIRED", HttpStatus.CONFLICT);
        }
        var employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("الموظف غير موجود.", "EMPLOYEE_NOT_FOUND"));
        if (!employee.isActive()) throw new BusinessRuleException("لا يمكن صرف سلفة لموظف غير نشط.", "ADVANCE_INACTIVE_EMPLOYEE", HttpStatus.CONFLICT);
        var category = attendanceCategoryRepository.findById(employee.getCategoryId())
                .orElseThrow(() -> new NotFoundException("فئة الموظف غير موجودة.", "HRCFG_EMPLOYEE_CATEGORY_NOT_FOUND"));
        if (!category.isAllowsEmployeeAdvances()) {
            throw new BusinessRuleException("فئة هذا الموظف لا تسمح بصرف السلف.", "ADVANCE_CATEGORY_NOT_ALLOWED", HttpStatus.CONFLICT);
        }
        employeeAdvanceEntryRepository.save(new EmployeeAdvanceEntry(employee.getId(), amount,
                entryType, note, occurredAt == null ? Instant.now() : occurredAt, actor));
        auditService.record("EMPLOYEE_ADVANCE", "EMPLOYEE", employee.getId(), actor,
                "Recorded advance for " + employee.getFullName() + " amount: " + amount, null);
    }

    public BigDecimal getAdvanceBalance(String employeeId) {
        return employeeAdvanceEntryRepository.balance(employeeId);
    }

    @Transactional
    public void recordAdvanceSettlement(String employeeId, BigDecimal amount, String note, java.time.Instant occurredAt, String actor) {
        employeeAdvanceEntryRepository.save(new EmployeeAdvanceEntry(employeeId, amount, "SETTLEMENT", note, occurredAt, actor));
    }

    private InventoryItem requireItem(String id) {
        return inventoryItemRepository.findById(id).orElseThrow(() -> new NotFoundException("Inventory item not found.", "OPS_ITEM_NOT_FOUND"));
    }
    private BusinessParty requireParty(String id) {
        return businessPartyRepository.findById(id).orElseThrow(() -> new NotFoundException("Business party not found.", "PTY_NOT_FOUND"));
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
                item.isActive(), item.getReorderPoint(), item.getReorderQuantity(), balance,
                item.getVersion(), item.getCreatedAt(), item.getUpdatedAt());
    }

    @Transactional
    public ValuedMovement recordSalesDelivery(String itemId, String customerId, String warehouseId,
                                               String reservationId, BigDecimal quantity, String deliveryNumber,
                                               Instant occurredAt, String actor) {
        requireItem(itemId);
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("Sales delivery quantity must be positive.",
                    "O2C_DELIVERY_QUANTITY_POSITIVE", HttpStatus.CONFLICT);
        }
        var reservation = warehouseInventoryService.consumeReservation(reservationId);
        if (!reservation.getItemId().equals(itemId) || !reservation.getWarehouseId().equals(warehouseId)
                || reservation.getReservedQuantity().compareTo(quantity) != 0) {
            throw new BusinessRuleException("Delivery does not match its stock reservation.",
                    "O2C_RESERVATION_MISMATCH", HttpStatus.CONFLICT);
        }
        var movement = stockMovementRepository.save(new StockMovement(itemId, normalizeId(customerId),
                "EXPORT_SALE", quantity.negate(), null, deliveryNumber,
                "Sales-order delivery", occurredAt, actor));
        movement.assignDocument("DELIVERY_NOTE", "Reserved stock delivered to customer");
        movement.assignReferences(null, null, deliveryNumber, null, null, null, warehouseId,
                null, null, null);
        var cost = inventoryValuationService.valueMovement(movement, null, actor);
        auditService.record("STOCK_MOVEMENT", "STOCK_ITEM", itemId, actor,
                "Sales delivery " + deliveryNumber + " qty: " + quantity, null);
        return new ValuedMovement(movement.getId(), cost.getUnitCost(), cost.getValueEffect().abs(), cost.getJournalEntryId());
    }

    @Transactional
    public ValuedMovement recordCustomerReturn(String itemId, String customerId, String warehouseId,
                                               BigDecimal quantity, BigDecimal originalUnitCost,
                                               String returnNumber, String disposition, Instant occurredAt, String actor) {
        requireItem(itemId);
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("Customer return quantity must be positive.",
                    "O2C_RETURN_QUANTITY_POSITIVE", HttpStatus.CONFLICT);
        }
        if (!"AVAILABLE".equals(disposition)) {
            throw new BusinessRuleException("Only inspected AVAILABLE returns can be received into sellable stock.",
                    "O2C_RETURN_DISPOSITION_INVALID", HttpStatus.CONFLICT);
        }
        var movement = stockMovementRepository.save(new StockMovement(itemId, normalizeId(customerId),
                "CUSTOMER_RETURN", quantity, null, returnNumber, "Customer return", occurredAt, actor));
        movement.assignDocument("CUSTOMER_RETURN", "Returned customer stock received after inspection");
        movement.assignReferences(null, returnNumber, null, null, null, null, warehouseId, null, null, null);
        var cost = inventoryValuationService.valueMovement(movement, originalUnitCost, actor);
        warehouseInventoryService.receiveAvailableStock(warehouseId, itemId, quantity);
        auditService.record("STOCK_MOVEMENT", "STOCK_ITEM", itemId, actor,
                "Customer return " + returnNumber + " qty: " + quantity, null);
        return new ValuedMovement(movement.getId(), cost.getUnitCost(), cost.getValueEffect().abs(), cost.getJournalEntryId());
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

    private static String defaultDocumentType(String op) {
        return switch (op) {
            case "SUPPLY_RECEIPT", "PROCESSING_INTAKE" -> "GOODS_RECEIPT";
            case "PROCESSING_DELIVERY", "EXPORT_SALE", "SORTING_SALE" -> "DELIVERY_NOTE";
            case "ADJUSTMENT" -> "ADJUSTMENT";
            case "PAYMENT" -> "SUPPLIER_PAYMENT";
            default -> null;
        };
    }

    private void validateDocumentReferences(OperationsApi.TransactionRequest request, String op) {
        String partyId = normalizeId(request.partyId());
        switch (op) {
            case "SUPPLY_RECEIPT", "PROCESSING_INTAKE" -> {
                if (isBlank(request.receiptNo())) {
                    throw new BusinessRuleException("A receipt document number is required for a goods receipt movement.",
                            "OPS_MOVEMENT_RECEIPT_REQUIRED", HttpStatus.CONFLICT);
                }
                if (op.equals("SUPPLY_RECEIPT") && isBlank(request.purchaseOrderNo())) {
                    throw new BusinessRuleException("A purchase-order number is required for a supplier receipt movement.",
                            "OPS_MOVEMENT_PURCHASE_ORDER_REQUIRED", HttpStatus.CONFLICT);
                }
            }
            case "PROCESSING_DELIVERY", "EXPORT_SALE", "SORTING_SALE" -> {
                if (isBlank(request.deliveryNoteNo())) {
                    throw new BusinessRuleException("A delivery-note number is required for a sale/delivery movement.",
                            "OPS_MOVEMENT_DELIVERY_NOTE_REQUIRED", HttpStatus.CONFLICT);
                }
            }
            case "ADJUSTMENT" -> {
                if (isBlank(request.voucherNo())) {
                    throw new BusinessRuleException("An adjustment voucher number is required for an adjustment movement.",
                            "OPS_MOVEMENT_VOUCHER_REQUIRED", HttpStatus.CONFLICT);
                }
            }
            default -> { /* PAYMENT and other financial-only movements carry optional references only. */ }
        }
        if (!isBlank(request.invoiceNo())) {
            if (partyId == null) {
                throw new BusinessRuleException("A business party is required when an invoice number is provided.",
                        "OPS_MOVEMENT_INVOICE_PARTY_REQUIRED", HttpStatus.CONFLICT);
            }
            if (stockMovementRepository.existsByPartyIdAndInvoiceNoIgnoreCase(partyId, request.invoiceNo().strip())) {
                throw new BusinessRuleException("Invoice number " + request.invoiceNo().strip() + " is already recorded for this supplier.",
                        "OPS_MOVEMENT_INVOICE_DUPLICATE", HttpStatus.CONFLICT);
            }
        }
    }

    public BigDecimal stockBalance(String itemId) {
        return stockMovementRepository.balance(itemId);
    }

    public BigDecimal latestUnitCost(String itemId) {
        return inventoryValuationService.getItemUnitCost(itemId);
    }

    public BigDecimal productionIssueCost(String orderNumber, String itemId) {
        return stockMovementRepository.findByOperationTypeAndReferenceCodeAndItemId(
                        "PRODUCTION_ISSUE", orderNumber, itemId).stream()
                .map(StockMovement::getId)
                .map(inventoryMovementCostRepository::findByMovementId)
                .flatMap(Optional::stream)
                .map(InventoryMovementCost::getValueEffect)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
