package com.bemo.hr.finance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
            String memo,
            String costCenterId,
            String projectId,
            String departmentId
    ) {}

    public record JournalEntryLinePayload(
            @NotBlank String accountId,
            String partyId,
            BigDecimal debit,
            BigDecimal credit,
            String memo,
            String costCenterId,
            String projectId,
            String departmentId
    ) {}

    public record JournalEntryPayload(
            String entryNumber,
            long entryDate,
            @NotBlank String description,
            String reference,
            String fiscalPeriodId,
            String currency,
            @NotNull @Size(min = 2) List<JournalEntryLinePayload> lines
    ) {}

    public record JournalEntryResponse(
            String id,
            String entryNumber,
            long entryDate,
            String description,
            String reference,
            String status,
            String fiscalPeriodId,
            String currency,
            String postedBy,
            Long postedAt,
            String reversalEntryId,
            String reversedEntryId,
            String reversalReason,
            String reversedBy,
            Long reversedAt,
            String operationId,
            long version,
            List<JournalEntryLineResponse> lines,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            long createdAt,
            long updatedAt
    ) {}

    public record JournalActionRequest(
            @NotBlank String operationId,
            Long expectedVersion,
            String reason
    ) {}

    public record JournalEntryPageResponse(
            List<JournalEntryResponse> content,
            int page,
            int pageSize,
            long totalElements,
            int totalPages
    ) {}

    public record NumberingSettings(boolean automaticNumbering) {}
}
