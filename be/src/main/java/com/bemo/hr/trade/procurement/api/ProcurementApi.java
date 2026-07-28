package com.bemo.hr.trade.procurement.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class ProcurementApi {

    public record PurchaseOrderLineResponse(
            String id,
            String itemName,
            String itemCategory,
            BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}

    public record PurchaseOrderLinePayload(
            @NotBlank String itemName,
            String itemCategory,
            @NotNull BigDecimal quantity,
            String unitOfMeasure,
            @NotNull BigDecimal unitPrice
    ) {}

    public record PurchaseOrderResponse(
            String id,
            String poNumber,
            long poDate,
            String supplierId,
            String supplierName,
            String purchaseRequestId,
            String paymentTerms,
            String status,
            BigDecimal totalAmount,
            List<PurchaseOrderLineResponse> items,
            long createdAt,
            long updatedAt
    ) {}

    public record PurchaseOrderPayload(
            @NotBlank String poNumber,
            long poDate,
            @NotBlank String supplierId,
            String purchaseRequestId,
            String paymentTerms,
            @NotNull BigDecimal totalAmount,
            List<PurchaseOrderLinePayload> items
    ) {}
}
