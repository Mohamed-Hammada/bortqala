package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.shared.security.Roles;
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

    @PostMapping("/reconcile")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_PROCUREMENT_MANAGER)
    public GrirReconciliationRecord reconcileLine(@RequestBody ReconcileLinePayload payload) {
        return grirService.reconcileLine(payload.goodsReceiptLineId(), payload.invoiceLineId(), payload.receivedAmount(), payload.invoicedAmount());
    }

    @PostMapping("/{id}/close")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public GrirReconciliationRecord closeRecord(@PathVariable String id) {
        return grirService.closeRecord(id);
    }

    @GetMapping("/summary")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_PROCUREMENT_MANAGER_VIEWER)
    public GrirReconciliationService.GrirSummaryReport getSummaryReport() {
        return grirService.getSummaryReport();
    }

    public record ReconcileLinePayload(String goodsReceiptLineId, String invoiceLineId, BigDecimal receivedAmount,
                                       BigDecimal invoicedAmount) {
    }
}
