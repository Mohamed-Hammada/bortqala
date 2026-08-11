package com.bemo.hr.employee.api;

import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.CategoryScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public final class CategoryApi {
    private CategoryApi() {
    }

    public record UpsertRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 150) String name,
            @Min(1) @Max(1_440) int expectedDailyMinutes,
            @NotNull PayCycle payCycle,
            @NotNull AttendanceMode attendanceMode,
            boolean singlePunchCounts,
            boolean allowsEmployeeAdvances,
            @NotEmpty Set<DayOfWeek> workDays,
            boolean active,
            CategoryScope scope,
            @NotNull @Valid List<ScheduleRequest> schedules,
            Long version) {
    }

    public record ScheduleRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo,
            @NotNull LocalTime startTime,
            @Min(1) @Max(1_440) Integer expectedMinutesOverride,
            @Min(0) @Max(240) int graceMinutes,
            LocalTime endTime,
            String scope,
            String scopeCategoryId) {
        public ScheduleRequest(String name, LocalDate effectiveFrom, LocalDate effectiveTo,
                               LocalTime startTime, Integer expectedMinutesOverride, int graceMinutes) {
            this(name, effectiveFrom, effectiveTo, startTime, expectedMinutesOverride, graceMinutes,
                    null, "ALL", null);
        }
    }

    public record Response(
            String id,
            String code,
            String name,
            CategoryScope scope,
            int expectedDailyMinutes,
            PayCycle payCycle,
            AttendanceMode attendanceMode,
            boolean singlePunchCounts,
            boolean allowsEmployeeAdvances,
            Set<DayOfWeek> workDays,
            boolean active,
            long version,
            Instant createdAt,
            Instant updatedAt,
            List<ScheduleResponse> schedules) {
    }

    public record ScheduleResponse(
            String id,
            String name,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            LocalTime startTime,
            Integer expectedMinutesOverride,
            int graceMinutes,
            LocalTime endTime,
            String scope,
            String scopeCategoryId) {
    }
}
