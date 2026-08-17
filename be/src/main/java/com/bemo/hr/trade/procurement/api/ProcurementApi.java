package com.bemo.hr.trade.procurement.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class ProcurementApi {

    public record PurchaseOrderLineResponse(
            String id, String itemId, String itemName, String itemCategory,
            BigDecimal quantity, BigDecimal receivedQuantity, BigDecimal remainingQuantity,
            String unitOfMeasure, BigDecimal unitPrice, BigDecimal lineTotal
    ) {
    }

    public record PurchaseOrderLinePayload(
            @NotBlank String itemId, @NotBlank String itemName, String itemCategory,
            @NotNull BigDecimal quantity, String unitOfMeasure, @NotNull BigDecimal unitPrice
    ) {
    }

    public record PurchaseOrderResponse(
            String id, String poNumber, long poDate, String supplierId, String supplierName,
            String purchaseRequestId, String departmentId, String paymentTerms, String currencyCode,
            String baseCurrencyCode, BigDecimal exchangeRate, long exchangeRateDate,
            String exchangeRateSource, String exchangeRateOverrideReason, BigDecimal baseTotalAmount,
            String status, BigDecimal totalAmount,
            List<PurchaseOrderLineResponse> items, long createdAt, long updatedAt
    ) {
    }

    public record PurchaseOrderPayload(
            String poNumber, long poDate, @NotBlank String supplierId,
            String purchaseRequestId, String departmentId, String paymentTerms, String currencyCode,
            BigDecimal exchangeRate, String exchangeRateOverrideReason,
            List<PurchaseOrderLinePayload> items
    ) {
    }

    public record ExchangeRateQuote(
            String currencyCode, String baseCurrencyCode, BigDecimal exchangeRate,
            long rateDate, String source
    ) {
    }

    public record ThreeWayMatchResponse(
            String id, String purchaseOrderId, String goodsReceiptId, String supplierInvoiceId,
            String matchStatus, BigDecimal priceVarianceAmount, BigDecimal quantityVarianceAmount,
            BigDecimal tolerancePercentage, String varianceReason, String resolvedBy,
            Long resolvedAt, long createdAt
    ) {
    }

    public record PerformMatchPayload(
            BigDecimal tolerancePercentage
    ) {
    }

    public record ResolveMatchPayload(
            @NotBlank String resolutionNotes
    ) {
    }

    // ─── Goods Receipt ────────────────────────────────────────────────

    public record GoodsReceiptLineResponse(
            String id, String purchaseOrderLineId, String itemId, String itemName, String itemCategory,
            BigDecimal deliveredQuantity, BigDecimal rejectedQuantity, BigDecimal deductedQuantity,
            BigDecimal quantity, String unitOfMeasure, BigDecimal unitPrice,
            String locationId, String lotNumber, String qualityReason
    ) {
    }

    public record GoodsReceiptLinePayload(
            @NotBlank String purchaseOrderLineId, @NotBlank String itemId, @NotBlank String itemName,
            String itemCategory,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal deliveredQuantity,
            @NotNull @DecimalMin(value = "0") BigDecimal rejectedQuantity,
            @NotNull @DecimalMin(value = "0") BigDecimal deductedQuantity,
            BigDecimal quantity, String unitOfMeasure, @NotNull BigDecimal unitPrice,
            String locationId, String lotNumber, String qualityReason
    ) {
    }

    public record GoodsReceiptResponse(
            String id, String grnNumber, long receiptDate, String purchaseOrderId,
            String supplierId, String supplierName, String warehouseId, String status,
            String currencyCode, String notes, List<GoodsReceiptLineResponse> lines, long createdAt
    ) {
    }

    public record GoodsReceiptPayload(
            String grnNumber, long receiptDate, @NotBlank String purchaseOrderId,
            @NotBlank String supplierId, String warehouseId, String notes,
            @NotNull @Size(min = 1) List<@Valid GoodsReceiptLinePayload> lines
    ) {
    }

    public record NumberingSettings(boolean automaticNumbering) {
    }

    // ─── Supplier Invoice ─────────────────────────────────────────────

    public record SupplierInvoiceResponse(
            String id, String invoiceNumber, String supplierId, String supplierName,
            String purchaseOrderId, String goodsReceiptId, String responsiblePartyId,
            String internalReference, String missingInvoiceReason, String currencyCode,
            String baseCurrencyCode, BigDecimal exchangeRate, long exchangeRateDate,
            String exchangeRateSource, String exchangeRateOverrideReason, BigDecimal baseNetAmount,
            long invoiceDate, BigDecimal totalAmount,
            BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal netAmount,
            BigDecimal paidAmount, BigDecimal outstandingAmount,
            Long dueDate, String notes, String status, long createdAt, long updatedAt
    ) {
    }

    public record SupplierInvoicePayload(
            String invoiceNumber, @NotBlank String supplierId,
            String purchaseOrderId, String goodsReceiptId, String internalReference,
            String missingInvoiceReason, String currencyCode, BigDecimal exchangeRate,
            String exchangeRateOverrideReason, long invoiceDate, @NotNull BigDecimal totalAmount,
            BigDecimal discountAmount, BigDecimal taxAmount, Long dueDate, String notes
    ) {
    }

    // ─── Supplier Payment ─────────────────────────────────────────────

    public record SupplierPaymentResponse(
            String id, String paymentNumber, long paymentDate, String supplierId,
            String supplierName, String supplierInvoiceId, BigDecimal amount,
            String currencyCode, String paymentMethod, String notes, String operationId, String status, long createdAt
    ) {
    }

    public record SupplierPaymentPayload(
            @NotBlank String paymentNumber, long paymentDate, @NotBlank String supplierId,
            @NotBlank String supplierInvoiceId, @NotNull BigDecimal amount,
            @NotBlank String paymentMethod, String notes, @NotBlank String operationId
    ) {
    }

    // ─── Supplier Return ──────────────────────────────────────────────

    public record SupplierReturnLineResponse(
            String id, String purchaseOrderLineId, String itemId, String itemName, String itemCategory,
            BigDecimal quantity, String unitOfMeasure, BigDecimal unitPrice,
            String locationId, String reason
    ) {
    }

    public record SupplierReturnLinePayload(
            @NotBlank String purchaseOrderLineId, @NotBlank String itemId, @NotBlank String itemName,
            String itemCategory,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
            String unitOfMeasure, @NotNull BigDecimal unitPrice,
            String locationId, String reason
    ) {
    }

    public record SupplierReturnResponse(
            String id, String returnNumber, long returnDate, String purchaseOrderId,
            String supplierId, String supplierName, String warehouseId, String status,
            String notes, List<SupplierReturnLineResponse> lines, long createdAt
    ) {
    }

    public record SupplierReturnPayload(
            String returnNumber, long returnDate, @NotBlank String purchaseOrderId,
            @NotBlank String supplierId, String warehouseId, String notes,
            @NotNull @Size(min = 1) List<@Valid SupplierReturnLinePayload> lines
    ) {
    }
}
