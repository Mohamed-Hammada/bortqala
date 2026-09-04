package com.bemo.hr.trade.fieldsales.api;

import com.bemo.hr.trade.fieldsales.domain.FieldSalesDocumentType;
import com.bemo.hr.trade.fieldsales.domain.FieldSalesSyncStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class FieldSalesApi {

    private FieldSalesApi() {}

    public record CustomerSummary(
            String id,
            String code,
            String name,
            String phone,
            String address,
            String taxId,
            BigDecimal creditLimit,
            BigDecimal currentBalance,
            boolean creditHold,
            Integer paymentTermsDays
    ) {}

    public record ProductSummary(
            String id,
            String itemCode,
            String itemName,
            String unitOfMeasure,
            BigDecimal basePrice,
            BigDecimal taxRate,
            BigDecimal availableStock
    ) {}

    public record WarehouseSummary(
            String id,
            String warehouseCode,
            String warehouseName
    ) {}

    public record OfflineBundleResponse(
            List<CustomerSummary> customers,
            List<ProductSummary> products,
            List<WarehouseSummary> warehouses,
            String salesRepUserId,
            String salesRepName,
            long serverTimestamp
    ) {}

    public record SyncLineItem(
            @NotBlank String itemId,
            String itemCode,
            String itemName,
            String unitOfMeasure,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            @NotNull BigDecimal lineTotal
    ) {}

    public record SyncTransactionRequestItem(
            @NotBlank String clientOfflineId,
            @NotNull FieldSalesDocumentType documentType,
            @NotBlank String offlineDocumentNumber,
            @NotBlank String customerId,
            String customerName,
            String warehouseId,
            @NotNull BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            @NotNull BigDecimal totalAmount,
            List<SyncLineItem> lines,
            String paymentMethod,
            String allocatedInvoiceNumber,
            String returnReason,
            String customerSignaturePng,
            String customerConfirmationName,
            String gpsCoordinates,
            String notes,
            long clientCreatedAt
    ) {}

    public record SyncBatchRequest(
            @NotEmpty List<SyncTransactionRequestItem> transactions
    ) {}

    public record SyncResultItem(
            String clientOfflineId,
            String serverDocumentId,
            String serverDocumentNumber,
            FieldSalesSyncStatus status,
            String conflictReason,
            String message
    ) {}

    public record SyncBatchResponse(
            int totalCount,
            int syncedCount,
            int conflictCount,
            List<SyncResultItem> results
    ) {}

    public record OfflineTransactionRecordResponse(
            String id,
            String clientOfflineId,
            FieldSalesDocumentType documentType,
            String offlineDocumentNumber,
            String serverDocumentId,
            String serverDocumentNumber,
            String customerId,
            String customerName,
            String salesRepUserId,
            BigDecimal totalAmount,
            FieldSalesSyncStatus status,
            String conflictReason,
            String customerConfirmationName,
            String gpsCoordinates,
            long clientCreatedAt,
            long syncedAt
    ) {}
}
