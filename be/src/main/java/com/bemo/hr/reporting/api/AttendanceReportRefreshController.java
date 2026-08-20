package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.application.AttendanceReportRefreshService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reporting/attendance")
public class AttendanceReportRefreshController {
    private final AttendanceReportRefreshService refreshService;

    public AttendanceReportRefreshController(AttendanceReportRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @PostMapping("/recalculate")
    @PreAuthorize("@auth.hasPermission('reports.decide')")
    public Map<String, Object> recalculate(@RequestParam int year, @RequestParam int month,
                                           Authentication authentication) {
        boolean refreshed = refreshService.refreshMonth(year, month, authentication.getName());
        return Map.of("year", year, "month", month, "refreshed", refreshed);
    }
}
