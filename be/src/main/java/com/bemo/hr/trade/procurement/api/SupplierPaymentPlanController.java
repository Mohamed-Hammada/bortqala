package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.trade.procurement.application.SupplierPaymentPlanService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/supplier-invoices")
public class SupplierPaymentPlanController {

    private final SupplierPaymentPlanService supplierPaymentPlanService;

    public SupplierPaymentPlanController(SupplierPaymentPlanService supplierPaymentPlanService) {
        this.supplierPaymentPlanService = supplierPaymentPlanService;
    }

    @PostMapping("/{id}/payment-plan")
    @Transactional
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public List<ProcurementApi.SupplierPaymentPlanResponse> createPaymentPlan(
            @PathVariable String id,
            @Valid @RequestBody ProcurementApi.SupplierPaymentPlanPayload payload) {
        return supplierPaymentPlanService.createPaymentPlan(id, payload);
    }

    @GetMapping("/{id}/payment-plan")
    @PreAuthorize("isAuthenticated()")
    public List<ProcurementApi.SupplierPaymentPlanResponse> listPaymentPlan(@PathVariable String id) {
        return supplierPaymentPlanService.listPaymentPlans(id);
    }
}
