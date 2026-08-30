package com.bemo.hr.whatsapp.api;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class WhatsAppApi {
    private WhatsAppApi() {}

    public record SendTestPayload(@NotBlank String phoneNumber) {}

    public record OutboundLogEntry(
            String id, String recipientType, String recipientId, String phoneNumber,
            String templateKey, String status, String providerMessageId,
            String errorMessage, int retryCount, Long sentAtEpochMs, Long createdAtEpochMs
    ) {}

    public record LogResponse(List<OutboundLogEntry> entries, int total) {}

    public record WhatsAppSettings(
            boolean configured, String provider, List<TemplateMapping> templates
    ) {}

    public record TemplateMapping(String key, String templateName) {}

    public record ResendPayload(@NotBlank String logId) {}
}
