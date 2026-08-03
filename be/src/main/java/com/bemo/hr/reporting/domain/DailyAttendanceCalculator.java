package com.bemo.hr.reporting.domain;

import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.ScheduleRule;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

public final class DailyAttendanceCalculator {
    private DailyAttendanceCalculator() { }

    public static DailyAttendanceResult calculate(String reportId, Employee employee, AttendanceCategory category,
                                                  ScheduleRule schedule, LocalDate date, List<Instant> punches,
                                                  boolean confirmedHoliday, ZoneId companyZone) {
        int expected = schedule != null && schedule.getExpectedMinutesOverride() != null
                ? schedule.getExpectedMinutesOverride() : category.getExpectedDailyMinutes();
        String ruleVersion = "category:" + category.getVersion() + ":schedule:" + (schedule == null ? "missing" : schedule.getId());
        if (!isWorkday(category, date)) {
            return result(reportId, employee, category, date, null, null, 0, 0, 0, 0, 0, 0,
                    DailyStatus.NON_WORKDAY, null, ruleVersion);
        }
        if (confirmedHoliday) {
            return result(reportId, employee, category, date, null, null, 0, expected, 0, 0, 0, 0,
                    DailyStatus.HOLIDAY, null, ruleVersion);
        }
        if (schedule == null) {
            return result(reportId, employee, category, date, null, null, punches.size(), expected, 0, 0, 0, 0,
                    DailyStatus.MISSING_SCHEDULE, "No effective schedule rule for this workday.", ruleVersion);
        }
        var sorted = punches.stream().sorted().toList();
        if (sorted.isEmpty()) {
            var status = category.getAttendanceMode() == AttendanceMode.BIOMETRIC
                    ? DailyStatus.NO_PUNCH : DailyStatus.MANUAL_ENTRY;
            var warning = status == DailyStatus.NO_PUNCH ? "No biometric punch found." : "Manual attendance confirmation is required.";
            return result(reportId, employee, category, date, null, null, 0, expected, 0, 0, 0, 0,
                    status, warning, ruleVersion);
        }
        Instant first = sorted.get(0);
        Instant last = sorted.get(sorted.size() - 1);
        int worked = sorted.size() < 2 ? 0 : safeMinutes(Duration.between(first, last));
        ZonedDateTime scheduledStart = date.atTime(schedule.getStartTime()).atZone(companyZone);
        int late = Math.max(0, safeMinutes(Duration.between(scheduledStart.plusMinutes(schedule.getGraceMinutes()), first.atZone(companyZone))));
        ZonedDateTime scheduledEnd = scheduledStart.plusMinutes(expected);
        int earlyLeave = sorted.size() < 2 ? 0 : Math.max(0, safeMinutes(Duration.between(last.atZone(companyZone), scheduledEnd)));
        int overtime = Math.max(0, worked - expected);
        var status = sorted.size() == 1 && !category.isSinglePunchCounts()
                ? DailyStatus.SINGLE_PUNCH : DailyStatus.PRESENT;
        String warning = sorted.size() == 1
                ? (category.isSinglePunchCounts() ? "Presence counted from one punch by category policy." : "One punch is incomplete and requires review.")
                : null;
        return result(reportId, employee, category, date, first, last, sorted.size(), expected, worked,
                late, earlyLeave, overtime, status, warning, ruleVersion);
    }

    private static boolean isWorkday(AttendanceCategory category, LocalDate date) {
        int bit = 1 << (date.getDayOfWeek().getValue() - 1);
        return (category.getWorkDaysMask() & bit) != 0;
    }

    private static int safeMinutes(Duration duration) {
        long minutes = duration.toMinutes();
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, minutes));
    }

    private static DailyAttendanceResult result(String reportId, Employee employee, AttendanceCategory category,
                                                LocalDate date, Instant first, Instant last, int count, int expected,
                                                int worked, int late, int early, int overtime, DailyStatus status,
                                                String warning, String ruleVersion) {
        return new DailyAttendanceResult(reportId, employee.getId(), category.getId(), date,
                employee.getEmployeeCode(), employee.getFullName(), category.getName(), first, last, count,
                expected, worked, late, early, overtime, status, warning, ruleVersion);
    }
}
