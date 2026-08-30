package com.bemo.hr.attendance.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class SelfiePunchApi {

    private SelfiePunchApi() {
    }

    public record SelfiePunchRequest(
            @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String operationId,
            Long clientTimestamp,
            String imageContentType,
            Integer imageBytes,
            @NotNull String imageBase64
    ) {
    }

    public record SelfiePunchResponse(
            String id, String employeeId, String operationId, long punchedAt, boolean duplicate
    ) {
    }
}
