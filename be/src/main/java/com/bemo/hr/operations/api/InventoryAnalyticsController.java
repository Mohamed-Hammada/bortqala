package com.bemo.hr.operations.api;

import com.bemo.hr.operations.application.InventoryAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operations/analytics")
@RequiredArgsConstructor
public class InventoryAnalyticsController {

    private final InventoryAnalyticsService analyticsService;

    @GetMapping("/aging")
    public ResponseEntity<InventoryAnalyticsService.StockAgingSummary> getStockAgingSummary() {
        return ResponseEntity.ok(analyticsService.getStockAgingSummary());
    }

    @GetMapping("/dead-stock")
    public ResponseEntity<List<InventoryAnalyticsService.DeadStockItem>> getDeadStock(
            @RequestParam(name = "thresholdDays", defaultValue = "90") int thresholdDays) {
        return ResponseEntity.ok(analyticsService.getDeadStockItems(thresholdDays));
    }

    @GetMapping("/reorder-alerts")
    public ResponseEntity<List<InventoryAnalyticsService.ReorderAlertItem>> getReorderAlerts() {
        return ResponseEntity.ok(analyticsService.getReorderAlerts());
    }

    @GetMapping("/project/{projectId}/materials")
    public ResponseEntity<List<InventoryAnalyticsService.ProjectMaterialLine>> getProjectMaterials(
            @PathVariable String projectId) {
        return ResponseEntity.ok(analyticsService.getProjectMaterials(projectId));
    }

    @GetMapping("/barcode-lookup")
    public ResponseEntity<InventoryAnalyticsService.BarcodeLookupResult> lookupBarcode(
            @RequestParam String barcode) {
        return analyticsService.lookupBarcode(barcode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
