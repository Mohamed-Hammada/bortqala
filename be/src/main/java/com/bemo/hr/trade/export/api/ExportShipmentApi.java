package com.bemo.hr.trade.export.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class ExportShipmentApi {

    // ─── Shipment Lines ──────────────────────────────────────────────

    public record ShipmentLinePayload(
            int lineOrder,
            @NotBlank String itemName,
            String itemCode,
            String lotReference,
            @NotNull @DecimalMin("0.001") BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal netWeightKg,
            BigDecimal grossWeightKg,
            Integer packagesCount
    ) {
    }

    public record ShipmentLineResponse(
            String id,
            int lineOrder,
            String itemName,
            String itemCode,
            String lotReference,
            BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal netWeightKg,
            BigDecimal grossWeightKg,
            Integer packagesCount
    ) {
    }

    // ─── Shipment ────────────────────────────────────────────────────

    public record ShipmentPayload(
            @NotBlank String customerPartyId,
            String customerPartyName,
            String contractRef,
            String containerNo,
            String bookingNo,
            String acidNo,
            String portOfLoading,
            String portOfDischarge,
            Long etbDate,
            Long etaDate,
            String notes,
            BigDecimal expectedFxAmount,
            String expectedFxCurrency,
            @Size(min = 1, max = 100) List<ShipmentLinePayload> lines
    ) {
    }

    public record ShipmentResponse(
            String id,
            String shipmentNumber,
            String customerPartyId,
            String customerPartyName,
            String contractRef,
            String containerNo,
            String bookingNo,
            String acidNo,
            String portOfLoading,
            String portOfDischarge,
            Long etbDate,
            Long etaDate,
            String status,
            String notes,
            BigDecimal expectedFxAmount,
            String expectedFxCurrency,
            BigDecimal realizedFxAmount,
            int daysOutstanding,
            @Size(min = 0) List<ShipmentLineResponse> lines,
            long createdAt,
            long updatedAt
    ) {
    }

    // ─── Compliance / Treatment Log ──────────────────────────────────

    public record TreatmentLogPayload(
            @NotBlank String lotReference,
            @NotBlank String chemical,
            String dose,
            @NotNull Long treatmentDate,
            @NotNull @Min(0) int preHarvestIntervalDays,
            String treatedBy,
            String notes
    ) {
    }

    public record TreatmentLogResponse(
            String id,
            String lotReference,
            String chemical,
            String dose,
            Long treatmentDate,
            int preHarvestIntervalDays,
            String earliestSafePickup,
            boolean violation,
            long daysUntilSafe,
            String treatedBy,
            String notes,
            long createdAt
    ) {
    }

    public record ViolationResponse(
            String lotReference,
            String chemical,
            Long treatmentDate,
            String earliestSafePickup,
            int preHarvestIntervalDays,
            long daysShort
    ) {
    }

    public record ComplianceCheckResponse(
            List<ViolationResponse> violations,
            int totalLotsChecked,
            int compliantLots
    ) {
    }

    // ─── Pesticide Register ──────────────────────────────────────────

    public record PesticidePayload(
            @NotBlank String chemicalName,
            String activeIngredient,
            String registrationNumber,
            BigDecimal mrlMgPerKg,
            String maxDosePerHa,
            Integer preHarvestIntervalDays,
            String cropAuthorized,
            String notes
    ) {
    }

    public record PesticideResponse(
            String id,
            String chemicalName,
            String activeIngredient,
            String registrationNumber,
            BigDecimal mrlMgPerKg,
            String maxDosePerHa,
            Integer preHarvestIntervalDays,
            String cropAuthorized,
            String status,
            String notes,
            long createdAt,
            long updatedAt
    ) {
    }

    // ─── Proceeds ────────────────────────────────────────────────────

    public record ProceedsPayload(
            BigDecimal realizedFxAmount
    ) {
    }

    public record ProceedsResponse(
            String shipmentId,
            BigDecimal expectedFxAmount,
            String expectedFxCurrency,
            BigDecimal realizedFxAmount,
            int daysOutstanding
    ) {
    }

    // ─── Aging ───────────────────────────────────────────────────────

    public record AgingEntry(
            String customerPartyId,
            String customerPartyName,
            String shipmentNumber,
            int daysOutstanding,
            BigDecimal expectedFxAmount,
            String expectedFxCurrency
    ) {
    }

    public record AgingResponse(
            List<AgingEntry> entries,
            BigDecimal totalExpectedFx,
            String currencyCode
    ) {
    }
}
