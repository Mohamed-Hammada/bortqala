package com.bemo.hr.reporting.domain;

import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.domain.ScheduleRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyAttendanceCalculatorTests {
    private static final ZoneId COMPANY_ZONE = ZoneId.of("Africa/Cairo");
    private static final LocalDate WORKDAY = LocalDate.of(2026, 7, 20);

    @Test
    void manualCategoryWithoutPunchesRequiresManualConfirmationInsteadOfDeduction() {
        var category = category(AttendanceMode.MANUAL, false, 480);
        var result = calculate(category, schedule(category, null), List.of());

        assertThat(result.getStatus()).isEqualTo(DailyStatus.MANUAL_ENTRY);
        assertThat(result.isBlocking()).isTrue();
        assertThat(result.getWarning()).contains("Manual attendance");
    }

    @Test
    void biometricCategoryWithoutPunchesCreatesNoPunchException() {
        var category = category(AttendanceMode.BIOMETRIC, false, 480);
        var result = calculate(category, schedule(category, null), List.of());

        assertThat(result.getStatus()).isEqualTo(DailyStatus.NO_PUNCH);
        assertThat(result.isBlocking()).isTrue();
    }

    @Test
    void effectiveScheduleOverrideControlsExpectedLateAndOvertimeMinutes() {
        var category = category(AttendanceMode.BIOMETRIC, false, 480);
        var schedule = schedule(category, 600);
        var first = WORKDAY.atTime(9, 20).atZone(COMPANY_ZONE).toInstant();
        var last = WORKDAY.atTime(19, 35).atZone(COMPANY_ZONE).toInstant();

        var result = calculate(category, schedule, List.of(last, first));

        assertThat(result.getStatus()).isEqualTo(DailyStatus.PRESENT);
        assertThat(result.getExpectedMinutes()).isEqualTo(600);
        assertThat(result.getWorkedMinutes()).isEqualTo(615);
        assertThat(result.getLateMinutes()).isEqualTo(5);
        assertThat(result.getEarlyLeaveMinutes()).isZero();
        assertThat(result.getOvertimeMinutes()).isEqualTo(15);
    }

    @Test
    void onePunchPolicyCanCountPresenceWithoutBlockingMonthlyReview() {
        var category = category(AttendanceMode.BIOMETRIC, true, 480);
        var punch = WORKDAY.atTime(9, 3).atZone(COMPANY_ZONE).toInstant();

        var result = calculate(category, schedule(category, null), List.of(punch));

        assertThat(result.getStatus()).isEqualTo(DailyStatus.PRESENT);
        assertThat(result.isBlocking()).isFalse();
        assertThat(result.getPunchCount()).isOne();
    }

    private DailyAttendanceResult calculate(AttendanceCategory category, ScheduleRule schedule,
                                             List<java.time.Instant> punches) {
        var employee = new Employee("EMP-1", "Test Employee", "42", category.getId(),
                EmploymentType.FIXED, WORKDAY.minusYears(1), null, true);
        return DailyAttendanceCalculator.calculate("report-1", employee, category, schedule,
                WORKDAY, punches, false, COMPANY_ZONE);
    }

    private AttendanceCategory category(AttendanceMode mode, boolean singlePunchCounts, int expectedMinutes) {
        return new AttendanceCategory("TEST", "Test Category", expectedMinutes, PayCycle.MONTHLY,
                mode, singlePunchCounts, 127, true);
    }

    private ScheduleRule schedule(AttendanceCategory category, Integer expectedOverride) {
        return new ScheduleRule(category.getId(), "Summer", WORKDAY.minusMonths(1), null,
                LocalTime.of(9, 0), expectedOverride, 15);
    }
}
