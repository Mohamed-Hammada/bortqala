package com.bemo.hr.workforce;

import com.bemo.hr.shared.security.Roles;
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
    @PreAuthorize(Roles.ADMIN_WORKFORCE_FINANCE_WORKFORCE_MANAGER_WORKFORCE_REVIEWER)
    public List<ManualAttendanceEntry> getByRange(@RequestParam String startDate, @RequestParam String endDate) {
        return attendanceService.getByDateRange(startDate, endDate);
    }

    @PostMapping("/batch")
    @PreAuthorize(Roles.ADMIN_WORKFORCE_MANAGER)
    public WorkforceApi.BatchAttendanceResponse saveBatch(@Valid @RequestBody WorkforceApi.BatchAttendanceRequest request) {
        return attendanceService.saveBatch(request);
    }

    @PostMapping("/bulk-update")
    @PreAuthorize(Roles.ADMIN_WORKFORCE_MANAGER)
    public WorkforceApi.BulkUpdateAttendanceResponse bulkUpdate(
            @Valid @RequestBody WorkforceApi.BulkUpdateAttendanceRequest request) {
        return attendanceService.bulkUpdate(request);
    }

    @GetMapping("/calculation-rules")
    @PreAuthorize(Roles.ADMIN_WORKFORCE_FINANCE_WORKFORCE_MANAGER_WORKFORCE_REVIEWER)
    public WorkforceApi.CalculationRulesResponse getCalculationRules(
            @RequestParam(required = false) String date) {
        return attendanceService.getCalculationRules(date);
    }
}
