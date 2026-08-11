package com.bemo.hr.operations.api;

import com.bemo.hr.operations.application.InventoryValuationSnapshotService;
import com.bemo.hr.operations.domain.StockValuationRecord;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/operations/inventory/valuation")
public class InventoryValuationController {

    private final InventoryValuationSnapshotService valuationService;

    public InventoryValuationController(InventoryValuationSnapshotService valuationService) {
        this.valuationService = valuationService;
    }

    public record CalculateValuationPayload(String asOfDate, BigDecimal defaultUnitCost) {}
    public record ReconcilePayload(String asOfDate, BigDecimal glBalance) {}

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'FINANCE_MANAGER')")
    public List<StockValuationRecord> calculateValuation(@RequestBody CalculateValuationPayload payload) {
        return valuationService.calculateValuation(LocalDate.parse(payload.asOfDate()), payload.defaultUnitCost());
    }

    @PostMapping("/reconciliation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public InventoryValuationSnapshotService.ValuationReconciliationResult reconcile(@RequestBody ReconcilePayload payload) {
        return valuationService.reconcileWithGeneralLedger(LocalDate.parse(payload.asOfDate()), payload.glBalance());
    }
}
