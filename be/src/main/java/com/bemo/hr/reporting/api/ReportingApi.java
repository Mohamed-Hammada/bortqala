package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.domain.AttendanceDecision;
import com.bemo.hr.reporting.domain.DayAnomalyDecision;
import com.bemo.hr.reporting.domain.DayAnomalyStatus;
import com.bemo.hr.reporting.domain.DailyStatus;
import com.bemo.hr.reporting.domain.HolidayProposalStatus;
import com.bemo.hr.reporting.domain.ReportStatus;
import com.bemo.hr.employee.domain.PayCycle;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;

public final class ReportingApi {
    private ReportingApi() { }

    public record CreateRequest(@NotNull LocalDate periodStart, @NotNull LocalDate periodEnd,
                                @NotNull PayCycle payCycle) { }
    public record DecisionRequest(@NotNull AttendanceDecision decision, @Min(0) @Max(1_440) Integer workedMinutes,
                                  @Size(max = 500) String note) { }
    public record HolidayDecisionRequest(@NotNull HolidayProposalStatus status, @Size(max = 150) String holidayName,
                                         @Size(max = 500) String note) { }
    public record BulkDecisionRequest(@NotNull AttendanceDecision decision, @NotNull String statusFilter,
                                      @Size(max = 500) String note, @NotNull String operationId) { }
    public record BulkDecisionResponse(int matchingCount, int editableCount, int excludedCount, int successCount,
                                       List<String> excludedRecordIds) { }
    public record DowntimeDecisionRequest(@NotNull String date, @Size(max = 100) String categoryId,
                                          @Size(max = 100) String location,
                                          @NotNull String decision, @Size(max = 500) String note) { }
    public record DayAnomalyDecisionRequest(@NotNull DayAnomalyDecision decision,
                                            @NotBlank @Size(max = 500) String reason,
                                            @NotBlank @Size(max = 80) String operationId) { }
    public record DayAnomalyView(String id, String reportId, LocalDate workDate, String categoryId,
                                 String categoryName, String location, int affectedCount,
                                 int totalEmployeeCount, BigDecimal absencePercentage,
                                 int thresholdPercentage, int affectedExpectedMinutes,
                                 DayAnomalyStatus status, DayAnomalyDecision decision, String reason,
                                 String decidedBy, Instant decidedAt, String reversedBy,
                                 Instant reversedAt, String reopenedBy, Instant reopenedAt,
                                 Instant createdAt) { }
    public record DayAnomalyActionResponse(Details details, int affectedCount, int appliedCount, int skippedCount) { }
    public enum PeriodKind { MONTHLY, FIRST_HALF, SECOND_HALF }
    public record PeriodOption(int year, int month, PeriodKind kind, LocalDate start, LocalDate end) { }
    public record Summary(String id, LocalDate periodStart, LocalDate periodEnd, PayCycle payCycle, ReportStatus status,
                          int unresolvedCount, String createdBy, Instant createdAt, String approvedBy,
                          Instant approvedAt, Instant exportedAt, long version) { }
    public record Details(Summary report, List<CategorySummary> categories, List<DailyResult> dailyResults,
                          List<HolidayProposalView> holidayProposals, List<DayAnomalyView> dayAnomalies) { }
    public record CategorySummary(String categoryId, String categoryName, long employeeDays, long presentDays,
                                  long exceptionDays, LocalTime typicalArrival, long overtimeMinutes) { }
    public record DailyResult(String id, String employeeId, String employeeCode, String employeeName,
                              String categoryId, String categoryName, LocalDate workDate, Instant firstPunch,
                              Instant lastPunch, int punchCount, int expectedMinutes, int workedMinutes,
                              Integer manualWorkedMinutes, int effectiveWorkedMinutes, int lateMinutes,
                              int earlyLeaveMinutes, int overtimeMinutes, DailyStatus status, String warning,
                              AttendanceDecision decision, String decisionNote, String decidedBy, Instant decidedAt,
                              String ruleVersion) { }
    public record HolidayProposalView(String id, String categoryId, String categoryName, LocalDate workDate,
                                      int activeEmployeeCount, HolidayProposalStatus status, String note,
                                      String decidedBy, Instant decidedAt) { }
}
