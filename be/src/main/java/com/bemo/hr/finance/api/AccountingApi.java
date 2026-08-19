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
    ) {
    }

    public record AccountPayload(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String type,
            String parentId,
            boolean isHeader,
            String currency,
            boolean active
    ) {
    }

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
            String departmentId,
            String wbsNodeId,
            String costCodeId
    ) {
        public JournalEntryLineResponse(String id, String journalEntryId, String accountId, String partyId,
                                        BigDecimal debit, BigDecimal credit, String memo, String costCenterId,
                                        String projectId, String departmentId) {
            this(id, journalEntryId, accountId, partyId, debit, credit, memo, costCenterId, projectId, departmentId, null, null);
        }
    }

    public record JournalEntryLinePayload(
            @NotBlank String accountId,
            String partyId,
            BigDecimal debit,
            BigDecimal credit,
            String memo,
            String costCenterId,
            String projectId,
            String departmentId,
            String wbsNodeId,
            String costCodeId
    ) {
        public JournalEntryLinePayload(String accountId, String partyId, BigDecimal debit, BigDecimal credit,
                                       String memo, String costCenterId, String projectId, String departmentId) {
            this(accountId, partyId, debit, credit, memo, costCenterId, projectId, departmentId, null, null);
        }
    }

    public record JournalEntryPayload(
            String entryNumber,
            long entryDate,
            @NotBlank String description,
            String reference,
            String fiscalPeriodId,
            String currency,
            String projectId,
            String wbsNodeId,
            String costCodeId,
            @NotNull @Size(min = 2) List<JournalEntryLinePayload> lines
    ) {
        public JournalEntryPayload(String entryNumber, long entryDate, String description, String reference,
                                   String fiscalPeriodId, String currency, List<JournalEntryLinePayload> lines) {
            this(entryNumber, entryDate, description, reference, fiscalPeriodId, currency, null, null, null, lines);
        }
    }

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
            String projectId,
            String wbsNodeId,
            String costCodeId,
            long version,
            List<JournalEntryLineResponse> lines,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            long createdAt,
            long updatedAt
    ) {
        public JournalEntryResponse(String id, String entryNumber, long entryDate, String description, String reference,
                                    String status, String fiscalPeriodId, String currency, String postedBy, Long postedAt,
                                    String reversalEntryId, String reversedEntryId, String reversalReason, String reversedBy,
                                    Long reversedAt, String operationId, long version, List<JournalEntryLineResponse> lines,
                                    BigDecimal totalDebit, BigDecimal totalCredit, long createdAt, long updatedAt) {
            this(id, entryNumber, entryDate, description, reference, status, fiscalPeriodId, currency, postedBy, postedAt,
                    reversalEntryId, reversedEntryId, reversalReason, reversedBy, reversedAt, operationId, null, null, null,
                    version, lines, totalDebit, totalCredit, createdAt, updatedAt);
        }
    }

    public record JournalActionRequest(
            @NotBlank String operationId,
            Long expectedVersion,
            String reason
    ) {
    }

    public record JournalEntryPageResponse(
            List<JournalEntryResponse> content,
            int page,
            int pageSize,
            long totalElements,
            int totalPages
    ) {
    }

    public record NumberingSettings(boolean automaticNumbering) {
    }
}
