package com.bemo.hr.project.api;

import com.bemo.hr.project.api.DailyReportApi.*;
import com.bemo.hr.project.application.ProjectDailyReportService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/daily-reports")
public class ProjectDailyReportController {

    private final ProjectDailyReportService dailyReportService;

    public ProjectDailyReportController(ProjectDailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    @GetMapping
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public List<DailyReportResponse> listDailyReports(@PathVariable String projectId) {
        return dailyReportService.listDailyReports(projectId);
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public DailyReportResponse getDailyReport(
            @PathVariable String projectId,
            @PathVariable String reportId) {
        return dailyReportService.getDailyReport(projectId, reportId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public DailyReportResponse createDailyReport(
            @PathVariable String projectId,
            @Valid @RequestBody CreateDailyReportRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return dailyReportService.createDailyReport(projectId, req, userId);
    }

    @PutMapping("/{reportId}")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
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
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public void deleteDailyReport(
            @PathVariable String projectId,
            @PathVariable String reportId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        dailyReportService.deleteDailyReport(projectId, reportId, userId);
    }

    @PostMapping("/{reportId}/submit")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public DailyReportResponse submitDailyReport(
            @PathVariable String projectId,
            @PathVariable String reportId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return dailyReportService.submitDailyReport(projectId, reportId, userId);
    }

    @PostMapping("/{reportId}/approve")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public DailyReportResponse approveDailyReport(
            @PathVariable String projectId,
            @PathVariable String reportId,
            Authentication auth) {
        String approverId = auth != null ? auth.getName() : "ADMIN";
        return dailyReportService.approveDailyReport(projectId, reportId, approverId);
    }

    @PostMapping("/{reportId}/reopen")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
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
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public DailyReportResponse copyPreviousDay(
            @PathVariable String projectId,
            @RequestParam Long targetDate,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return dailyReportService.copyPreviousDay(projectId, targetDate, userId);
    }

    @GetMapping("/summary")
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public DprPeriodSummaryResponse getPeriodSummary(
            @PathVariable String projectId,
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        return dailyReportService.getPeriodSummary(projectId, startDate, endDate);
    }

    @GetMapping("/{reportId}/attachments")
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public List<AttachmentResponse> listAttachments(
            @PathVariable String projectId,
            @PathVariable String reportId) {
        return dailyReportService.listAttachments(projectId, reportId);
    }

    @PostMapping(value = "/{reportId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public AttachmentResponse addAttachment(
            @PathVariable String projectId,
            @PathVariable String reportId,
            @RequestPart(value = "metadata", required = false) AttachmentRequest metadata,
            @RequestPart("file") MultipartFile file,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return dailyReportService.addAttachment(projectId, reportId, metadata, file, userId);
    }

    @GetMapping("/{reportId}/attachments/{attachmentId}/download")
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable String projectId,
            @PathVariable String reportId,
            @PathVariable String attachmentId) {
        var attachment = dailyReportService.downloadAttachment(projectId, reportId, attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(attachment.getFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(attachment.contentCopy());
    }

    @DeleteMapping("/{reportId}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public void deleteAttachment(
            @PathVariable String projectId,
            @PathVariable String reportId,
            @PathVariable String attachmentId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        dailyReportService.deleteAttachment(projectId, reportId, attachmentId, userId);
    }
}
