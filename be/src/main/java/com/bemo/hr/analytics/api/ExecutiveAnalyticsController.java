package com.bemo.hr.analytics.api;

import com.bemo.hr.analytics.api.ExecutiveAnalyticsApi.*;
import com.bemo.hr.analytics.application.ExecutiveAnalyticsService;
import com.bemo.hr.analytics.domain.KpiCategory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics/executive")
public class ExecutiveAnalyticsController {

    private final ExecutiveAnalyticsService analyticsService;

    public ExecutiveAnalyticsController(ExecutiveAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/kpi-registry")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public List<KpiDefinitionResponse> getKpiRegistry() {
        return analyticsService.getKpiRegistry();
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ExecutiveOverviewResponse getExecutiveOverview(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String projectId
    ) {
        return analyticsService.getExecutiveOverview(period, companyId, branchId, projectId);
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ComparativeTrendsResponse getComparativeTrends(
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) KpiCategory category
    ) {
        return analyticsService.getComparativeTrends(months, category);
    }

    @GetMapping("/snapshots")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public List<ExecutiveKpiSnapshotResponse> listSnapshots(
            @RequestParam(required = false) String periodKey
    ) {
        return analyticsService.listSnapshots(periodKey);
    }

    @PostMapping("/snapshots")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ExecutiveKpiSnapshotResponse recordSnapshot(@Valid @RequestBody CreateSnapshotPayload payload) {
        return analyticsService.recordSnapshot(payload);
    }
}
