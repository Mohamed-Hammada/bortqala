package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.domain.ImportStatus;

import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class ImportApi {
    private ImportApi() {
    }

    public record BatchResponse(
            String id, String fileName, String sourceId, String deviceName, ImportStatus status,
            int totalRows, int importedRows, int validRows, int newPunches, int duplicatePunches,
            int errorRows, String importedBy,
            Instant importedAt, boolean duplicate, List<RowErrorResponse> errors) { }

    public record RowErrorResponse(int rowNumber, String message, String rawLine) { }

    public record PreviewRowResponse(int rowNumber, String deviceUserId, String employeeName, long punchedAt, String rawLine) { }

    public record PreviewResponse(
            String fileName, String checksum, int totalRows, int importedRows, int errorRows,
            List<PreviewRowResponse> rows, List<RowErrorResponse> errors) { }

    public record UnmatchedIdentityResponse(
            String deviceUserId, String observedName, long punchCount, Instant firstPunch, Instant lastPunch) { }

    public record SourceRequest(
            @NotBlank String name,
            String sourceType,
            Boolean active,
            Boolean autoCreateEmployees,
            String autoCreateCategoryId,
            String autoCreateEmploymentType,
            String autoCreateActiveFromMode,
            Boolean autoCreateEmployeeActive
    ) {
        public SourceRequest(String name, String sourceType, Boolean active) {
            this(name, sourceType, active, false, null, "FIXED", "FIRST_PUNCH", true);
        }
    }

    public record SourceResponse(
            String id, String name, String sourceType, String normalizedCode, boolean active,
            boolean autoCreateEmployees, String autoCreateCategoryId, String autoCreateEmploymentType,
            String autoCreateActiveFromMode, boolean autoCreateEmployeeActive, Instant createdAt
    ) {
        public SourceResponse(String id, String name, String sourceType, String normalizedCode,
                              boolean active, Instant createdAt) {
            this(id, name, sourceType, normalizedCode, active, false, null, "FIXED",
                    "FIRST_PUNCH", true, createdAt);
        }
    }

    public record DeviceRequest(
            @NotBlank String name,
            @NotBlank String endpointUrl,
            boolean enabled,
            @Min(1) @Max(1440) int syncIntervalMinutes,
            String username,
            String password
    ) { }

    public record DeviceResponse(
            String id, String name, String endpointUrl, boolean enabled, int syncIntervalMinutes,
            Instant lastSyncAt, Instant lastSuccessfulPunchAt, Instant nextSyncAt,
            String lastStatus, String lastMessage, String username, boolean hasPassword, Instant createdAt
    ) { }

    public record DeviceSyncResponse(
            DeviceResponse device, int receivedRows, int importedRows, int duplicateRows, boolean duplicateBatch
    ) { }
}
