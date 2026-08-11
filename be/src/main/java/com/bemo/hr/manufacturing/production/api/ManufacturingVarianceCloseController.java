package com.bemo.hr.manufacturing.production.api;

import com.bemo.hr.manufacturing.production.application.ManufacturingVarianceCloseService;
import com.bemo.hr.manufacturing.production.domain.ProductionVarianceClose;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/manufacturing/variance")
public class ManufacturingVarianceCloseController {

    private final ManufacturingVarianceCloseService varianceService;

    public ManufacturingVarianceCloseController(ManufacturingVarianceCloseService varianceService) {
        this.varianceService = varianceService;
    }

    public record CloseVariancePayload(String workOrderId, BigDecimal standardCost, BigDecimal actualCost) {}

    @PostMapping("/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRODUCTION_MANAGER', 'FINANCE_MANAGER')")
    public ProductionVarianceClose calculateAndCloseVariance(@RequestBody CloseVariancePayload payload) {
        return varianceService.calculateAndCloseVariance(payload.workOrderId(), payload.standardCost(), payload.actualCost());
    }

    @GetMapping("/work-orders/{workOrderId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRODUCTION_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public ProductionVarianceClose getVarianceClose(@PathVariable String workOrderId) {
        return varianceService.getVarianceClose(workOrderId);
    }
}
