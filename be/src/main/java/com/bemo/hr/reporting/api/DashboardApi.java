package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.domain.ReportStatus;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public final class DashboardApi {
    private DashboardApi() { }

    public record Response(
            int year,
            int month,
            Instant updatedAt,
            long activeEmployees,
            long activeCategories,
            ReportStatus reportStatus,
            String reportId,
            int unresolvedCount,
            long scheduledEmployeeDays,
            long presentEmployeeDays,
            double attendanceRate,
            long lateEmployeeDays,
            long singlePunchDays,
            long overtimeMinutes,
            long unmatchedIdentities,
            long importedPunches,
            long totalStockMovements,
            long totalInventoryItems,
            long lowStockCount,
            long negativeStockCount,
            long totalPartnerEntries,
            long activePartiesCount,
            List<CategoryMetric> categories,
            List<RecentImport> recentImports
    ) { }
    public record CategoryMetric(String categoryId, String categoryName, long employeeDays, long presentDays,
                                 long exceptionDays, LocalTime typicalArrival, long overtimeMinutes) { }
    public record RecentImport(String id, String fileName, String deviceName, int importedRows, int errorRows, Instant importedAt) { }
}
