package com.bemo.hr.analytics.api;

import com.bemo.hr.analytics.api.ExecutiveAnalyticsApi.*;
import com.bemo.hr.analytics.application.ExecutiveAnalyticsService;
import com.bemo.hr.analytics.domain.KpiCategory;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    // 2026-09-06: removed the `companyId`/`branchId`/`projectId` parameters this endpoint used to
    // accept — none were ever used to filter anything (docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md,
    // Medium Finding M-1 / API Contract Review). A parameter that is silently ignored is worse than
    // no parameter at all.
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ExecutiveOverviewResponse getExecutiveOverview(@RequestParam(required = false) String period) {
        return analyticsService.getExecutiveOverview(period);
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

    // 2026-09-06: removed the unused `companyId` parameter (see note above); `branchId` is kept —
    // it is genuinely enforced via SecurityAuthorizationEvaluator.hasBranchAccess and used to filter
    // the branch leaderboard.
    @GetMapping("/cockpit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public OwnerCockpitResponse getOwnerCockpit(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String branchId
    ) {
        return analyticsService.getOwnerCockpit(period, branchId);
    }

    @GetMapping(value = "/cockpit/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public ResponseEntity<byte[]> exportExecutiveCockpit(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String branchId
    ) {
        byte[] bytes = analyticsService.exportExecutiveCockpitExcel(period, branchId);
        String filename = "Executive_Cockpit_" + (period != null && !period.isBlank() ? period : "ALL") + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/targets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public CockpitTargetResponse getTargets(@RequestParam(required = false) String periodKey) {
        return analyticsService.getTargets(periodKey);
    }

    @PostMapping("/targets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'GENERAL_MANAGER')")
    public CockpitTargetResponse saveTargets(@Valid @RequestBody SaveCockpitTargetRequest request) {
        return analyticsService.saveTargets(request);
    }
}

