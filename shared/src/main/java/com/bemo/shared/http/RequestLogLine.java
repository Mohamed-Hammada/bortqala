package com.bemo.shared.http;

import java.util.Map;
import java.util.TreeMap;

import com.bemo.shared.logging.JsonLoggingUtil;
import com.bemo.shared.logging.Maskers;

/**
 * Structured record of an incoming HTTP request for the access-log. Header values are masked
 * before they are serialized so authorization tokens never reach log files.
 */
public record RequestLogLine(
        String method,
        String uri,
        String queryString,
        String remoteAddress,
        String userAgent,
        String correlationId,
        String tenantId,
        String userId,
        Map<String, String> headers,
        String body) {

    public String asJson() {
        Map<String, Object> log = JsonLoggingUtil.newLog();
        log.put("type", "request");
        JsonLoggingUtil.putIfPresent(log, "method", method);
        JsonLoggingUtil.putIfPresent(log, "uri", uri);
        JsonLoggingUtil.putIfPresent(log, "query", queryString);
        JsonLoggingUtil.putIfPresent(log, "remoteAddress", remoteAddress);
        JsonLoggingUtil.putIfPresent(log, "userAgent", userAgent);
        JsonLoggingUtil.putIfPresent(log, "correlationId", correlationId);
        JsonLoggingUtil.putIfPresent(log, "tenantId", tenantId);
        JsonLoggingUtil.putIfPresent(log, "userId", userId);
        JsonLoggingUtil.putIfPresent(log, "body", body);
        if (headers != null && !headers.isEmpty()) {
            Map<String, String> maskedHeaders = new TreeMap<>();
            headers.forEach((name, value) -> maskedHeaders.put(name, Maskers.mask(value)));
            log.put("headers", maskedHeaders);
        }
        return JsonLoggingUtil.toJson(log);
    }
}
