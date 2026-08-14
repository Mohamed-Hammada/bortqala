package com.bemo.hr.trade.sales.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;

public class SalesApi {

    public record SalesOrderResponse(
            String id,
            String soNumber,
            long soDate,
            String customerId,
            String quotationId,
            String status,
            BigDecimal totalAmount,
            String warehouseId,
            String currencyCode,
            List<SalesOrderLineResponse> lines,
            long createdAt,
            long updatedAt
    ) {}

    public record SalesOrderPayload(
            @NotBlank String soNumber,
            long soDate,
            @NotBlank String customerId,
            String quotationId,
            BigDecimal totalAmount,
            String warehouseId,
            String currencyCode,
            List<SalesOrderLineRequest> lines
    ) {}

    public record SalesOrderLineRequest(@NotBlank String itemId,@NotBlank String itemName,
            @NotNull @Positive BigDecimal quantity,@NotNull @Positive BigDecimal unitPrice,@NotNull @Min(0) BigDecimal discountRate) {}
    public record SalesOrderLineResponse(String id,String itemId,String itemName,BigDecimal orderedQuantity,
            BigDecimal deliveredQuantity,BigDecimal unitPrice,BigDecimal discountRate,BigDecimal netPrice,BigDecimal lineTotal) {}

    public record DeliveryRequest(@NotBlank String deliveryNumber,long deliveryDate,@NotBlank String operationId) {}
    public record DeliveryLineResponse(String id,String salesOrderLineId,String itemId,BigDecimal quantity,
            BigDecimal unitPrice,String stockMovementId,BigDecimal unitCogs,BigDecimal cogsAmount) {}
    public record DeliveryResponse(String id,String deliveryNumber,String salesOrderId,String customerId,long deliveryDate,
            String warehouseId,String operationId,String invoiceId,String invoiceNumber,String status,List<DeliveryLineResponse> lines) {}
    public record ReturnLineRequest(@NotBlank String deliveryLineId,@NotNull @Positive BigDecimal quantity,@NotBlank String disposition) {}
    public record ReturnRequest(@NotBlank String returnNumber,@NotBlank String deliveryId,long returnDate,@NotBlank String reason,
            @NotBlank String operationId,@NotEmpty List<ReturnLineRequest> lines) {}
    public record ReturnLineResponse(String id,String deliveryLineId,String itemId,BigDecimal quantity,String disposition,
            String stockMovementId,BigDecimal creditAmount,BigDecimal cogsAmount) {}
    public record ReturnResponse(String id,String returnNumber,String salesOrderId,String customerId,long returnDate,String reason,
            String deliveryId,String warehouseId,String operationId,String creditNoteId,String creditNoteNumber,String status,List<ReturnLineResponse> lines) {}

    public record CreditProfileRequest(@NotNull @Min(0) BigDecimal creditLimit,@Min(0) int paymentTermsDays,boolean creditHold) {}
    public record CreditProfileResponse(String customerId,BigDecimal creditLimit,int paymentTermsDays,boolean creditHold,BigDecimal outstanding,BigDecimal available,long version) {}
    public record InvoiceRequest(@NotBlank String invoiceNumber,@NotBlank String customerId,String salesOrderId,long invoiceDate,long dueDate,
            @NotBlank String currencyCode,@NotNull @Positive BigDecimal amount) {}
    public record InvoiceResponse(String id,String invoiceNumber,String customerId,String salesOrderId,long invoiceDate,long dueDate,
            String currencyCode,BigDecimal amount,BigDecimal outstandingAmount,String status,long version) {}
    public record AllocationRequest(@NotBlank String invoiceId,@NotNull @Positive BigDecimal amount) {}
    public record ReceiptRequest(@NotBlank String receiptNumber,@NotBlank String customerId,long receiptDate,@NotBlank String currencyCode,
            @NotNull @Positive BigDecimal amount,@NotBlank String operationId,List<AllocationRequest> allocations) {}
    public record AllocationResponse(String invoiceId,BigDecimal amount) {}
    public record ReceiptResponse(String id,String receiptNumber,String customerId,long receiptDate,String currencyCode,BigDecimal amount,
            BigDecimal unallocatedAmount,String operationId,List<AllocationResponse> allocations) {}
    public record AgingResponse(long asOf,BigDecimal current,BigDecimal days1To30,BigDecimal days31To60,BigDecimal days61To90,BigDecimal over90,BigDecimal total) {}
    public record CollectionTaskRequest(@NotNull String status,String ownerUserId,long nextActionDate,String note,long version,@Positive long asOf) {}
    public record CollectionTaskResponse(String id,String invoiceId,String invoiceNumber,String customerId,BigDecimal outstandingAmount,long dueDate,
            int daysOverdue,String status,String ownerUserId,long nextActionDate,String note,long version) {}
}
