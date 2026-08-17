package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.domain.AttendanceExceptionResolution;
import com.bemo.hr.reporting.domain.AttendanceExceptionStatus;
import com.bemo.hr.reporting.domain.AttendanceExceptionType;
import com.bemo.hr.reporting.domain.AttendancePolicyScope;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public final class AttendanceExceptionApi {
    private AttendanceExceptionApi() {
    }

    public record PolicyRequest(@NotBlank String name, @NotNull AttendancePolicyScope scopeType, String scopeId,
                                @NotNull LocalDate effectiveFrom, LocalDate effectiveTo, int priority,
                                @Min(0) int lateThresholdMinutes, @Min(0) int earlyThresholdMinutes,
                                @Min(1) int maxShiftMinutes,
                                @Min(0) @Max(100) int missingPunchScore, @Min(0) @Max(100) int singlePunchScore,
                                @Min(0) @Max(100) int lateScore, @Min(0) @Max(100) int earlyScore,
                                @Min(0) @Max(100) int payrollBlockScore, boolean active) {
    }

    public record PolicyResponse(String id, String name, AttendancePolicyScope scopeType, String scopeId,
                                 LocalDate effectiveFrom,
                                 LocalDate effectiveTo, int priority, int lateThresholdMinutes,
                                 int earlyThresholdMinutes, int maxShiftMinutes,
                                 int missingPunchScore, int singlePunchScore, int lateScore, int earlyScore,
                                 int payrollBlockScore, boolean active, long version) {
    }

    public record ExceptionView(String id, String reportId, String dailyResultId, String employeeId,
                                String employeeName,
                                String categoryId, String categoryName, LocalDate workDate,
                                AttendanceExceptionType exceptionType, int score,
                                int metricMinutes, String explanationKey, String policyId, String policyName,
                                long policyVersion, String policySnapshotJson, AttendancePolicyScope policyScope,
                                boolean payrollBlocking, AttendanceExceptionStatus status,
                                AttendanceExceptionResolution resolution, String reason, long version) {
    }

    public record Summary(int total, int open, int critical, int resolved, int affectedEmployees) {
    }

    public record WorkbenchResponse(Summary summary, List<ExceptionView> exceptions) {
    }

    public record BulkRequest(@NotEmpty List<@NotBlank String> exceptionIds,
                              @NotNull AttendanceExceptionResolution resolution,
                              @NotBlank @Size(max = 500) String reason, @NotBlank @Size(max = 80) String operationId) {
    }

    public record BulkPreview(int selected, int editable, int alreadyClosed, int payrollBlockersCleared,
                              List<String> excludedIds) {
    }

    public record BulkResult(WorkbenchResponse workbench, int applied, int replayed, int skipped) {
    }
}
