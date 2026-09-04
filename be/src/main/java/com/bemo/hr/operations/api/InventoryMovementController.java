package com.bemo.hr.operations.api;

import com.bemo.hr.operations.application.InventoryMovementFullService;
import com.bemo.hr.operations.domain.CycleCountHeader;
import com.bemo.hr.operations.domain.CycleCountLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/operations")
public class InventoryMovementController {

    private final InventoryMovementFullService movementService;

    public InventoryMovementController(InventoryMovementFullService movementService) {
        this.movementService = movementService;
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public InventoryMovementFullService.TransferView createTransfer(@Valid @RequestBody CreateTransferPayload payload) {
        var transfer = movementService.createTransfer(payload.transferNumber(), payload.sourceWarehouseId(), payload.targetWarehouseId(), LocalDate.parse(payload.transferDate()));
        return movementService.transfer(transfer.getId());
    }

    @GetMapping("/transfers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public List<InventoryMovementFullService.TransferView> transfers() {
        return movementService.transfers();
    }

    @PostMapping("/transfers/{id}/lines")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public InventoryMovementFullService.TransferView addTransferLine(@PathVariable String id, @Valid @RequestBody AddTransferLinePayload payload) {
        movementService.addTransferLine(id, payload.itemId(), payload.quantity());
        return movementService.transfer(id);
    }

    @PostMapping("/transfers/{id}/ship")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public InventoryMovementFullService.TransferView shipTransfer(@PathVariable String id, Authentication authentication) {
        movementService.shipTransfer(id, authentication.getName());
        return movementService.transfer(id);
    }

    @PostMapping("/transfers/{id}/dispatch")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public InventoryMovementFullService.TransferView dispatchTransfer(
            @PathVariable String id,
            @RequestBody(required = false) DispatchTransferPayload payload,
            Authentication authentication
    ) {
        String carrier = payload != null ? payload.carrierName() : null;
        String driver = payload != null ? payload.driverName() : null;
        String phone = payload != null ? payload.driverPhone() : null;
        String plate = payload != null ? payload.vehiclePlate() : null;
        String waybill = payload != null ? payload.waybillNumber() : null;
        String notes = payload != null ? payload.notes() : null;
        movementService.dispatchTransfer(id, carrier, driver, phone, plate, waybill, notes, authentication.getName());
        return movementService.transfer(id);
    }

    @PostMapping("/transfers/{id}/receive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public InventoryMovementFullService.TransferView receiveTransfer(@PathVariable String id, Authentication authentication) {
        movementService.receiveTransfer(id, authentication.getName());
        return movementService.transfer(id);
    }

    @PostMapping("/transfers/{id}/receive-inspection")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public InventoryMovementFullService.TransferView receiveTransferInspection(
            @PathVariable String id,
            @RequestBody(required = false) ReceiveTransferPayload payload,
            Authentication authentication
    ) {
        List<InventoryMovementFullService.ReceiptInspectionLineInput> lines = payload != null ? payload.lines() : List.of();
        String notes = payload != null ? payload.notes() : null;
        movementService.receiveTransferWithInspection(id, lines, notes, authentication.getName());
        return movementService.transfer(id);
    }

    @GetMapping("/transfers/discrepancies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public List<com.bemo.hr.operations.domain.StockTransferDiscrepancy> discrepancies(
            @RequestParam(required = false) String transferId
    ) {
        return movementService.discrepancies(transferId);
    }

    @PostMapping("/transfers/discrepancies/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public com.bemo.hr.operations.domain.StockTransferDiscrepancy resolveDiscrepancy(
            @PathVariable String id,
            @Valid @RequestBody ResolveDiscrepancyPayload payload,
            Authentication authentication
    ) {
        return movementService.resolveDiscrepancy(id, payload.resolutionStatus(), payload.resolutionNotes(), authentication.getName());
    }

    @PostMapping("/transfers/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public InventoryMovementFullService.TransferView cancelTransfer(@PathVariable String id, Authentication authentication) {
        movementService.cancelTransfer(id, authentication.getName());
        return movementService.transfer(id);
    }

    @PostMapping("/cycle-counts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public CycleCountHeader createCycleCount(@RequestBody CreateCycleCountPayload payload) {
        return movementService.createCycleCount(payload.countNumber(), payload.warehouseId(), LocalDate.parse(payload.countDate()));
    }

    @PostMapping("/cycle-counts/{id}/lines")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public CycleCountLine addCycleCountLine(@PathVariable String id, @RequestBody AddCycleCountLinePayload payload) {
        return movementService.addCycleCountLine(id, payload.itemId(), null, payload.countedQuantity());
    }

    @PostMapping("/cycle-counts/{id}/adjust")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public CycleCountHeader adjustCycleCount(@PathVariable String id, Authentication authentication) {
        return movementService.adjustCycleCount(id, authentication.getName());
    }

    @GetMapping("/cycle-counts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public List<InventoryMovementFullService.CycleCountSummary> cycleCounts() {
        return movementService.cycleCounts();
    }

    @PostMapping("/cycle-counts/reconcile")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public CycleCountHeader reconcile(@RequestBody ReconcileCycleCountPayload payload, Authentication authentication) {
        return movementService.reconcile(payload.operationId(), payload.warehouseId(), payload.itemId(),
                payload.countedQuantity(), LocalDate.parse(payload.countDate()), authentication.getName());
    }

    public record CreateTransferPayload(@NotBlank String transferNumber, @NotBlank String sourceWarehouseId,
                                        @NotBlank String targetWarehouseId, @NotBlank String transferDate) {
    }

    public record AddTransferLinePayload(@NotBlank String itemId, @NotNull @DecimalMin("0.0001") BigDecimal quantity) {
    }

    public record CreateCycleCountPayload(String countNumber, String warehouseId, String countDate) {
    }

    public record AddCycleCountLinePayload(String itemId, BigDecimal countedQuantity) {
    }

    public record ReconcileCycleCountPayload(String operationId, String warehouseId, String itemId,
                                             BigDecimal countedQuantity, String countDate) {
    }

    public record DispatchTransferPayload(
            String carrierName,
            String driverName,
            String driverPhone,
            String vehiclePlate,
            String waybillNumber,
            String notes
    ) {
    }

    public record ReceiveTransferPayload(
            List<InventoryMovementFullService.ReceiptInspectionLineInput> lines,
            String notes
    ) {
    }

    public record ResolveDiscrepancyPayload(
            @NotBlank String resolutionStatus,
            String resolutionNotes
    ) {
    }
}
