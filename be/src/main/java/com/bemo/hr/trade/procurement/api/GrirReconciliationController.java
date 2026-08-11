package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.trade.procurement.application.GrirReconciliationService;
import com.bemo.hr.trade.procurement.domain.GrirReconciliationRecord;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/trade/procurement/grir")
public class GrirReconciliationController {

    private final GrirReconciliationService grirService;

    public GrirReconciliationController(GrirReconciliationService grirService) {
        this.grirService = grirService;
    }

    public record ReconcileLinePayload(String goodsReceiptLineId, String invoiceLineId, BigDecimal receivedAmount, BigDecimal invoicedAmount) {}

    @PostMapping("/reconcile")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    public GrirReconciliationRecord reconcileLine(@RequestBody ReconcileLinePayload payload) {
        return grirService.reconcileLine(payload.goodsReceiptLineId(), payload.invoiceLineId(), payload.receivedAmount(), payload.invoicedAmount());
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public GrirReconciliationRecord closeRecord(@PathVariable String id) {
        return grirService.closeRecord(id);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public GrirReconciliationService.GrirSummaryReport getSummaryReport() {
        return grirService.getSummaryReport();
    }
}
