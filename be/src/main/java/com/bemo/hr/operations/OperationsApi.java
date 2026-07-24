package com.bemo.hr.operations;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OperationsApi {
    private OperationsApi() { }

    public record ItemRequest(@NotBlank @Size(max = 50) String code, @NotBlank @Size(max = 160) String name,
                              @NotBlank @Size(max = 50) String itemType, @NotBlank @Size(max = 30) String unitCode,
                              boolean active, Long version) { }
    public record ItemView(String id, String code, String name, String itemType, String unitCode, boolean active,
                           BigDecimal currentBalance, long version, Instant createdAt, Instant updatedAt) { }
    public record TransactionRequest(String itemId, String partyId, @NotBlank @Size(max = 50) String operationType,
                                     @NotNull @Digits(integer = 15, fraction = 4) BigDecimal quantityDelta,
                                     @NotNull @Digits(integer = 17, fraction = 2) BigDecimal amountDelta,
                                     @DecimalMin("0") @DecimalMax("100") BigDecimal lossPercentage,
                                     @Size(max = 100) String referenceCode, @Size(max = 1000) String note,
                                     @NotNull Instant occurredAt) { }
    public record StockMovementView(String id, String itemId, String itemCode, String itemName, String partyId,
                                    String partyName, String operationType, BigDecimal quantityDelta,
                                    BigDecimal lossPercentage, String referenceCode, String note,
                                    Instant occurredAt, String createdBy, Instant createdAt) { }
    public record LedgerView(String id, String partyId, String partyName, String entryType, BigDecimal amountDelta,
                             String referenceCode, String note, Instant occurredAt, String createdBy, Instant createdAt) { }
    public record PartyBalance(String partyId, String partyCode, String partyName, String partyType, BigDecimal balance) { }
    public record AdvanceRequest(@NotBlank String employeeId, @NotNull @Digits(integer = 17, fraction = 2) BigDecimal amountDelta,
                                 @NotBlank @Size(max = 30) String entryType, @Size(max = 1000) String note,
                                 @NotNull Instant occurredAt) { }
    public record AdvanceView(String id, String employeeId, String employeeCode, String employeeName,
                              BigDecimal amountDelta, BigDecimal currentBalance, String entryType, String note,
                              Instant occurredAt, String createdBy, Instant createdAt) { }
    public record Snapshot(List<ItemView> items, List<StockMovementView> movements,
                           List<PartyBalance> partyBalances, List<LedgerView> ledgerEntries,
                           List<AdvanceView> employeeAdvances) { }
}
