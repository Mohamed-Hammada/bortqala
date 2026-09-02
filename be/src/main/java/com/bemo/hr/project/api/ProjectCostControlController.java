package com.bemo.hr.project.api;

import com.bemo.hr.project.api.CostControlApi.*;
import com.bemo.hr.project.application.ProjectCostControlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/cost-control")
public class ProjectCostControlController {

    private final ProjectCostControlService costControlService;

    public ProjectCostControlController(ProjectCostControlService costControlService) {
        this.costControlService = costControlService;
    }

    @GetMapping("/summary")
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public CostControlSummaryResponse getSummary(@PathVariable String projectId) {
        return costControlService.getSummary(projectId);
    }

    @GetMapping("/budget-versions")
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public List<ProjectBudgetVersionResponse> listBudgetVersions(@PathVariable String projectId) {
        return costControlService.listBudgetVersions(projectId);
    }

    @GetMapping("/budget-versions/{versionId}")
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public ProjectBudgetVersionResponse getBudgetVersion(
            @PathVariable String projectId,
            @PathVariable String versionId) {
        return costControlService.getBudgetVersion(versionId);
    }

    @PostMapping("/budget-versions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public ProjectBudgetVersionResponse createBudgetVersion(
            @PathVariable String projectId,
            @Valid @RequestBody CreateBudgetVersionRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return costControlService.createBudgetVersion(projectId, req, userId);
    }

    @PostMapping("/budget-versions/{versionId}/approve")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public ProjectBudgetVersionResponse approveBudgetVersion(
            @PathVariable String projectId,
            @PathVariable String versionId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return costControlService.approveBudgetVersion(versionId, userId);
    }

    @GetMapping("/ledger")
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public List<ProjectCostLedgerEntryResponse> listCostLedgerEntries(@PathVariable String projectId) {
        return costControlService.listCostLedgerEntries(projectId);
    }

    @PostMapping("/ledger")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public ProjectCostLedgerEntryResponse recordCostLedgerEntry(
            @PathVariable String projectId,
            @Valid @RequestBody RecordCostLedgerEntryRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return costControlService.recordCostLedgerEntry(projectId, req, userId);
    }

    @GetMapping("/forecast-eac")
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public List<ProjectForecastEacResponse> listForecastEac(@PathVariable String projectId) {
        return costControlService.listForecastEac(projectId);
    }

    @PutMapping("/forecast-eac")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public ProjectForecastEacResponse updateForecastEac(
            @PathVariable String projectId,
            @Valid @RequestBody UpdateForecastEacRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return costControlService.updateForecastEac(projectId, req, userId);
    }
}
