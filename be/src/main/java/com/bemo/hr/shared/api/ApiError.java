package com.bemo.hr.shared.api;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String code,
        String message,
        String localizedMessage,
        int status,
        String path,
        String correlationId,
        Instant timestamp,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String code, String message) {
    }
}
