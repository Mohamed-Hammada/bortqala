package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.domain.ReportStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public final class DashboardApi {
    private DashboardApi() {
    }

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
    ) {
    }

    public record CategoryMetric(String categoryId, String categoryName, long employeeDays, long presentDays,
                                 long exceptionDays, LocalTime typicalArrival, long overtimeMinutes) {
    }

    public record RecentImport(String id, String fileName, String deviceName, int importedRows, int errorRows,
                               Instant importedAt) {
    }

    public record AttendanceChartPoint(String label, long present, long absent, long late, long exception) {
    }

    public record PayrollSummaryRecord(int totalEmployees, int paidCount, int pendingCount,
                                       BigDecimal totalGross, BigDecimal totalPaid, BigDecimal totalPending) {
    }

    public record DepartmentMetric(String departmentId, String departmentName, int employeeCount,
                                   long presentDays, long scheduledDays, double rate) {
    }

    public record TrendPoint(String label, int year, int month, long scheduledEmployeeDays, long presentEmployeeDays,
                             double attendanceRate, long exceptionDays, long overtimeMinutes,
                             int paidCount, int pendingCount, BigDecimal totalGross, BigDecimal totalPaid) {
    }
}
