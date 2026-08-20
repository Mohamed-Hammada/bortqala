package com.bemo.hr.compliance.eta.api;

import com.bemo.hr.compliance.eta.domain.EtaDocumentType;
import com.bemo.hr.compliance.eta.domain.EtaEnvironment;
import com.bemo.hr.compliance.eta.domain.EtaSubmissionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class EtaComplianceApi {

    private EtaComplianceApi() {
    }

    public record SaveConfigRequest(
            @NotBlank String clientId,
            String clientSecret,
            @NotBlank String issuerTaxId,
            @NotBlank String issuerName,
            @NotNull EtaEnvironment environment,
            String tokenUrl,
            String apiBaseUrl,
            boolean active
    ) {
    }

    public record ConfigResponse(
            String id,
            String clientId,
            String maskedSecret,
            String issuerTaxId,
            String issuerName,
            EtaEnvironment environment,
            String tokenUrl,
            String apiBaseUrl,
            boolean active,
            long updatedAt
    ) {
    }

    public record QueueInvoiceRequest(
            @NotBlank String invoiceId,
            @NotNull EtaDocumentType documentType
    ) {
    }

    public record CancelDocumentRequest(
            @NotBlank String reason
    ) {
    }

    public record SubmissionResponse(
            String id,
            String invoiceId,
            String internalId,
            EtaDocumentType documentType,
            String etaUuid,
            String submissionUuid,
            EtaSubmissionStatus status,
            long dateTimeIssued,
            BigDecimal totalSalesAmount,
            BigDecimal totalDiscountAmount,
            BigDecimal netAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String canonicalJsonHash,
            String rawResponseJson,
            String validationErrorsJson,
            int submissionAttempts,
            String cancellationReason,
            long createdAt,
            long updatedAt,
            long version
    ) {
    }

    public record SubmissionSummaryResponse(
            long totalSubmitted,
            long validCount,
            long invalidCount,
            long pendingCount,
            BigDecimal totalTaxReported
    ) {
    }

    public record SaveItemMappingRequest(
            @NotBlank String itemId,
            @NotBlank String itemCode,
            @NotBlank String codeType,
            @NotBlank String itemCodeValue,
            String descriptionAr,
            String descriptionEn,
            boolean active
    ) {
    }

    public record ItemMappingResponse(
            String id,
            String itemId,
            String itemCode,
            String codeType,
            String itemCodeValue,
            String descriptionAr,
            String descriptionEn,
            boolean active,
            long createdAt
    ) {
    }
}
