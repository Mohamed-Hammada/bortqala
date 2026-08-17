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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER','WORKFORCE_MANAGER')")
    List<AttendanceExceptionApi.PolicyResponse> policies() {
        return service.policies();
    }

    @PostMapping("/attendance/policies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')")
    AttendanceExceptionApi.PolicyResponse createPolicy(@Valid @RequestBody AttendanceExceptionApi.PolicyRequest request, Authentication auth) {
        return service.createPolicy(request, auth.getName());
    }

    @PostMapping("/reports/{reportId}/attendance-exceptions/detect")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER','HR_REVIEWER')")
    int detect(@PathVariable String reportId, Authentication auth) {
        return service.detect(reportId, auth.getName());
    }

    @GetMapping("/reports/{reportId}/attendance-exceptions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER','HR_REVIEWER','PAYROLL_MANAGER')")
    AttendanceExceptionApi.WorkbenchResponse workbench(@PathVariable String reportId) {
        return service.workbench(reportId);
    }

    @PostMapping("/reports/{reportId}/attendance-exceptions/bulk-preview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER','HR_REVIEWER')")
    AttendanceExceptionApi.BulkPreview preview(@PathVariable String reportId, @Valid @RequestBody AttendanceExceptionApi.BulkRequest request) {
        return service.preview(reportId, request);
    }

    @PostMapping("/reports/{reportId}/attendance-exceptions/bulk-resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER','HR_REVIEWER')")
    AttendanceExceptionApi.BulkResult apply(@PathVariable String reportId, @Valid @RequestBody AttendanceExceptionApi.BulkRequest request, Authentication auth) {
        return service.apply(reportId, request, auth.getName());
    }
}
