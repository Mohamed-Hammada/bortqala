package com.bemo.hr.trade.sales.api;

import com.bemo.hr.trade.sales.domain.QuotationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class SalesQuotationApi {

    private SalesQuotationApi() {
    }

    public record QuotationLineItem(
            @NotBlank String itemId,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            String notes
    ) {
    }

    public record CreateQuotationRequest(
            @NotBlank String customerId,
            @NotNull LocalDate quoteDate,
            @NotNull LocalDate validUntil,
            String termsAndConditions,
            @NotEmpty List<QuotationLineItem> lines
    ) {
    }

    public record QuotationLineResponse(
            String id,
            String itemId,
            String itemCode,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal lineTotal,
            String notes
    ) {
    }

    public record QuotationResponse(
            String id,
            String quotationNumber,
            String customerId,
            String customerName,
            LocalDate quoteDate,
            LocalDate validUntil,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            QuotationStatus status,
            String termsAndConditions,
            String salesOrderId,
            List<QuotationLineResponse> lines,
            long createdAt,
            long updatedAt,
            long version
    ) {
    }
}
