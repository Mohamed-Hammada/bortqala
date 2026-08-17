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

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    DashboardApi.Response dashboard(@RequestParam(required = false) Integer year,
                                    @RequestParam(required = false) Integer month) {
        var current = YearMonth.now();
        int targetYear = (year != null && year >= 2000 && year <= 2100) ? year : current.getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : current.getMonthValue();
        return dashboardService.dashboard(targetYear, targetMonth);
    }

    @GetMapping("/summary")
    DashboardApi.Response summary(@RequestParam(required = false) Integer year,
                                  @RequestParam(required = false) Integer month) {
        return dashboard(year, month);
    }

    @GetMapping("/attendance-chart")
    java.util.List<DashboardApi.AttendanceChartPoint> attendanceChart(
            @RequestParam(defaultValue = "MONTH") String period,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        var current = YearMonth.now();
        int y = (year != null && year >= 2000 && year <= 2100) ? year : current.getYear();
        int m = (month != null && month >= 1 && month <= 12) ? month : current.getMonthValue();
        return dashboardService.attendanceChart(period, departmentId, y, m);
    }

    @GetMapping("/payroll-summary")
    DashboardApi.PayrollSummaryRecord payrollSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        var current = YearMonth.now();
        int y = (year != null && year >= 2000 && year <= 2100) ? year : current.getYear();
        int m = (month != null && month >= 1 && month <= 12) ? month : current.getMonthValue();
        return dashboardService.payrollSummary(y, m);
    }

    @GetMapping("/department-metrics")
    java.util.List<DashboardApi.DepartmentMetric> departmentMetrics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        var current = YearMonth.now();
        int y = (year != null && year >= 2000 && year <= 2100) ? year : current.getYear();
        int m = (month != null && month >= 1 && month <= 12) ? month : current.getMonthValue();
        return dashboardService.departmentMetrics(y, m);
    }

    @GetMapping("/trends")
    java.util.List<DashboardApi.TrendPoint> trends(
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        var current = YearMonth.now();
        int y = (year != null && year >= 2000 && year <= 2100) ? year : current.getYear();
        int m = (month != null && month >= 1 && month <= 12) ? month : current.getMonthValue();
        return dashboardService.trends(months, y, m);
    }
}

// BORTQALA_ATTENDANCE_PIPELINE_20260816_V1_TREND_API_PERIOD
