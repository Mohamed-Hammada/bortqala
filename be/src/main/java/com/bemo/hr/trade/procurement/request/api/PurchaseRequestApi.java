package com.bemo.hr.trade.procurement.request.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class PurchaseRequestApi {

    public record PurchaseRequestLinePayload(
            @NotBlank String itemId,
            String itemName,
            @NotNull @DecimalMin("0.000001") BigDecimal quantity,
            String unitOfMeasure,
            @DecimalMin("0") BigDecimal estimatedUnitPrice
    ) {
    }

    public record PurchaseRequestPayload(
            @NotBlank String requestedBy,
            String departmentId,
            Long neededBy,
            @Size(max = 500) String notes,
            @NotNull @Valid List<PurchaseRequestLinePayload> lines
    ) {
    }

    public record PurchaseRequestLineResponse(
            String id, String itemId, String itemName, BigDecimal quantity,
            String unitOfMeasure, BigDecimal estimatedUnitPrice, BigDecimal convertedQuantity
    ) {
    }

    public record PurchaseRequestResponse(
            String id, String requestNumber, String requestedBy, String departmentId, String departmentName,
            String status, Long neededBy, String notes, String convertedPoId,
            BigDecimal estimatedTotal, List<PurchaseRequestLineResponse> lines,
            long createdAt, long updatedAt
    ) {
        public PurchaseRequestResponse(String id, String requestNumber, String requestedBy, String departmentId,
                                       String departmentName, String status, Long neededBy, String notes,
                                       String convertedPoId, long createdAt, long updatedAt) {
            this(id, requestNumber, requestedBy, departmentId, departmentName, status, neededBy, notes,
                    convertedPoId, null, null, createdAt, updatedAt);
        }
    }

    public record ConvertRequest(@NotBlank String supplierId) {
    }
}
