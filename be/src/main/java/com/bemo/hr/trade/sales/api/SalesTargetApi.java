package com.bemo.hr.trade.sales.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class SalesTargetApi {

    public record TargetRequest(
            @NotBlank String scope,
            @NotBlank String targetRefId,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period,
            @NotBlank String metric,
            @NotNull @Positive BigDecimal targetValue) {}

    public record TargetResponse(String id, String scope, String targetRefId,
                                 String period, String metric, BigDecimal targetValue,
                                 BigDecimal achievedValue, Long version) {}

    public record CommissionRuleRequest(
            @NotBlank String name,
            @NotBlank String basis,
            @NotNull @DecimalMin("0.01") @DecimalMax("100") BigDecimal percent,
            @NotNull @DecimalMin("0") BigDecimal minAmount,
            boolean active,
            Long validFrom,
            Long validTo) {}

    public record CommissionRuleResponse(String id, String name, String basis,
                                         BigDecimal percent, BigDecimal minAmount,
                                         boolean active, Long validFrom, Long validTo,
                                         Long version) {}

    public record CommissionStatementEntry(
            String ruleId, String ruleName, BigDecimal basisAmount,
            BigDecimal percent, BigDecimal commissionAmount) {}

    public record CommissionStatementResponse(
            String repId, String period,
            java.util.List<CommissionStatementEntry> entries,
            BigDecimal totalCommission, boolean payrollSent, Long payrollSentAt) {}

    public record SendToPayrollRequest(
            @NotBlank String repId,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period) {}

    public record PayrollSendResponse(
            String repId, String period,
            BigDecimal totalCommission, boolean alreadySent, Long sentAt) {}
}
