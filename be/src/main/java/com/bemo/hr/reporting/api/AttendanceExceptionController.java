package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.application.AttendanceExceptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AttendanceExceptionController {
    private final AttendanceExceptionService service;

    @GetMapping("/attendance/policies")
    @PreAuthorize("@auth.hasAnyPermission('attendance.read', 'reports.read')")
    List<AttendanceExceptionApi.PolicyResponse> policies() {
        return service.policies();
    }

    @PostMapping("/attendance/policies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasAnyPermission('attendance.review', 'reports.decide')")
    AttendanceExceptionApi.PolicyResponse createPolicy(@Valid @RequestBody AttendanceExceptionApi.PolicyRequest request, Authentication auth) {
        return service.createPolicy(request, auth.getName());
    }

    @PostMapping("/reports/{reportId}/attendance-exceptions/detect")
    @PreAuthorize("@auth.hasPermission('reports.decide')")
    int detect(@PathVariable String reportId, Authentication auth) {
        return service.detect(reportId, auth.getName());
    }

    @GetMapping("/reports/{reportId}/attendance-exceptions")
    @PreAuthorize("@auth.hasAnyPermission('reports.read', 'payroll.read')")
    AttendanceExceptionApi.WorkbenchResponse workbench(@PathVariable String reportId) {
        return service.workbench(reportId);
    }

    @PostMapping("/reports/{reportId}/attendance-exceptions/bulk-preview")
    @PreAuthorize("@auth.hasPermission('reports.decide')")
    AttendanceExceptionApi.BulkPreview preview(@PathVariable String reportId, @Valid @RequestBody AttendanceExceptionApi.BulkRequest request) {
        return service.preview(reportId, request);
    }

    @PostMapping("/reports/{reportId}/attendance-exceptions/bulk-resolve")
    @PreAuthorize("@auth.hasPermission('reports.decide')")
    AttendanceExceptionApi.BulkResult apply(@PathVariable String reportId, @Valid @RequestBody AttendanceExceptionApi.BulkRequest request, Authentication auth) {
        return service.apply(reportId, request, auth.getName());
    }
}
