package com.bemo.hr.platform.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class PlatformApi {
    private PlatformApi() {}

    public record ApiKeyCreateRequest(
            @NotBlank String name,
            String scopes,
            @Positive int rateLimitPerMin
    ) {}

    public record ApiKeyCreateResponse(
            String id, String name, String fullKey, String scopes,
            int rateLimitPerMin, boolean active, long createdAtEpochMs
    ) {}

    public record ApiKeyResponse(
            String id, String name, String scopes,
            int rateLimitPerMin, boolean active,
            Long lastUsedAtEpochMs, String createdBy,
            long createdAtEpochMs, long updatedAtEpochMs, Long version
    ) {}

    public record ApiKeyListResponse(List<ApiKeyResponse> keys) {}

    public record ApiKeyToggleRequest(boolean active) {}

    public record ApiKeyRevokeRequest() {}

    public record WebhookEndpointCreateRequest(
            @NotBlank String url,
            String events
    ) {}

    public record WebhookEndpointResponse(
            String id, String url, String events,
            boolean active, long createdAtEpochMs, long updatedAtEpochMs, Long version
    ) {}

    public record WebhookEndpointListResponse(List<WebhookEndpointResponse> endpoints) {}

    public record WebhookEndpointToggleRequest(boolean active) {}

    public record WebhookDeliveryResponse(
            Long id, String endpointId, String event, String payload,
            String status, int attempts, String lastError,
            Integer responseStatus, long createdAtEpochMs
    ) {}

    public record WebhookDeliveryListResponse(List<WebhookDeliveryResponse> deliveries) {}

    public record WebhookRedriveRequest() {}

    public record SearchResponse(List<SearchResultItem> results) {}

    public record SearchResultItem(
            String type, String id, String title, String subtitle, String url
    ) {}

    public record BulkUpdateRequest(
            String entityType,
            String field,
            String value,
            List<String> ids
    ) {}

    public record BulkUpdateResultItem(String id, boolean success, String error) {}

    public record BulkUpdateResponse(List<BulkUpdateResultItem> results) {}
}
