package com.bemo.hr.party.api;

import java.math.BigDecimal;
import java.util.List;

public final class PartyFinancialPositionApi {

    private PartyFinancialPositionApi() {
    }

    public record AgingBreakdown(
            BigDecimal currentNotDue,
            BigDecimal bucket1To30,
            BigDecimal bucket31To60,
            BigDecimal bucket61To90,
            BigDecimal bucket90Plus,
            BigDecimal totalOverdue
    ) {}

    public record PartyFinancialPositionSummary(
            String partyId,
            String partyCode,
            String partyName,
            String partyType,
            String currencyCode,
            BigDecimal openingBalance,
            BigDecimal totalDebits,
            BigDecimal totalCredits,
            BigDecimal netClosingBalance,
            BigDecimal overdueAmount,
            BigDecimal unappliedCashAmount,
            BigDecimal retentionHeldAmount,
            BigDecimal creditLimit,
            boolean creditHold,
            String creditStatus,
            AgingBreakdown aging
    ) {}

    public record PartyStatementLine(
            long transactionDate,
            String documentType,
            String documentNumber,
            String reference,
            String description,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal runningBalance,
            Long dueDate,
            long overdueDays,
            String sourceEntityId
    ) {}

    public record PartyStatementResponse(
            PartyFinancialPositionSummary summary,
            Long fromDate,
            Long toDate,
            List<PartyStatementLine> lines
    ) {}

    public record AgingReportRow(
            String partyId,
            String partyCode,
            String partyName,
            String partyType,
            BigDecimal totalBalance,
            BigDecimal currentNotDue,
            BigDecimal bucket1To30,
            BigDecimal bucket31To60,
            BigDecimal bucket61To90,
            BigDecimal bucket90Plus
    ) {}

    public record AgingReportResponse(
            long asOfDate,
            String partyTypeFilter,
            BigDecimal totalOutstanding,
            List<AgingReportRow> rows
    ) {}

    public record ArApGlReconciliationResponse(
            String partyType,
            BigDecimal subledgerTotal,
            BigDecimal glControlAccountBalance,
            BigDecimal variance,
            boolean isReconciled
    ) {}
}
