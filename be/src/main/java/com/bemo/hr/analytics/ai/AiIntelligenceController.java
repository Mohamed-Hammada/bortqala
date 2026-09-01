package com.bemo.hr.analytics.ai;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AiIntelligenceController {

    private final AiIntelligenceService aiService;

    public AiIntelligenceController(AiIntelligenceService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/cashflow-forecast")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'AUDITOR', 'VIEWER')")
    public ResponseEntity<AiIntelligenceApi.CashFlowForecastResponse> getCashFlowForecast(
            @RequestParam(defaultValue = "3") int months) {
        return ResponseEntity.ok(aiService.getCashFlowForecast(months));
    }

    @GetMapping("/expense-anomalies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'AUDITOR', 'VIEWER')")
    public ResponseEntity<List<AiIntelligenceApi.ExpenseAnomalyDto>> getExpenseAnomalies() {
        return ResponseEntity.ok(aiService.detectExpenseAnomalies());
    }

    @GetMapping("/demand-forecast")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'PROCUREMENT_MANAGER', 'AUDITOR', 'VIEWER')")
    public ResponseEntity<List<AiIntelligenceApi.DemandForecastDto>> getDemandForecast() {
        return ResponseEntity.ok(aiService.getDemandForecast());
    }

    @GetMapping("/collections-risk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'SALES_MANAGER', 'AUDITOR', 'VIEWER')")
    public ResponseEntity<List<AiIntelligenceApi.CollectionsRiskDto>> getCollectionsRisk() {
        return ResponseEntity.ok(aiService.getCollectionsRisk());
    }

    @PostMapping("/nl-query")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'AUDITOR')")
    public ResponseEntity<AiIntelligenceApi.NlQueryResponse> executeNlQuery(
            @Valid @RequestBody AiIntelligenceApi.NlQueryRequest request) {
        return ResponseEntity.ok(aiService.executeNlQuery(request));
    }
}
