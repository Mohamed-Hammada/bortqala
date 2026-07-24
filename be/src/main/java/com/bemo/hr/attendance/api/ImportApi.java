package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.domain.ImportStatus;

import java.time.Instant;
import java.util.List;

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
}
