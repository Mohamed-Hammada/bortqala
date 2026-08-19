package com.bemo.hr.operations.api;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations/inventory/valuation")
public class InventoryValuationController {

    @PostMapping("/calculate")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_INVENTORY_MANAGER)
    public void calculateValuation() {
        throw legacyEndpointDisabled();
    }

    @PostMapping("/reconciliation")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_INVENTORY_MANAGER_VIEWER)
    public void reconcile() {
        throw legacyEndpointDisabled();
    }

    private BusinessRuleException legacyEndpointDisabled() {
        return new BusinessRuleException(
                "This legacy valuation endpoint is disabled. Use the authoritative movement-cost valuation report and finance reconciliation.",
                "INVENTORY_VALUATION_LEGACY_DISABLED", HttpStatus.NOT_IMPLEMENTED);
    }
}
