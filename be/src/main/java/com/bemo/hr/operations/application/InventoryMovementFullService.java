package com.bemo.hr.operations.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.domain.CycleCountHeader;
import com.bemo.hr.operations.domain.CycleCountLine;
import com.bemo.hr.operations.domain.StockTransferDiscrepancy;
import com.bemo.hr.operations.domain.StockTransferHeader;
import com.bemo.hr.operations.domain.StockTransferLine;
import com.bemo.hr.operations.infrastructure.CycleCountHeaderRepository;
import com.bemo.hr.operations.infrastructure.CycleCountLineRepository;
import com.bemo.hr.operations.infrastructure.StockTransferDiscrepancyRepository;
import com.bemo.hr.operations.infrastructure.StockTransferHeaderRepository;
import com.bemo.hr.operations.infrastructure.StockTransferLineRepository;
import com.bemo.hr.organization.domain.Branch;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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
    private final StockTransferDiscrepancyRepository discrepancyRepository;
    private final BranchRepository branchRepository;

    public InventoryMovementFullService(StockTransferHeaderRepository transferHeaderRepository,
                                        StockTransferLineRepository transferLineRepository,
                                        CycleCountHeaderRepository cycleCountHeaderRepository,
                                        CycleCountLineRepository cycleCountLineRepository,
                                        OperationsService operationsService,
                                        WarehouseInventoryService warehouseInventoryService,
                                        WarehouseRepository warehouseRepository,
                                        AuditService auditService) {
        this(transferHeaderRepository, transferLineRepository, cycleCountHeaderRepository, cycleCountLineRepository,
                operationsService, warehouseInventoryService, warehouseRepository, auditService, null, null);
    }

    @Autowired
    public InventoryMovementFullService(StockTransferHeaderRepository transferHeaderRepository,
                                        StockTransferLineRepository transferLineRepository,
                                        CycleCountHeaderRepository cycleCountHeaderRepository,
                                        CycleCountLineRepository cycleCountLineRepository,
                                        OperationsService operationsService,
                                        WarehouseInventoryService warehouseInventoryService,
                                        WarehouseRepository warehouseRepository,
                                        AuditService auditService,
                                        StockTransferDiscrepancyRepository discrepancyRepository,
                                        BranchRepository branchRepository) {
        this.transferHeaderRepository = transferHeaderRepository;
        this.transferLineRepository = transferLineRepository;
        this.cycleCountHeaderRepository = cycleCountHeaderRepository;
        this.cycleCountLineRepository = cycleCountLineRepository;
        this.operationsService = operationsService;
        this.warehouseInventoryService = warehouseInventoryService;
        this.warehouseRepository = warehouseRepository;
        this.auditService = auditService;
        this.discrepancyRepository = discrepancyRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public StockTransferHeader createTransfer(String transferNumber, String sourceWarehouseId, String targetWarehouseId, LocalDate transferDate) {
        log.debug("createTransfer called with transferNumber={}, sourceWarehouseId={}, targetWarehouseId={}", transferNumber, sourceWarehouseId, targetWarehouseId);
        String normalizedNumber = requireText(transferNumber, "TRANSFER_NUMBER_REQUIRED");
        if (sourceWarehouseId == null || sourceWarehouseId.equals(targetWarehouseId)) {
            throw new BusinessRuleException("Source and target warehouses must be different.",
                    "TRANSFER_WAREHOUSES_DIFFERENT", HttpStatus.CONFLICT);
        }
        Warehouse source = requireWarehouse(sourceWarehouseId);
        Warehouse target = requireWarehouse(targetWarehouseId);
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
        StockTransferHeader transfer = new StockTransferHeader(
                normalizedNumber,
                sourceWarehouseId,
                targetWarehouseId,
                source.getBranchId(),
                target.getBranchId(),
                transferDate
        );
        StockTransferHeader saved = transferHeaderRepository.save(transfer);
        log.info("StockTransferHeader {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public StockTransferLine addTransferLine(String transferId, String itemId, BigDecimal quantity) {
        log.debug("addTransferLine called with transferId={}, itemId={}, quantity={}", transferId, itemId, quantity);
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
        StockTransferLine saved = transferLineRepository.save(line);
        log.info("StockTransferLine {} added to transfer {}", saved.getId(), transferId);
        return saved;
    }

    @Transactional
    public StockTransferHeader shipTransfer(String transferId, String actor) {
        return dispatchTransfer(transferId, null, null, null, null, null, null, actor);
    }

    @Transactional
    public StockTransferHeader dispatchTransfer(String transferId, String carrierName, String driverName,
                                                String driverPhone, String vehiclePlate, String waybillNumber,
                                                String notes, String actor) {
        log.debug("dispatchTransfer called with transferId={}, actor={}", transferId, actor);
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
        lines.forEach(line -> {
            warehouseInventoryService.issueAvailableStock(
                    transfer.getSourceWarehouseId(), line.getItemId(), line.getQuantity());
            line.setShippedQuantity(line.getQuantity());
            transferLineRepository.save(line);
        });

        transfer.dispatch(carrierName, driverName, driverPhone, vehiclePlate, waybillNumber, notes, actor);
        StockTransferHeader saved = transferHeaderRepository.save(transfer);
        auditService.record("STOCK_TRANSFER_SHIPPED", "STOCK_TRANSFER", saved.getId(), actor,
                "Shipped transfer " + saved.getTransferNumber() + " lines=" + lines.size()
                        + (waybillNumber != null ? " waybill=" + waybillNumber : ""), null);
        log.info("StockTransferHeader {} shipped successfully", saved.getId());
        return saved;
    }

    @Transactional
    public StockTransferHeader receiveTransfer(String transferId, String actor) {
        return receiveTransferWithInspection(transferId, List.of(), null, actor);
    }

    @Transactional
    public StockTransferHeader receiveTransferWithInspection(String transferId,
                                                             List<ReceiptInspectionLineInput> inspectionLines,
                                                             String inspectionNotes,
                                                             String actor) {
        log.debug("receiveTransferWithInspection called with transferId={}, actor={}", transferId, actor);
        StockTransferHeader transfer = getTransfer(transferId);
        if (transfer.getStatus() == StockTransferHeader.Status.RECEIVED) return transfer;
        if (transfer.getStatus() != StockTransferHeader.Status.SHIPPED) {
            throw new BusinessRuleException("Only a shipped transfer can be received.",
                    "TRANSFER_NOT_SHIPPED", HttpStatus.CONFLICT);
        }
        List<StockTransferLine> lines = transferLineRepository.findByTransferId(transferId);
        Map<String, ReceiptInspectionLineInput> inspectionMap = (inspectionLines != null)
                ? inspectionLines.stream().collect(Collectors.toMap(ReceiptInspectionLineInput::lineId, i -> i, (a, b) -> a))
                : Map.of();

        boolean anyDiscrepancy = false;

        for (StockTransferLine line : lines) {
            BigDecimal shippedQty = line.getShippedQuantity() != null ? line.getShippedQuantity() : line.getQuantity();
            ReceiptInspectionLineInput insp = inspectionMap.get(line.getId());

            BigDecimal receivedGoodQty = shippedQty;
            BigDecimal damagedQty = BigDecimal.ZERO;
            BigDecimal lostQty = BigDecimal.ZERO;
            String reason = null;
            String notes = null;

            if (insp != null) {
                receivedGoodQty = insp.receivedQuantity() != null ? insp.receivedQuantity() : BigDecimal.ZERO;
                damagedQty = insp.damagedQuantity() != null ? insp.damagedQuantity() : BigDecimal.ZERO;
                lostQty = insp.lostQuantity() != null ? insp.lostQuantity() : BigDecimal.ZERO;
                reason = insp.discrepancyReason();
                notes = insp.discrepancyNotes();
            }

            line.updateReceipt(receivedGoodQty, damagedQty, lostQty, reason, notes);
            transferLineRepository.save(line);

            // Stock target warehouse with good accepted quantity
            if (receivedGoodQty.compareTo(BigDecimal.ZERO) > 0) {
                warehouseInventoryService.receiveAvailableStock(
                        transfer.getTargetWarehouseId(), line.getItemId(), receivedGoodQty);
            }

            // Flag discrepancy if damage or loss recorded
            if (damagedQty.compareTo(BigDecimal.ZERO) > 0 || lostQty.compareTo(BigDecimal.ZERO) > 0) {
                anyDiscrepancy = true;
                if (discrepancyRepository != null) {
                    StockTransferDiscrepancy.DiscrepancyType discType = damagedQty.compareTo(BigDecimal.ZERO) > 0
                            ? StockTransferDiscrepancy.DiscrepancyType.DAMAGED
                            : StockTransferDiscrepancy.DiscrepancyType.SHORTAGE;

                    StockTransferDiscrepancy discrepancy = new StockTransferDiscrepancy(
                            transferId,
                            line.getId(),
                            line.getItemId(),
                            shippedQty,
                            receivedGoodQty,
                            damagedQty,
                            lostQty,
                            discType,
                            actor != null ? actor : "SYSTEM"
                    );
                    discrepancyRepository.save(discrepancy);
                }
            }
        }

        transfer.receiveWithInspection(actor, anyDiscrepancy);
        StockTransferHeader saved = transferHeaderRepository.save(transfer);
        auditService.record("STOCK_TRANSFER_RECEIVED", "STOCK_TRANSFER", saved.getId(), actor,
                "Received transfer " + saved.getTransferNumber() + " lines=" + lines.size()
                        + " discrepancy=" + anyDiscrepancy, null);
        log.info("StockTransferHeader {} received successfully with discrepancy={}", saved.getId(), anyDiscrepancy);
        return saved;
    }

    @Transactional
    public StockTransferDiscrepancy resolveDiscrepancy(String discrepancyId, String resolutionStatus,
                                                      String resolutionNotes, String actor) {
        if (discrepancyRepository == null) {
            throw new BusinessRuleException("Discrepancy repository unavailable", "TRANSFER_DISCREPANCY_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        StockTransferDiscrepancy discrepancy = discrepancyRepository.findById(discrepancyId)
                .orElseThrow(() -> new BusinessRuleException("Discrepancy not found", "TRANSFER_DISCREPANCY_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (discrepancy.getResolutionStatus() != StockTransferDiscrepancy.ResolutionStatus.PENDING) {
            throw new BusinessRuleException("Discrepancy already resolved", "TRANSFER_DISCREPANCY_ALREADY_RESOLVED", HttpStatus.BAD_REQUEST);
        }

        StockTransferDiscrepancy.ResolutionStatus status = StockTransferDiscrepancy.ResolutionStatus.valueOf(resolutionStatus);
        discrepancy.resolve(actor, status, resolutionNotes, null);
        StockTransferDiscrepancy saved = discrepancyRepository.save(discrepancy);

        auditService.record("STOCK_DISCREPANCY_RESOLVED", "STOCK_DISCREPANCY", saved.getId(), actor,
                "Resolved discrepancy for transfer " + saved.getTransferId() + " status=" + status, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<StockTransferDiscrepancy> discrepancies(String transferId) {
        if (discrepancyRepository == null) return List.of();
        if (transferId != null && !transferId.isBlank()) {
            return discrepancyRepository.findByTransferId(transferId);
        }
        return discrepancyRepository.findAllByOrderByReportedAtDesc();
    }

    @Transactional
    public StockTransferHeader cancelTransfer(String transferId, String actor) {
        log.debug("cancelTransfer called with transferId={}, actor={}", transferId, actor);
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
        log.info("StockTransferHeader {} cancelled successfully", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TransferView> transfers() {
        return transferHeaderRepository.findAllByOrderByTransferDateDescCreatedAtDesc().stream()
                .map(this::transferView).toList();
    }

    @Transactional(readOnly = true)
    public TransferView transfer(String id) {
        return transferView(getTransfer(id));
    }

    @Transactional
    public CycleCountHeader createCycleCount(String countNumber, String warehouseId, LocalDate countDate) {
        log.debug("createCycleCount called with countNumber={}, warehouseId={}", countNumber, warehouseId);
        requireWarehouse(warehouseId);
        CycleCountHeader count = new CycleCountHeader(countNumber, warehouseId, countDate);
        count.start();
        CycleCountHeader saved = cycleCountHeaderRepository.save(count);
        log.info("CycleCountHeader {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public CycleCountLine addCycleCountLine(String countId, String itemId, BigDecimal systemQuantity, BigDecimal countedQuantity) {
        log.debug("addCycleCountLine called with countId={}, itemId={}", countId, itemId);
        CycleCountHeader count = cycleCountHeaderRepository.findById(countId)
                .orElseThrow(() -> new BusinessRuleException("Cycle count not found", "CYCLE_COUNT_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (count.getStatus() != CycleCountHeader.Status.IN_PROGRESS) {
            throw new BusinessRuleException("Lines can only be added to an active cycle count.", "CYCLE_COUNT_NOT_IN_PROGRESS", HttpStatus.CONFLICT);
        }
        operationsService.inventoryItem(itemId);
        CycleCountLine line = new CycleCountLine(countId, itemId,
                warehouseInventoryService.getPhysicalStock(count.getWarehouseId(), itemId), countedQuantity);
        CycleCountLine saved = cycleCountLineRepository.save(line);
        log.info("CycleCountLine {} added to cycle count {}", saved.getId(), countId);
        return saved;
    }

    @Transactional
    public CycleCountHeader adjustCycleCount(String countId, String actor) {
        log.debug("adjustCycleCount called with countId={}, actor={}", countId, actor);
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
        log.info("CycleCountHeader {} adjusted successfully", saved.getId());
        return saved;
    }

    @Transactional
    public CycleCountHeader reconcile(String operationId, String warehouseId, String itemId,
                                      BigDecimal countedQuantity, LocalDate countDate, String actor) {
        log.debug("reconcile called with operationId={}, warehouseId={}, itemId={}, actor={}", operationId, warehouseId, itemId, actor);
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

    private TransferView transferView(StockTransferHeader header) {
        Warehouse source = warehouseRepository.findById(header.getSourceWarehouseId()).orElse(null);
        Warehouse target = warehouseRepository.findById(header.getTargetWarehouseId()).orElse(null);

        String sourceBranchName = null;
        String targetBranchName = null;
        if (branchRepository != null) {
            if (header.getSourceBranchId() != null) {
                sourceBranchName = branchRepository.findById(header.getSourceBranchId()).map(Branch::getName).orElse(null);
            }
            if (header.getTargetBranchId() != null) {
                targetBranchName = branchRepository.findById(header.getTargetBranchId()).map(Branch::getName).orElse(null);
            }
        }

        List<TransferLineView> lines = transferLineRepository.findByTransferId(header.getId()).stream().map(line -> {
            var item = operationsService.inventoryItem(line.getItemId());
            return new TransferLineView(
                    line.getId(),
                    line.getItemId(),
                    item.code(),
                    item.name(),
                    line.getQuantity(),
                    line.getShippedQuantity() != null ? line.getShippedQuantity() : line.getQuantity(),
                    line.getReceivedQuantity(),
                    line.getDamagedQuantity(),
                    line.getLostQuantity(),
                    line.getDiscrepancyReason(),
                    line.getDiscrepancyNotes()
            );
        }).toList();

        return new TransferView(
                header.getId(),
                header.getTransferNumber(),
                header.getSourceWarehouseId(),
                source == null ? null : source.getName(),
                header.getTargetWarehouseId(),
                target == null ? null : target.getName(),
                header.getSourceBranchId(),
                sourceBranchName,
                header.getTargetBranchId(),
                targetBranchName,
                header.getTransferDate(),
                header.getStatus().name(),
                header.getCarrierName(),
                header.getDriverName(),
                header.getDriverPhone(),
                header.getVehiclePlate(),
                header.getWaybillNumber(),
                header.getDispatchedAt(),
                header.getDispatchedBy(),
                header.getReceivedAt(),
                header.getReceivedBy(),
                header.isHasDiscrepancy(),
                header.getNotes(),
                header.getIntercompanyTransactionId(),
                header.getVersion(),
                lines
        );
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

    public record ReceiptInspectionLineInput(
            String lineId,
            BigDecimal receivedQuantity,
            BigDecimal damagedQuantity,
            BigDecimal lostQuantity,
            String discrepancyReason,
            String discrepancyNotes
    ) {
    }

    public record CycleCountSummary(String id, String countNumber, String warehouseId, LocalDate countDate,
                                    String status, String itemId, BigDecimal systemQuantity,
                                    BigDecimal countedQuantity, BigDecimal variance) {
    }

    public record TransferLineView(
            String id,
            String itemId,
            String itemCode,
            String itemName,
            BigDecimal quantity,
            BigDecimal shippedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal damagedQuantity,
            BigDecimal lostQuantity,
            String discrepancyReason,
            String discrepancyNotes
    ) {
    }

    public record TransferView(
            String id,
            String transferNumber,
            String sourceWarehouseId,
            String sourceWarehouseName,
            String targetWarehouseId,
            String targetWarehouseName,
            String sourceBranchId,
            String sourceBranchName,
            String targetBranchId,
            String targetBranchName,
            LocalDate transferDate,
            String status,
            String carrierName,
            String driverName,
            String driverPhone,
            String vehiclePlate,
            String waybillNumber,
            Long dispatchedAt,
            String dispatchedBy,
            Long receivedAt,
            String receivedBy,
            boolean hasDiscrepancy,
            String notes,
            String intercompanyTransactionId,
            long version,
            List<TransferLineView> lines
    ) {
    }
}
