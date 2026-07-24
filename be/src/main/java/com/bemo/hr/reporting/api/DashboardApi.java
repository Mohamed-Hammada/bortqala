package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.domain.ReportStatus;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public final class DashboardApi {
    private DashboardApi() { }

    public record Response(int year, int month, long activeEmployees, long activeCategories,
                           ReportStatus reportStatus, String reportId, int unresolvedCount,
                           long scheduledEmployeeDays, long presentEmployeeDays, double attendanceRate,
                           long lateEmployeeDays, long overtimeMinutes, long unmatchedIdentities,
                           long importedPunches, List<CategoryMetric> categories, List<RecentImport> recentImports) { }
    public record CategoryMetric(String categoryId, String categoryName, long employeeDays, long presentDays,
                                 long exceptionDays, LocalTime typicalArrival, long overtimeMinutes) { }
    public record RecentImport(String id, String fileName, String deviceName, int importedRows, int errorRows, Instant importedAt) { }
}
