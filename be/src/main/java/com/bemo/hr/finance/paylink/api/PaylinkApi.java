package com.bemo.hr.finance.paylink.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public final class PaylinkApi {
    private PaylinkApi() {}

    public record CreateLinkPayload(
            @NotBlank String kind,
            String refId,
            @NotNull @Positive BigDecimal amount,
            String description,
            Long expiresAtEpochMs
    ) {}

    public record LinkResponse(
            String id, String kind, String refId, BigDecimal amount, String currency,
            String token, String status, String gatewayRef, String description,
            String companyName, Long expiresAtEpochMs, Long paidAtEpochMs,
            Long createdAtEpochMs, Long version
    ) {}

    public record ListLinksResponse(List<LinkResponse> links) {}

    public record WebhookPayload(String providerTxnId, String signature) {}

    public record PublicPagePayload(
            String companyName, String description, BigDecimal amount, String currency, boolean expired, boolean paid
    ) {}
}
