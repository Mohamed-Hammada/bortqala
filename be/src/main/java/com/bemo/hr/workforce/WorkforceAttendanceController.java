package com.bemo.hr.workforce;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/attendance")
@RequiredArgsConstructor
public class WorkforceAttendanceController {
    private final WorkforceAttendanceService attendanceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE')")
    public List<ManualAttendanceEntry> getByRange(@RequestParam String startDate, @RequestParam String endDate) {
        return attendanceService.getByDateRange(startDate, endDate);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkforceApi.BatchAttendanceResponse saveBatch(@Valid @RequestBody WorkforceApi.BatchAttendanceRequest request) {
        return attendanceService.saveBatch(request);
    }

    @PostMapping("/bulk-update")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkforceApi.BulkUpdateAttendanceResponse bulkUpdate(
            @Valid @RequestBody WorkforceApi.BulkUpdateAttendanceRequest request) {
        return attendanceService.bulkUpdate(request);
    }

    @GetMapping("/calculation-rules")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE')")
    public WorkforceApi.CalculationRulesResponse getCalculationRules(
            @RequestParam(required = false) String date) {
        return attendanceService.getCalculationRules(date);
    }
}
