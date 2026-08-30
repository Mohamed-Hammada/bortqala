package com.bemo.hr.trade.procurement.api;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class OcrCaptureApi {

    public record OcrJobResponse(
            String id, String uploadedBy, String imageOriginalName, String imageContentType,
            String status, String extractedPayload, String confidenceSummary, String errorCode,
            long createdAt
    ) {
    }

    public record OcrExtractedLine(
            String name, String qty, String unitPrice, double confidence
    ) {
    }

    public record OcrExtractedData(
            String supplierName, String invoiceNo, String date,
            List<OcrExtractedLine> lines, List<SuggestedParty> suggestedParties
    ) {
    }

    public record SuggestedParty(String id, String name, double matchScore) {
    }

    public record ConvertOcrPayload(
            @NotBlank String partyId, String warehouseId
    ) {
    }

    public record OcrProviderStatus(String configured, String providerName) {
    }
}
