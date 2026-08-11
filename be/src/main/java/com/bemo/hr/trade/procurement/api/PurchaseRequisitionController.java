package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.trade.procurement.application.PurchaseRequisitionService;
import com.bemo.hr.trade.procurement.domain.PurchaseRequisition;
import com.bemo.hr.trade.procurement.domain.PurchaseRequisitionLine;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/procurement/requisitions")
public class PurchaseRequisitionController {

    private final PurchaseRequisitionService requisitionService;

    public PurchaseRequisitionController(PurchaseRequisitionService requisitionService) {
        this.requisitionService = requisitionService;
    }

    public record CreateRequisitionPayload(String requisitionNumber, String departmentId, String requestedBy) {}
    public record AddRequisitionLinePayload(String itemId, String itemName, BigDecimal requestedQuantity, BigDecimal unitPriceEstimate, String notes) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'PURCHASER')")
    public PurchaseRequisition createRequisition(@RequestBody CreateRequisitionPayload payload) {
        return requisitionService.createRequisition(payload.requisitionNumber(), payload.departmentId(), payload.requestedBy());
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'PURCHASER')")
    public PurchaseRequisitionLine addLine(@PathVariable String id, @RequestBody AddRequisitionLinePayload payload) {
        return requisitionService.addRequisitionLine(id, payload.itemId(), payload.itemName(), payload.requestedQuantity(), payload.unitPriceEstimate(), payload.notes());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'PURCHASER')")
    public PurchaseRequisition submit(@PathVariable String id) {
        return requisitionService.submitRequisition(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER')")
    public PurchaseRequisition approve(@PathVariable String id) {
        return requisitionService.approveRequisition(id);
    }

    @GetMapping("/approved")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'PURCHASER', 'VIEWER')")
    public List<PurchaseRequisition> getApprovedRequisitions() {
        return requisitionService.getApprovedRequisitions();
    }

    @GetMapping("/{id}/lines")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'PURCHASER', 'VIEWER')")
    public List<PurchaseRequisitionLine> getLines(@PathVariable String id) {
        return requisitionService.getRequisitionLines(id);
    }
}
