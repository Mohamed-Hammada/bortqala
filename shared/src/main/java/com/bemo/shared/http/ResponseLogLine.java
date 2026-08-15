package com.bemo.shared.http;

import java.util.Map;

import com.bemo.shared.logging.JsonLoggingUtil;

/**
 * Structured record of the response side of an access-log entry.
 */
public record ResponseLogLine(
        String status,
        String contentType,
        long durationInMillis,
        String correlationId,
        String body) {

    public String asJson() {
        Map<String, Object> log = JsonLoggingUtil.newLog();
        log.put("type", "response");
        JsonLoggingUtil.putIfPresent(log, "status", status);
        JsonLoggingUtil.putIfPresent(log, "contentType", contentType);
        JsonLoggingUtil.putIfPresent(log, "durationInMillis", durationInMillis);
        JsonLoggingUtil.putIfPresent(log, "correlationId", correlationId);
        JsonLoggingUtil.putIfPresent(log, "body", body);
        return JsonLoggingUtil.toJson(log);
    }
}
