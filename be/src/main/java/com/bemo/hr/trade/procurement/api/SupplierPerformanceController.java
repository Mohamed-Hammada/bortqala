package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.trade.procurement.application.SupplierPerformanceService;
import com.bemo.hr.trade.procurement.application.SupplierPerformanceService.SupplierScorecardResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/procurement/suppliers")
public class SupplierPerformanceController {

    private final SupplierPerformanceService supplierPerformanceService;

    public SupplierPerformanceController(SupplierPerformanceService supplierPerformanceService) {
        this.supplierPerformanceService = supplierPerformanceService;
    }

    @GetMapping("/scorecards")
    @PreAuthorize("hasAnyAuthority('procurement.read', 'procurement.manage', 'projects.read', 'finance.read', 'platform.admin')")
    public List<SupplierScorecardResponse> getSupplierScorecards() {
        return supplierPerformanceService.getSupplierScorecards();
    }

    @GetMapping("/{supplierId}/scorecard")
    @PreAuthorize("hasAnyAuthority('procurement.read', 'procurement.manage', 'projects.read', 'finance.read', 'platform.admin')")
    public SupplierScorecardResponse getSupplierScorecard(@PathVariable String supplierId) {
        return supplierPerformanceService.getSupplierScorecard(supplierId);
    }
}
