package com.bemo.hr.finance.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class BankReconciliationApi {
    private BankReconciliationApi() {
    }

    public record StatementResponse(String id, String bankAccountId, String statementReference, long periodStart,
                                    long periodEnd, BigDecimal openingBalance, BigDecimal closingBalance,
                                    String currencyCode,
                                    String fileName, String status, String importedBy, long importedAt,
                                    String reconciledBy,
                                    Long reconciledAt, long version, long lineCount, long unmatchedCount) {
    }

    public record MatchResponse(String id, String journalEntryId, BigDecimal matchedAmount, String matchType,
                                String status, String matchedBy, long matchedAt, String reversedBy, Long reversedAt,
                                String reversalReason) {
    }

    public record CandidateResponse(String journalEntryId, String entryNumber, long entryDate, String description,
                                    String reference, BigDecimal bankAmount, BigDecimal availableAmount, int score,
                                    String reason) {
    }

    public record LineResponse(String id, int lineNumber, long transactionDate, Long valueDate, String description,
                               String bankReference, BigDecimal amount, BigDecimal runningBalance, String status,
                               BigDecimal matchedAmount, BigDecimal remainingAmount, long version,
                               List<MatchResponse> matches,
                               List<CandidateResponse> suggestions) {
    }

    public record WorkbenchResponse(StatementResponse statement, List<LineResponse> lines) {
    }

    public record Allocation(@NotBlank String journalEntryId, @NotNull @DecimalMin("0.01") BigDecimal amount) {
    }

    public record MatchRequest(@NotBlank @Size(max = 80) String operationId, @Valid List<Allocation> allocations,
                               @DecimalMin("0.00") BigDecimal feeAmount, String feeExpenseAccountId) {
    }

    public record OperationRequest(@NotBlank @Size(max = 80) String operationId) {
    }

    public record ReverseRequest(@NotBlank @Size(max = 80) String operationId,
                                 @NotBlank @Size(max = 500) String reason) {
    }

    public record CashPositionLine(String bankAccountId, String bankName, String currencyCode,
                                   BigDecimal latestStatementBalance, Long asOfDate, long unmatchedLines) {
    }

    public record CashPositionResponse(List<CashPositionLine> accounts,
                                       java.util.Map<String, BigDecimal> totalsByCurrency) {
    }
}
