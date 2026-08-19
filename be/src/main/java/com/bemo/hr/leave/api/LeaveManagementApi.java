package com.bemo.hr.leave.api;

import com.bemo.hr.leave.domain.LeaveRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class LeaveManagementApi {

    private LeaveManagementApi() {
    }

    public record CreateLeaveTypeRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 200) String nameAr,
            @NotBlank @Size(max = 200) String nameEn,
            boolean paid,
            boolean requiresAttachment,
            int maxConsecutiveDays
    ) {
    }

    public record LeaveTypeResponse(
            String id,
            String code,
            String nameAr,
            String nameEn,
            boolean paid,
            boolean requiresAttachment,
            int maxConsecutiveDays,
            long createdAt
    ) {
    }

    public record AdjustBalanceRequest(
            @NotBlank String employeeId,
            @NotBlank String leaveTypeId,
            int year,
            @NotNull BigDecimal entitledDays,
            BigDecimal carriedOverDays
    ) {
    }

    public record LeaveBalanceResponse(
            String id,
            String employeeId,
            String employeeName,
            String leaveTypeId,
            String leaveTypeCode,
            String leaveTypeName,
            int year,
            BigDecimal entitledDays,
            BigDecimal carriedOverDays,
            BigDecimal usedDays,
            BigDecimal pendingDays,
            BigDecimal remainingDays
    ) {
    }

    public record SubmitLeaveRequest(
            @NotBlank String employeeId,
            @NotBlank String leaveTypeId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            String reason
    ) {
    }

    public record RejectLeaveRequest(
            @NotBlank String rejectionReason
    ) {
    }

    public record LeaveRequestResponse(
            String id,
            String requestNumber,
            String employeeId,
            String employeeName,
            String leaveTypeId,
            String leaveTypeCode,
            String leaveTypeName,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalDays,
            LeaveRequestStatus status,
            String reason,
            String rejectionReason,
            String approverUserId,
            Long approvedAt,
            long createdAt,
            long updatedAt,
            long version
    ) {
    }
}
