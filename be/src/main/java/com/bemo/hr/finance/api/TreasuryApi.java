package com.bemo.hr.finance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class TreasuryApi {

    public record BankAccountResponse(
            String id,
            String bankName,
            String accountNumber,
            String iban,
            String swiftCode,
            String accountId,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record BankAccountPayload(
            @NotBlank String bankName,
            @NotBlank String accountNumber,
            String iban,
            String swiftCode,
            String accountId,
            boolean active
    ) {}

    public record TaxRateResponse(
            String id,
            String code,
            String name,
            BigDecimal ratePercentage,
            String taxType,
            String accountId,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record TaxRatePayload(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull BigDecimal ratePercentage,
            @NotBlank String taxType,
            String accountId,
            boolean active
    ) {}

    public record CurrencyResponse(
            String id,
            String code,
            String name,
            String symbol,
            boolean isBase,
            BigDecimal exchangeRate,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record CurrencyPayload(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String symbol,
            boolean isBase,
            BigDecimal exchangeRate,
            boolean active
    ) {}
}
