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
        int targetYear = (year != null && year >= 2000 && year <= 2100) ? year : current.getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : current.getMonthValue();
        return dashboardService.dashboard(targetYear, targetMonth);
    }
}
