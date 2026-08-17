package com.bemo.hr.attendance.api;

import java.util.List;

public final class AttendanceExplorerApi {
    private AttendanceExplorerApi() {
    }

    public record MonthSummaryResponse(
            String month,
            long punchCount,
            long employeeCount,
            long mappedEmployeeCount,
            long unmatchedEmployeeCount,
            long firstPunch,
            long lastPunch) {
    }

    public record EmployeeSummaryResponse(
            String deviceUserId,
            String observedName,
            String employeeId,
            String employeeCode,
            String employeeName,
            boolean mapped,
            long punchCount,
            long firstPunch,
            long lastPunch) {
    }

    public record AttendanceDayResponse(
            String date,
            long firstPunch,
            Long lastPunch,
            long punchCount,
            long workedMinutes,
            boolean incomplete,
            List<Long> punches) {
    }

    public record EmployeeAttendanceResponse(
            String deviceUserId,
            String observedName,
            String employeeId,
            String employeeCode,
            String employeeName,
            boolean mapped,
            String month,
            long punchCount,
            long firstPunch,
            long lastPunch,
            long workedMinutes,
            List<AttendanceDayResponse> days) {
    }
}
