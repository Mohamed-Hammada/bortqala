package com.bemo.hr.manufacturing.production.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class ManufacturingApi {

    public record BomResponse(
            String id,
            String bomCode,
            String finishedGoodName,
            BigDecimal yieldQuantity,
            String notes,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record BomPayload(
            @NotBlank String bomCode,
            @NotBlank String finishedGoodName,
            @NotNull BigDecimal yieldQuantity,
            String notes,
            boolean active
    ) {}

    public record ProductionOrderResponse(
            String id,
            String orderNumber,
            String bomId,
            BigDecimal targetQuantity,
            long startDate,
            String status,
            long createdAt,
            long updatedAt
    ) {}

    public record ProductionOrderPayload(
            @NotBlank String orderNumber,
            @NotBlank String bomId,
            @NotNull BigDecimal targetQuantity,
            long startDate
    ) {}

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
    ) {}

    public record QualityInspectionPayload(
            @NotBlank String inspectionNumber,
            long inspectionDate,
            @NotBlank String sourceType,
            @NotNull BigDecimal passedQuantity,
            @NotNull BigDecimal failedQuantity,
            @NotBlank String status,
            @NotBlank String inspectorName,
            String notes
    ) {}
}
