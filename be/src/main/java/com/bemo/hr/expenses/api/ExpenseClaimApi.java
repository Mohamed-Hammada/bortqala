package com.bemo.hr.expenses.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseClaimApi {

    public record CreateClaimRequest(
            @NotBlank String category,
            @NotNull LocalDate spentOn,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String currency,
            @Size(max = 1000) String description,
            @Size(max = 255) String attachmentName,
            @Size(max = 100) String attachmentContentType,
            Long attachmentSize
    ) {}

    public record UpdateClaimRequest(
            @NotBlank String category,
            @NotNull LocalDate spentOn,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String currency,
            @Size(max = 1000) String description,
            @Size(max = 255) String attachmentName,
            @Size(max = 100) String attachmentContentType,
            Long attachmentSize
    ) {}

    public record DecisionRequest(
            @Size(max = 500) String note
    ) {}

    public record ReimburseRequest(
            @NotBlank String reference
    ) {}

    public record ClaimResponse(
            String id, String employeeId, String employeeName,
            String category, String spentOn, BigDecimal amount, String currency,
            String description, String receiptName, String receiptContentType, Long receiptSize,
            String status, String approverId, Long decidedAt, String decisionNote,
            String reimbursementReference, long createdAt, long updatedAt
    ) {}
}
