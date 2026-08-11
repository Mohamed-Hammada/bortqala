package com.bemo.hr.operations.api;

import com.bemo.hr.operations.application.InventoryMovementFullService;
import com.bemo.hr.operations.domain.CycleCountHeader;
import com.bemo.hr.operations.domain.CycleCountLine;
import com.bemo.hr.operations.domain.StockTransferHeader;
import com.bemo.hr.operations.domain.StockTransferLine;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/operations")
public class InventoryMovementController {

    private final InventoryMovementFullService movementService;

    public InventoryMovementController(InventoryMovementFullService movementService) {
        this.movementService = movementService;
    }

    public record CreateTransferPayload(String transferNumber, String sourceWarehouseId, String targetWarehouseId, String transferDate) {}
    public record AddTransferLinePayload(String itemId, BigDecimal quantity) {}
    public record CreateCycleCountPayload(String countNumber, String warehouseId, String countDate) {}
    public record AddCycleCountLinePayload(String itemId, BigDecimal systemQuantity, BigDecimal countedQuantity) {}

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public StockTransferHeader createTransfer(@RequestBody CreateTransferPayload payload) {
        return movementService.createTransfer(payload.transferNumber(), payload.sourceWarehouseId(), payload.targetWarehouseId(), LocalDate.parse(payload.transferDate()));
    }

    @PostMapping("/transfers/{id}/lines")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public StockTransferLine addTransferLine(@PathVariable String id, @RequestBody AddTransferLinePayload payload) {
        return movementService.addTransferLine(id, payload.itemId(), payload.quantity());
    }

    @PostMapping("/transfers/{id}/ship")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public StockTransferHeader shipTransfer(@PathVariable String id) {
        return movementService.shipTransfer(id);
    }

    @PostMapping("/transfers/{id}/receive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public StockTransferHeader receiveTransfer(@PathVariable String id) {
        return movementService.receiveTransfer(id);
    }

    @PostMapping("/cycle-counts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public CycleCountHeader createCycleCount(@RequestBody CreateCycleCountPayload payload) {
        return movementService.createCycleCount(payload.countNumber(), payload.warehouseId(), LocalDate.parse(payload.countDate()));
    }

    @PostMapping("/cycle-counts/{id}/lines")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public CycleCountLine addCycleCountLine(@PathVariable String id, @RequestBody AddCycleCountLinePayload payload) {
        return movementService.addCycleCountLine(id, payload.itemId(), payload.systemQuantity(), payload.countedQuantity());
    }

    @PostMapping("/cycle-counts/{id}/adjust")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public CycleCountHeader adjustCycleCount(@PathVariable String id) {
        return movementService.adjustCycleCount(id);
    }
}
