package com.bemo.hr.project.api;

import com.bemo.hr.project.api.DailyReportApi.*;
import com.bemo.hr.project.application.ProjectDailyReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/daily-reports")
public class ProjectDailyReportController {

    private final ProjectDailyReportService dailyReportService;

    public ProjectDailyReportController(ProjectDailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('projects.read', 'projects.manage')")
    public List<DailyReportResponse> listDailyReports(@PathVariable String projectId) {
        return dailyReportService.listDailyReports(projectId);
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyAuthority('projects.read', 'projects.manage')")
    public DailyReportResponse getDailyReport(
            @PathVariable String projectId,
            @PathVariable String reportId) {
        return dailyReportService.getDailyReport(projectId, reportId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('projects.manage')")
    public DailyReportResponse createDailyReport(
            @PathVariable String projectId,
            @Valid @RequestBody CreateDailyReportRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return dailyReportService.createDailyReport(projectId, req, userId);
    }

    @PutMapping("/{reportId}")
    @PreAuthorize("hasAuthority('projects.manage')")
    public DailyReportResponse updateDailyReport(
            @PathVariable String projectId,
            @PathVariable String reportId,
            @Valid @RequestBody UpdateDailyReportRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return dailyReportService.updateDailyReport(projectId, reportId, req, userId);
    }

    @DeleteMapping("/{reportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('projects.manage')")
    public void deleteDailyReport(
            @PathVariable String projectId,
            @PathVariable String reportId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        dailyReportService.deleteDailyReport(projectId, reportId, userId);
    }

    @PostMapping("/{reportId}/submit")
    @PreAuthorize("hasAuthority('projects.manage')")
    public DailyReportResponse submitDailyReport(
            @PathVariable String projectId,
            @PathVariable String reportId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return dailyReportService.submitDailyReport(projectId, reportId, userId);
    }

    @PostMapping("/{reportId}/approve")
    @PreAuthorize("hasAuthority('projects.manage')")
    public DailyReportResponse approveDailyReport(
            @PathVariable String projectId,
            @PathVariable String reportId,
            Authentication auth) {
        String approverId = auth != null ? auth.getName() : "ADMIN";
        return dailyReportService.approveDailyReport(projectId, reportId, approverId);
    }

    @PostMapping("/{reportId}/reopen")
    @PreAuthorize("hasAuthority('projects.manage')")
    public DailyReportResponse reopenDailyReport(
            @PathVariable String projectId,
            @PathVariable String reportId,
            @RequestBody(required = false) ReopenReportRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        String reason = req != null ? req.reason() : null;
        return dailyReportService.reopenDailyReport(projectId, reportId, reason, userId);
    }

    @PostMapping("/copy-previous")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('projects.manage')")
    public DailyReportResponse copyPreviousDay(
            @PathVariable String projectId,
            @RequestParam Long targetDate,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return dailyReportService.copyPreviousDay(projectId, targetDate, userId);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('projects.read', 'projects.manage')")
    public DprPeriodSummaryResponse getPeriodSummary(
            @PathVariable String projectId,
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        return dailyReportService.getPeriodSummary(projectId, startDate, endDate);
    }
}
