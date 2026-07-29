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
            String id, String fileName, String deviceName, ImportStatus status,
            int totalRows, int importedRows, int errorRows, String importedBy,
            Instant importedAt, boolean duplicate, List<RowErrorResponse> errors) { }

    public record RowErrorResponse(int rowNumber, String message, String rawLine) { }

    public record UnmatchedIdentityResponse(
            String deviceUserId, String observedName, long punchCount, Instant firstPunch, Instant lastPunch) { }

    public record DeviceRequest(
            @NotBlank String name,
            @NotBlank String endpointUrl,
            boolean enabled,
            @Min(1) @Max(1440) int syncIntervalMinutes
    ) { }

    public record DeviceResponse(
            String id, String name, String endpointUrl, boolean enabled, int syncIntervalMinutes,
            Instant lastSyncAt, Instant lastSuccessfulPunchAt, Instant nextSyncAt,
            String lastStatus, String lastMessage, Instant createdAt
    ) { }

    public record DeviceSyncResponse(
            DeviceResponse device, int receivedRows, int importedRows, int duplicateRows, boolean duplicateBatch
    ) { }
}
