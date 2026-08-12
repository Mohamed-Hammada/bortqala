package com.bemo.hr.operations.application;

import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.domain.*;
import com.bemo.hr.operations.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
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

    public InventoryMovementFullService(StockTransferHeaderRepository transferHeaderRepository,
                                         StockTransferLineRepository transferLineRepository,
                                         CycleCountHeaderRepository cycleCountHeaderRepository,
                                         CycleCountLineRepository cycleCountLineRepository,
                                         OperationsService operationsService) {
        this.transferHeaderRepository = transferHeaderRepository;
        this.transferLineRepository = transferLineRepository;
        this.cycleCountHeaderRepository = cycleCountHeaderRepository;
        this.cycleCountLineRepository = cycleCountLineRepository;
        this.operationsService = operationsService;
    }

    @Transactional
    public StockTransferHeader createTransfer(String transferNumber, String sourceWarehouseId, String targetWarehouseId, LocalDate transferDate) {
        StockTransferHeader transfer = new StockTransferHeader(transferNumber, sourceWarehouseId, targetWarehouseId, transferDate);
        return transferHeaderRepository.save(transfer);
    }

    @Transactional
    public StockTransferLine addTransferLine(String transferId, String itemId, BigDecimal quantity) {
        StockTransferLine line = new StockTransferLine(transferId, itemId, quantity);
        return transferLineRepository.save(line);
    }

    @Transactional
    public StockTransferHeader shipTransfer(String transferId) {
        StockTransferHeader transfer = getTransfer(transferId);
        transfer.ship();
        return transferHeaderRepository.save(transfer);
    }

    @Transactional
    public StockTransferHeader receiveTransfer(String transferId) {
        StockTransferHeader transfer = getTransfer(transferId);
        transfer.receive();
        return transferHeaderRepository.save(transfer);
    }

    @Transactional
    public CycleCountHeader createCycleCount(String countNumber, String warehouseId, LocalDate countDate) {
        CycleCountHeader count = new CycleCountHeader(countNumber, warehouseId, countDate);
        count.start();
        return cycleCountHeaderRepository.save(count);
    }

    @Transactional
    public CycleCountLine addCycleCountLine(String countId, String itemId, BigDecimal systemQuantity, BigDecimal countedQuantity) {
        CycleCountLine line = new CycleCountLine(countId, itemId, operationsService.stockBalance(itemId), countedQuantity);
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
            }
        }
        count.adjust();
        return cycleCountHeaderRepository.save(count);
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

    private StockTransferHeader getTransfer(String id) {
        return transferHeaderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Stock transfer not found", "TRANSFER_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
