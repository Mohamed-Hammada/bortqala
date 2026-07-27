package com.bemo.hr.trade.sales.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

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
}
