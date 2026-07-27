package com.bemo.hr.trade.procurement.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class ProcurementApi {

    public record PurchaseOrderResponse(
            String id,
            String poNumber,
            long poDate,
            String supplierId,
            String purchaseRequestId,
            String paymentTerms,
            String status,
            BigDecimal totalAmount,
            long createdAt,
            long updatedAt
    ) {}

    public record PurchaseOrderPayload(
            @NotBlank String poNumber,
            long poDate,
            @NotBlank String supplierId,
            String purchaseRequestId,
            String paymentTerms,
            @NotNull BigDecimal totalAmount
    ) {}
}
