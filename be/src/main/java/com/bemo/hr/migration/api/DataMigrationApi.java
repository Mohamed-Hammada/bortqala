package com.bemo.hr.migration.api;

import com.bemo.hr.migration.domain.MigrationEntityType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DataMigrationApi {

    private DataMigrationApi() {}

    public record BatchResponse(
            String id,
            MigrationEntityType entityType,
            String status,
            String fileName,
            int totalRecords,
            int importedRecords,
            int rejectedRecords,
            int duplicateRecords,
            BigDecimal totalAmount,
            String glAccountCode,
            boolean glBalanceMatch,
            String createdBy,
            Instant startedAt,
            Instant completedAt,
            long version
    ) {}

    public record RecordResponse(
            String id,
            String batchId,
            int rowNumber,
            String rawJsonData,
            String status,
            String errorMessage
    ) {}

    public record ValidationResultResponse(
            String batchId,
            int totalRows,
            int validRows,
            int invalidRows,
            int duplicateRows,
            List<RecordResponse> sampleErrors
    ) {}

    public record DryRunResponse(
            String batchId,
            MigrationEntityType entityType,
            int recordCount,
            BigDecimal totalCalculatedAmount,
            String glAccountCode,
            BigDecimal glAccountBalance,
            boolean balanced,
            String message
    ) {}

    public record CommitResponse(
            String batchId,
            String status,
            int importedCount,
            int rejectedCount,
            BigDecimal committedAmount,
            Instant completedAt
    ) {}

    public record RollbackResponse(
            String batchId,
            String status,
            int rolledBackCount,
            String message
    ) {}

    public record UploadRequest(
            MigrationEntityType entityType,
            String fileName,
            List<Map<String, Object>> rows
    ) {}
}
