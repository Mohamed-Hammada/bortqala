package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.trade.procurement.application.ProcurementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/procurement")
public class ProcurementController {

    private final ProcurementService procurementService;

    public ProcurementController(ProcurementService procurementService) {
        this.procurementService = procurementService;
    }

    @GetMapping("/orders")
    public List<ProcurementApi.PurchaseOrderResponse> listPurchaseOrders() {
        return procurementService.list();
    }

    @PostMapping("/orders")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ProcurementApi.PurchaseOrderResponse createPurchaseOrder(@Valid @RequestBody ProcurementApi.PurchaseOrderPayload payload) {
        return procurementService.create(payload);
    }

    @PostMapping("/orders/{id}/issue")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ProcurementApi.PurchaseOrderResponse issuePurchaseOrder(@PathVariable String id) {
        return procurementService.issue(id);
    }
}
