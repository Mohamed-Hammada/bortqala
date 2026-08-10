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
            long createdAt,
            long updatedAt
    ) {}

    public record SalesOrderPayload(
            @NotBlank String soNumber,
            long soDate,
            @NotBlank String customerId,
            String quotationId,
            @NotNull BigDecimal totalAmount
    ) {}

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
    public record CollectionTaskRequest(@NotNull String status,String ownerUserId,long nextActionDate,String note,long version) {}
    public record CollectionTaskResponse(String id,String invoiceId,String invoiceNumber,String customerId,BigDecimal outstandingAmount,long dueDate,
            int daysOverdue,String status,String ownerUserId,long nextActionDate,String note,long version) {}
}
