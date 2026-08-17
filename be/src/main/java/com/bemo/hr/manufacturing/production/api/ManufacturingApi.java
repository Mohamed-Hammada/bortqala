package com.bemo.hr.manufacturing.production.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class ManufacturingApi {

    public record BomLineResponse(
            String id,
            String componentItemId,
            String componentItemName,
            BigDecimal quantityPer,
            String unitOfMeasure,
            BigDecimal wastePercent,
            int lineNumber
    ) {
    }

    public record BomLinePayload(
            @NotBlank String componentItemId,
            @NotBlank String componentItemName,
            @NotNull @DecimalMin(value = "0.0001") BigDecimal quantityPer,
            String unitOfMeasure,
            BigDecimal wastePercent,
            int lineNumber
    ) {
    }

    public record BomResponse(
            String id,
            String bomCode,
            String finishedItemId,
            String finishedGoodName,
            BigDecimal yieldQuantity,
            String revision,
            Long effectiveFrom,
            Long effectiveTo,
            String notes,
            boolean active,
            List<BomLineResponse> lines,
            long createdAt,
            long updatedAt
    ) {
    }

    public record BomPayload(
            @NotBlank String bomCode,
            String finishedItemId,
            @NotBlank String finishedGoodName,
            @NotNull @DecimalMin(value = "0.0001") BigDecimal yieldQuantity,
            String revision,
            Long effectiveFrom,
            Long effectiveTo,
            String notes,
            boolean active,
            List<@Valid BomLinePayload> lines
    ) {
    }

    public record ProductionOrderResponse(
            String id,
            String orderNumber,
            String bomId,
            String finishedItemId,
            String bomRevision,
            BigDecimal targetQuantity,
            BigDecimal actualOutputQuantity,
            BigDecimal scrapQuantity,
            BigDecimal actualMaterialCost,
            BigDecimal actualUnitCost,
            long startDate,
            Long completionDate,
            String status,
            String notes,
            long createdAt,
            long updatedAt,
            long version
    ) {
    }

    public record ProductionOrderPayload(
            @NotBlank String orderNumber,
            @NotBlank String bomId,
            String finishedItemId,
            String bomRevision,
            @NotNull @DecimalMin(value = "0.0001") BigDecimal targetQuantity,
            long startDate,
            String notes
    ) {
    }

    public record CompleteProductionOrderPayload(
            @NotNull @DecimalMin(value = "0.0001") BigDecimal actualOutputQuantity,
            BigDecimal scrapQuantity,
            long completionDate,
            String notes
    ) {
    }

    public record MaterialRequirementView(
            String componentItemId,
            String componentItemName,
            BigDecimal requiredQuantity,
            BigDecimal availableStock,
            BigDecimal shortageQuantity,
            boolean ready
    ) {
    }

    public record MaterialReadinessResponse(
            String orderId,
            String orderNumber,
            boolean allMaterialsAvailable,
            List<MaterialRequirementView> requirements
    ) {
    }

    public record QualityInspectionResponse(
            String id,
            String inspectionNumber,
            long inspectionDate,
            String sourceType,
            BigDecimal passedQuantity,
            BigDecimal failedQuantity,
            String status,
            String inspectorName,
            String notes,
            long createdAt
    ) {
    }

    public record QualityInspectionPayload(
            @NotBlank String inspectionNumber,
            long inspectionDate,
            @NotBlank String sourceType,
            @NotNull BigDecimal passedQuantity,
            @NotNull BigDecimal failedQuantity,
            @NotBlank String status,
            @NotBlank String inspectorName,
            String notes
    ) {
    }
}
