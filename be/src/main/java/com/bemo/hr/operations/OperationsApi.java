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
                              String categoryId, String uomId,
                              @DecimalMin("0") BigDecimal reorderPoint, @DecimalMin("0") BigDecimal reorderQuantity,
                              boolean active, Long version) { }
    public record ItemView(String id, String code, String name, String itemType, String unitCode,
                           String categoryId, String categoryName, String uomId, String uomName,
                           boolean active, BigDecimal reorderPoint, BigDecimal reorderQuantity,
                           BigDecimal currentBalance, long version, Instant createdAt, Instant updatedAt) { }
    public record ReorderAlertView(String itemId, String itemCode, String itemName, BigDecimal currentBalance,
                                   BigDecimal reorderPoint, BigDecimal reorderQuantity, BigDecimal shortage) { }
    public record TransactionRequest(String itemId, String partyId, @NotBlank @Size(max = 50) String operationType,
                                     @NotNull @Digits(integer = 15, fraction = 4) BigDecimal quantityDelta,
                                     @NotNull @Digits(integer = 17, fraction = 2) BigDecimal amountDelta,
                                     @DecimalMin("0") @DecimalMax("100") BigDecimal lossPercentage,
                                     @Size(max = 100) String referenceCode, @Size(max = 1000) String note,
                                     @Size(max = 30) String documentType, @Size(max = 1000) String reason,
                                     @Size(max = 100) String purchaseOrderNo, @Size(max = 100) String receiptNo,
                                     @Size(max = 100) String deliveryNoteNo, @Size(max = 100) String invoiceNo,
                                     @Size(max = 100) String voucherNo, @Size(max = 100) String externalRef,
                                     @Size(max = 50) String warehouse, @Size(max = 255) String attachmentName,
                                     @Size(max = 100) String attachmentContentType, Long attachmentSize,
                                     @NotNull Instant occurredAt,
                                     @Digits(integer = 13, fraction = 6) BigDecimal unitCost) {
        public TransactionRequest(String itemId, String partyId, String operationType, BigDecimal quantityDelta,
                                  BigDecimal amountDelta, BigDecimal lossPercentage, String referenceCode, String note,
                                  String documentType, String reason, String purchaseOrderNo, String receiptNo,
                                  String deliveryNoteNo, String invoiceNo, String voucherNo, String externalRef,
                                  String warehouse, String attachmentName, String attachmentContentType,
                                  Long attachmentSize, Instant occurredAt) {
            this(itemId, partyId, operationType, quantityDelta, amountDelta, lossPercentage, referenceCode, note,
                    documentType, reason, purchaseOrderNo, receiptNo, deliveryNoteNo, invoiceNo, voucherNo,
                    externalRef, warehouse, attachmentName, attachmentContentType, attachmentSize, occurredAt, null);
        }
    }
    public record StockMovementView(String id, String itemId, String itemCode, String itemName, String partyId,
                                    String partyName, String operationType, String documentType,
                                    BigDecimal quantityDelta,
                                    BigDecimal lossPercentage, String referenceCode, String note, String reason,
                                    String purchaseOrderNo, String receiptNo, String deliveryNoteNo, String invoiceNo,
                                    String voucherNo, String externalRef, String warehouse,
                                    String attachmentName, String attachmentContentType, Long attachmentSize,
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

    public record ItemCategoryRequest(@NotBlank @Size(max = 100) String name,
                                      @Size(max = 500) String description) { }
    public record ItemCategoryView(String id, String name, String description, boolean active,
                                   Instant createdAt, Instant updatedAt) { }

    public record UnitOfMeasureRequest(@NotBlank @Size(max = 50) String name,
                                       @Size(max = 20) String abbreviation,
                                       @Size(max = 500) String description) { }
    public record UnitOfMeasureView(String id, String name, String abbreviation, String description, boolean active,
                                    Instant createdAt, Instant updatedAt) { }

    public record AdjustmentRequest(@NotBlank String itemId,
                                    @NotNull @Digits(integer = 15, fraction = 4) BigDecimal quantityDelta,
                                    @Size(max = 100) String referenceCode,
                                    @NotBlank @Size(max = 1000) String reason,
                                    boolean approved,
                                    @NotNull Instant occurredAt) { }

    public record NegativeBalanceView(String itemId, String itemCode, String itemName,
                                      BigDecimal currentBalance) { }

    public record UnitConversionRequest(@NotBlank @Size(max = 36) String fromUomId,
                                        @NotBlank @Size(max = 36) String toUomId,
                                        @NotNull BigDecimal factor) { }
    public record UnitConversionView(String id, String fromUomId, String fromUomName,
                                     String toUomId, String toUomName,
                                     BigDecimal factor, Instant createdAt) { }

    public record ValuationPolicyRequest(@NotNull InventoryValuationPolicy.Method valuationMethod,
                                         String inventoryAccountId, String receiptOffsetAccountId,
                                         String cogsAccountId, String adjustmentAccountId,
                                         boolean glPostingEnabled, boolean allowBackdatedPosting,
                                         Long version) { }
    public record ValuationPolicyView(String id, InventoryValuationPolicy.Method valuationMethod,
                                      String inventoryAccountId, String receiptOffsetAccountId,
                                      String cogsAccountId, String adjustmentAccountId,
                                      boolean glPostingEnabled, boolean allowBackdatedPosting,
                                      long version, Instant createdAt, Instant updatedAt) { }
    public record MovementCostView(String id, String movementId, String itemId, String itemCode, String itemName,
                                   InventoryValuationPolicy.Method valuationMethod, BigDecimal quantityEffect,
                                   BigDecimal unitCost, BigDecimal valueEffect, String journalEntryId,
                                   String explanation, Instant occurredAt, Instant createdAt) { }
    public record ItemValuationView(String itemId, String itemCode, String itemName, BigDecimal quantityOnHand,
                                    BigDecimal valuedQuantity, BigDecimal inventoryValue, BigDecimal averageUnitCost,
                                    BigDecimal openingQuantityGap) { }
    public record ValuationReport(ValuationPolicyView policy, BigDecimal totalInventoryValue,
                                  List<ItemValuationView> items, List<MovementCostView> movementCosts) { }
    public record RevaluationRequest(@NotBlank String itemId, @NotNull @DecimalMin("0.000001") BigDecimal newUnitCost,
                                     @NotBlank @Size(max = 1000) String reason, @NotBlank @Size(max = 80) String operationId,
                                     @NotNull Instant occurredAt) { }
    public record RevaluationView(String id, String itemId, String operationId, BigDecimal quantityOnHand,
                                  BigDecimal oldValue, BigDecimal newValue, BigDecimal valueDifference,
                                  String reason, String journalEntryId, Instant occurredAt,
                                  String createdBy, Instant createdAt) { }
}
