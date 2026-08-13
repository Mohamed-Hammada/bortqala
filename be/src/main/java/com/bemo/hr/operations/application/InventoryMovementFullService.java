package com.bemo.hr.operations.application;

import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.domain.*;
import com.bemo.hr.operations.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class InventoryMovementFullService {

    private final StockTransferHeaderRepository transferHeaderRepository;
    private final StockTransferLineRepository transferLineRepository;
    private final CycleCountHeaderRepository cycleCountHeaderRepository;
    private final CycleCountLineRepository cycleCountLineRepository;
    private final OperationsService operationsService;
    private final WarehouseInventoryService warehouseInventoryService;
    private final WarehouseRepository warehouseRepository;
    private final AuditService auditService;

    public InventoryMovementFullService(StockTransferHeaderRepository transferHeaderRepository,
                                         StockTransferLineRepository transferLineRepository,
                                         CycleCountHeaderRepository cycleCountHeaderRepository,
                                         CycleCountLineRepository cycleCountLineRepository,
                                         OperationsService operationsService,
                                         WarehouseInventoryService warehouseInventoryService,
                                         WarehouseRepository warehouseRepository,
                                         AuditService auditService) {
        this.transferHeaderRepository = transferHeaderRepository;
        this.transferLineRepository = transferLineRepository;
        this.cycleCountHeaderRepository = cycleCountHeaderRepository;
        this.cycleCountLineRepository = cycleCountLineRepository;
        this.operationsService = operationsService;
        this.warehouseInventoryService = warehouseInventoryService;
        this.warehouseRepository = warehouseRepository;
        this.auditService = auditService;
    }

    @Transactional
    public StockTransferHeader createTransfer(String transferNumber, String sourceWarehouseId, String targetWarehouseId, LocalDate transferDate) {
        String normalizedNumber = requireText(transferNumber, "TRANSFER_NUMBER_REQUIRED");
        if (sourceWarehouseId == null || sourceWarehouseId.equals(targetWarehouseId)) {
            throw new BusinessRuleException("Source and target warehouses must be different.",
                    "TRANSFER_WAREHOUSES_DIFFERENT", HttpStatus.CONFLICT);
        }
        requireWarehouse(sourceWarehouseId);
        requireWarehouse(targetWarehouseId);
        if (transferDate == null) {
            throw new BusinessRuleException("Transfer date is required.", "TRANSFER_DATE_REQUIRED", HttpStatus.CONFLICT);
        }
        StockTransferHeader existing = transferHeaderRepository.findByTransferNumberIgnoreCase(normalizedNumber).orElse(null);
        if (existing != null) {
            if (existing.getSourceWarehouseId().equals(sourceWarehouseId)
                    && existing.getTargetWarehouseId().equals(targetWarehouseId)
                    && existing.getTransferDate().equals(transferDate)) return existing;
            throw new BusinessRuleException("Transfer number already exists.",
                    "TRANSFER_NUMBER_EXISTS", HttpStatus.CONFLICT);
        }
        StockTransferHeader transfer = new StockTransferHeader(normalizedNumber, sourceWarehouseId, targetWarehouseId, transferDate);
        return transferHeaderRepository.save(transfer);
    }

    @Transactional
    public StockTransferLine addTransferLine(String transferId, String itemId, BigDecimal quantity) {
        StockTransferHeader transfer = getTransfer(transferId);
        if (transfer.getStatus() != StockTransferHeader.Status.DRAFT) {
            throw new BusinessRuleException("Lines can only be added to a draft transfer.",
                    "TRANSFER_NOT_DRAFT", HttpStatus.CONFLICT);
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("Transfer quantity must be positive.",
                    "TRANSFER_QUANTITY_POSITIVE", HttpStatus.CONFLICT);
        }
        operationsService.inventoryItem(itemId);
        boolean duplicate = transferLineRepository.findByTransferId(transferId).stream()
                .anyMatch(line -> line.getItemId().equals(itemId));
        if (duplicate) {
            throw new BusinessRuleException("The item already exists on this transfer.",
                    "TRANSFER_ITEM_DUPLICATE", HttpStatus.CONFLICT);
        }
        StockTransferLine line = new StockTransferLine(transferId, itemId, quantity);
        return transferLineRepository.save(line);
    }

    @Transactional
    public StockTransferHeader shipTransfer(String transferId, String actor) {
        StockTransferHeader transfer = getTransfer(transferId);
        if (transfer.getStatus() == StockTransferHeader.Status.SHIPPED) return transfer;
        if (transfer.getStatus() != StockTransferHeader.Status.DRAFT) {
            throw new BusinessRuleException("Only a draft transfer can be shipped.",
                    "TRANSFER_NOT_DRAFT", HttpStatus.CONFLICT);
        }
        List<StockTransferLine> lines = transferLineRepository.findByTransferId(transferId);
        if (lines.isEmpty()) {
            throw new BusinessRuleException("Add at least one transfer line before shipping.",
                    "TRANSFER_LINES_REQUIRED", HttpStatus.CONFLICT);
        }
        for (StockTransferLine line : lines) {
            if (warehouseInventoryService.getAvailableStock(transfer.getSourceWarehouseId(), line.getItemId())
                    .compareTo(line.getQuantity()) < 0) {
                throw new BusinessRuleException("Insufficient source-warehouse stock for transfer.",
                        "TRANSFER_SOURCE_STOCK_INSUFFICIENT", HttpStatus.CONFLICT);
            }
        }
        lines.forEach(line -> warehouseInventoryService.issueAvailableStock(
                transfer.getSourceWarehouseId(), line.getItemId(), line.getQuantity()));
        transfer.ship();
        StockTransferHeader saved = transferHeaderRepository.save(transfer);
        auditService.record("STOCK_TRANSFER_SHIPPED", "STOCK_TRANSFER", saved.getId(), actor,
                "Shipped transfer " + saved.getTransferNumber() + " lines=" + lines.size(), null);
        return saved;
    }

    @Transactional
    public StockTransferHeader receiveTransfer(String transferId, String actor) {
        StockTransferHeader transfer = getTransfer(transferId);
        if (transfer.getStatus() == StockTransferHeader.Status.RECEIVED) return transfer;
        if (transfer.getStatus() != StockTransferHeader.Status.SHIPPED) {
            throw new BusinessRuleException("Only a shipped transfer can be received.",
                    "TRANSFER_NOT_SHIPPED", HttpStatus.CONFLICT);
        }
        List<StockTransferLine> lines = transferLineRepository.findByTransferId(transferId);
        lines.forEach(line -> warehouseInventoryService.receiveAvailableStock(
                transfer.getTargetWarehouseId(), line.getItemId(), line.getQuantity()));
        transfer.receive();
        StockTransferHeader saved = transferHeaderRepository.save(transfer);
        auditService.record("STOCK_TRANSFER_RECEIVED", "STOCK_TRANSFER", saved.getId(), actor,
                "Received transfer " + saved.getTransferNumber() + " lines=" + lines.size(), null);
        return saved;
    }

    @Transactional
    public StockTransferHeader cancelTransfer(String transferId, String actor) {
        StockTransferHeader transfer = getTransfer(transferId);
        if (transfer.getStatus() == StockTransferHeader.Status.CANCELLED) return transfer;
        if (transfer.getStatus() != StockTransferHeader.Status.DRAFT) {
            throw new BusinessRuleException("Only a draft transfer can be cancelled.",
                    "TRANSFER_NOT_DRAFT", HttpStatus.CONFLICT);
        }
        transfer.cancel();
        StockTransferHeader saved = transferHeaderRepository.save(transfer);
        auditService.record("STOCK_TRANSFER_CANCELLED", "STOCK_TRANSFER", saved.getId(), actor,
                "Cancelled transfer " + saved.getTransferNumber(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TransferView> transfers() {
        return transferHeaderRepository.findAllByOrderByTransferDateDescCreatedAtDesc().stream()
                .map(this::transferView).toList();
    }

    @Transactional(readOnly = true)
    public TransferView transfer(String id) { return transferView(getTransfer(id)); }

    @Transactional
    public CycleCountHeader createCycleCount(String countNumber, String warehouseId, LocalDate countDate) {
        requireWarehouse(warehouseId);
        CycleCountHeader count = new CycleCountHeader(countNumber, warehouseId, countDate);
        count.start();
        return cycleCountHeaderRepository.save(count);
    }

    @Transactional
    public CycleCountLine addCycleCountLine(String countId, String itemId, BigDecimal systemQuantity, BigDecimal countedQuantity) {
        CycleCountHeader count = cycleCountHeaderRepository.findById(countId)
                .orElseThrow(() -> new BusinessRuleException("Cycle count not found", "CYCLE_COUNT_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (count.getStatus() != CycleCountHeader.Status.IN_PROGRESS) {
            throw new BusinessRuleException("Lines can only be added to an active cycle count.", "CYCLE_COUNT_NOT_IN_PROGRESS", HttpStatus.CONFLICT);
        }
        operationsService.inventoryItem(itemId);
        CycleCountLine line = new CycleCountLine(countId, itemId,
                warehouseInventoryService.getPhysicalStock(count.getWarehouseId(), itemId), countedQuantity);
        return cycleCountLineRepository.save(line);
    }

    @Transactional
    public CycleCountHeader adjustCycleCount(String countId, String actor) {
        CycleCountHeader count = cycleCountHeaderRepository.findById(countId)
                .orElseThrow(() -> new BusinessRuleException("Cycle count not found", "CYCLE_COUNT_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (count.getStatus() == CycleCountHeader.Status.ADJUSTED) return count;
        count.submit();
        for (CycleCountLine line : cycleCountLineRepository.findByCountId(countId)) {
            if (line.getVarianceQuantity().signum() != 0) {
                operationsService.createStockAdjustment(new com.bemo.hr.operations.OperationsApi.AdjustmentRequest(
                        line.getItemId(), line.getVarianceQuantity(), count.getCountNumber(),
                        "Cycle count " + count.getCountNumber(), true,
                        count.getCountDate().atStartOfDay().toInstant(ZoneOffset.UTC)), actor);
                warehouseInventoryService.adjustAvailableStock(count.getWarehouseId(), line.getItemId(), line.getVarianceQuantity());
            }
        }
        count.adjust();
        CycleCountHeader saved = cycleCountHeaderRepository.save(count);
        auditService.record("CYCLE_COUNT_ADJUSTED", "CYCLE_COUNT", saved.getId(), actor,
                "Adjusted cycle count " + saved.getCountNumber(), null);
        return saved;
    }

    @Transactional
    public CycleCountHeader reconcile(String operationId, String warehouseId, String itemId,
                                      BigDecimal countedQuantity, LocalDate countDate, String actor) {
        var replay = cycleCountHeaderRepository.findByCountNumber(operationId);
        if (replay.isPresent()) return replay.get();
        CycleCountHeader count = createCycleCount(operationId, warehouseId, countDate);
        addCycleCountLine(count.getId(), itemId, null, countedQuantity);
        return adjustCycleCount(count.getId(), actor);
    }

    @Transactional(readOnly = true)
    public List<CycleCountSummary> cycleCounts() {
        return cycleCountHeaderRepository.findAllByOrderByCountDateDescCreatedAtDesc().stream()
                .flatMap(header -> cycleCountLineRepository.findByCountId(header.getId()).stream()
                        .map(line -> new CycleCountSummary(header.getId(), header.getCountNumber(), header.getWarehouseId(),
                                header.getCountDate(), header.getStatus().name(), line.getItemId(),
                                line.getSystemQuantity(), line.getCountedQuantity(), line.getVarianceQuantity())))
                .toList();
    }

    public record CycleCountSummary(String id, String countNumber, String warehouseId, LocalDate countDate,
                                    String status, String itemId, BigDecimal systemQuantity,
                                    BigDecimal countedQuantity, BigDecimal variance) { }

    public record TransferLineView(String id, String itemId, String itemCode, String itemName, BigDecimal quantity) { }
    public record TransferView(String id, String transferNumber, String sourceWarehouseId, String sourceWarehouseName,
                               String targetWarehouseId, String targetWarehouseName, LocalDate transferDate,
                               String status, long version, List<TransferLineView> lines) { }

    private TransferView transferView(StockTransferHeader header) {
        Warehouse source = warehouseRepository.findById(header.getSourceWarehouseId()).orElse(null);
        Warehouse target = warehouseRepository.findById(header.getTargetWarehouseId()).orElse(null);
        List<TransferLineView> lines = transferLineRepository.findByTransferId(header.getId()).stream().map(line -> {
            var item = operationsService.inventoryItem(line.getItemId());
            return new TransferLineView(line.getId(), line.getItemId(), item.code(), item.name(), line.getQuantity());
        }).toList();
        return new TransferView(header.getId(), header.getTransferNumber(), header.getSourceWarehouseId(),
                source == null ? null : source.getName(), header.getTargetWarehouseId(), target == null ? null : target.getName(),
                header.getTransferDate(), header.getStatus().name(), header.getVersion(), lines);
    }

    private Warehouse requireWarehouse(String id) {
        return warehouseRepository.findById(id).filter(Warehouse::isActive)
                .orElseThrow(() -> new BusinessRuleException("Select an active warehouse.",
                        "WAREHOUSE_ACTIVE_REQUIRED", HttpStatus.CONFLICT));
    }

    private String requireText(String value, String code) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("A transfer number is required.", code, HttpStatus.CONFLICT);
        }
        return value.strip();
    }

    private StockTransferHeader getTransfer(String id) {
        return transferHeaderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Stock transfer not found", "TRANSFER_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
