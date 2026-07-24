package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.application.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }

    @GetMapping
    DashboardApi.Response dashboard(@RequestParam(required = false) Integer year,
                                    @RequestParam(required = false) Integer month) {
        var current = YearMonth.now();
        return dashboardService.dashboard(year == null ? current.getYear() : year, month == null ? current.getMonthValue() : month);
    }
}
