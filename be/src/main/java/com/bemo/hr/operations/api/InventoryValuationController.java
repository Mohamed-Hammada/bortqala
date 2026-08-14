package com.bemo.hr.operations.api;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operations/inventory/valuation")
public class InventoryValuationController {

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'FINANCE_MANAGER')")
    public void calculateValuation() {
        throw legacyEndpointDisabled();
    }

    @PostMapping("/reconciliation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public void reconcile() {
        throw legacyEndpointDisabled();
    }

    private BusinessRuleException legacyEndpointDisabled() {
        return new BusinessRuleException(
                "This legacy valuation endpoint is disabled. Use the authoritative movement-cost valuation report and finance reconciliation.",
                "INVENTORY_VALUATION_LEGACY_DISABLED", HttpStatus.NOT_IMPLEMENTED);
    }
}
