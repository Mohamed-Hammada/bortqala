package com.bemo.hr.trade.procurement.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class ProcurementApi {

    public record PurchaseOrderLineResponse(
            String id, String itemId, String itemName, String itemCategory,
            BigDecimal quantity, String unitOfMeasure, BigDecimal unitPrice, BigDecimal lineTotal
    ) {}

    public record PurchaseOrderLinePayload(
            @NotBlank String itemId, @NotBlank String itemName, String itemCategory,
            @NotNull BigDecimal quantity, String unitOfMeasure, @NotNull BigDecimal unitPrice
    ) {}

    public record PurchaseOrderResponse(
            String id, String poNumber, long poDate, String supplierId, String supplierName,
            String purchaseRequestId, String paymentTerms, String status, BigDecimal totalAmount,
            List<PurchaseOrderLineResponse> items, long createdAt, long updatedAt
    ) {}

    public record PurchaseOrderPayload(
            String poNumber, long poDate, @NotBlank String supplierId,
            String purchaseRequestId, String paymentTerms,
            List<PurchaseOrderLinePayload> items
    ) {}

    // ─── Goods Receipt ────────────────────────────────────────────────

    public record GoodsReceiptLineResponse(
            String id, String purchaseOrderLineId, String itemId, String itemName, String itemCategory,
            BigDecimal deliveredQuantity, BigDecimal rejectedQuantity, BigDecimal deductedQuantity,
            BigDecimal quantity, String unitOfMeasure, BigDecimal unitPrice,
            String locationId, String lotNumber, String qualityReason
    ) {}

    public record GoodsReceiptLinePayload(
            @NotBlank String purchaseOrderLineId, @NotBlank String itemId, @NotBlank String itemName, String itemCategory,
            BigDecimal deliveredQuantity, BigDecimal rejectedQuantity, BigDecimal deductedQuantity,
            BigDecimal quantity, String unitOfMeasure, @NotNull BigDecimal unitPrice,
            String locationId, String lotNumber, String qualityReason
    ) {}

    public record GoodsReceiptResponse(
            String id, String grnNumber, long receiptDate, String purchaseOrderId,
            String supplierId, String supplierName, String warehouseId, String status,
            String notes, List<GoodsReceiptLineResponse> lines, long createdAt
    ) {}

    public record GoodsReceiptPayload(
            String grnNumber, long receiptDate, @NotBlank String purchaseOrderId,
            @NotBlank String supplierId, String warehouseId, String notes,
            @NotNull List<GoodsReceiptLinePayload> lines
    ) {}

    public record NumberingSettings(boolean automaticNumbering) {}

    // ─── Supplier Invoice ─────────────────────────────────────────────

    public record SupplierInvoiceResponse(
            String id, String invoiceNumber, String supplierId, String supplierName,
            String purchaseOrderId, String goodsReceiptId, String responsiblePartyId,
            long invoiceDate, BigDecimal totalAmount,
            BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal netAmount,
            BigDecimal paidAmount, BigDecimal outstandingAmount,
            Long dueDate, String notes, String status, long createdAt, long updatedAt
    ) {}

    public record SupplierInvoicePayload(
            String invoiceNumber, @NotBlank String supplierId,
            String purchaseOrderId, String goodsReceiptId, String internalReference,
            String missingInvoiceReason, long invoiceDate, @NotNull BigDecimal totalAmount,
            BigDecimal discountAmount, BigDecimal taxAmount, Long dueDate, String notes
    ) {}

    // ─── Supplier Payment ─────────────────────────────────────────────

    public record SupplierPaymentResponse(
            String id, String paymentNumber, long paymentDate, String supplierId,
            String supplierName, String supplierInvoiceId, BigDecimal amount,
            String paymentMethod, String notes, String status, long createdAt
    ) {}

    public record SupplierPaymentPayload(
            @NotBlank String paymentNumber, long paymentDate, @NotBlank String supplierId,
            @NotBlank String supplierInvoiceId, @NotNull BigDecimal amount,
            @NotBlank String paymentMethod, String notes
    ) {}
}
