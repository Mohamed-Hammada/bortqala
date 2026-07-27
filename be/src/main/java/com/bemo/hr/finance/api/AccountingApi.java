package com.bemo.hr.finance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class AccountingApi {

    public record AccountResponse(
            String id,
            String code,
            String name,
            String type,
            String parentId,
            boolean isHeader,
            String currency,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record AccountPayload(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String type,
            String parentId,
            boolean isHeader,
            String currency,
            boolean active
    ) {}

    public record JournalEntryLineResponse(
            String id,
            String journalEntryId,
            String accountId,
            String partyId,
            BigDecimal debit,
            BigDecimal credit,
            String memo
    ) {}

    public record JournalEntryLinePayload(
            @NotBlank String accountId,
            String partyId,
            BigDecimal debit,
            BigDecimal credit,
            String memo
    ) {}

    public record JournalEntryResponse(
            String id,
            String entryNumber,
            long entryDate,
            String description,
            String reference,
            String status,
            String fiscalPeriodId,
            String postedBy,
            Long postedAt,
            List<JournalEntryLineResponse> lines,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            long createdAt,
            long updatedAt
    ) {}

    public record JournalEntryPayload(
            @NotBlank String entryNumber,
            long entryDate,
            @NotBlank String description,
            String reference,
            String fiscalPeriodId,
            @NotNull List<JournalEntryLinePayload> lines
    ) {}

    public record JournalEntryPageResponse(
            List<JournalEntryResponse> content,
            int page,
            int pageSize,
            long totalElements,
            int totalPages
    ) {}
}
