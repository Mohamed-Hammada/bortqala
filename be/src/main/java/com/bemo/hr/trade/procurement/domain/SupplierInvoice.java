package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "supplier_invoices")
public class SupplierInvoice {

    public enum Status { UNPAID, PARTIALLY_PAID, PAID, CANCELLED }

    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "invoice_number", length = 50) private String invoiceNumber;
    @Column(name = "internal_reference", nullable = false, length = 50) private String internalReference;
    @Column(name = "missing_invoice_reason", length = 255) private String missingInvoiceReason;
    @Column(name = "currency_code", nullable = false, length = 10) private String currencyCode;
    @Column(name = "base_currency_code", nullable = false, length = 10) private String baseCurrencyCode = "EGP";
    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 6) private BigDecimal exchangeRate = BigDecimal.ONE;
    @Column(name = "exchange_rate_date", nullable = false) private LocalDate exchangeRateDate;
    @Column(name = "exchange_rate_source", nullable = false, length = 50) private String exchangeRateSource = "BASE_CURRENCY";
    @Column(name = "exchange_rate_override_reason", length = 500) private String exchangeRateOverrideReason;
    @Column(name = "base_net_amount", nullable = false, precision = 15, scale = 2) private BigDecimal baseNetAmount = BigDecimal.ZERO;
    @Column(name = "supplier_id", nullable = false, length = 36) private String supplierId;
    @Column(name = "purchase_order_id", length = 36) private String purchaseOrderId;
    @Column(name = "goods_receipt_id", length = 36) private String goodsReceiptId;
    @Column(name = "responsible_party_id", length = 36) private String responsiblePartyId;
    @Column(name = "invoice_date", nullable = false) private LocalDate invoiceDate;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2) private BigDecimal totalAmount;
    @Column(name = "discount_amount", precision = 15, scale = 2) private BigDecimal discountAmount;
    @Column(name = "tax_amount", precision = 15, scale = 2) private BigDecimal taxAmount;
    @Column(name = "net_amount", precision = 15, scale = 2) private BigDecimal netAmount;
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(length = 500) private String notes;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "created_at", nullable = false) private long createdAt;
    @Column(name = "updated_at", nullable = false) private long updatedAt;
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SupplierInvoice() {}

    public SupplierInvoice(String invoiceNumber, String supplierId, String purchaseOrderId,
                           String goodsReceiptId, String responsiblePartyId,
                           LocalDate invoiceDate, BigDecimal totalAmount,
                           BigDecimal discountAmount, BigDecimal taxAmount,
                           LocalDate dueDate, String notes) {
        this(invoiceNumber, invoiceNumber, null, "EGP", supplierId, purchaseOrderId, goodsReceiptId,
                responsiblePartyId, invoiceDate, totalAmount, discountAmount, taxAmount, dueDate, notes);
    }

    public SupplierInvoice(String invoiceNumber, String internalReference, String missingInvoiceReason,
                           String currencyCode, String supplierId, String purchaseOrderId,
                           String goodsReceiptId, String responsiblePartyId,
                           LocalDate invoiceDate, BigDecimal totalAmount,
                           BigDecimal discountAmount, BigDecimal taxAmount,
                           LocalDate dueDate, String notes) {
        this.id = UUID.randomUUID().toString();
        this.invoiceNumber = invoiceNumber == null || invoiceNumber.isBlank() ? null : invoiceNumber.strip();
        this.internalReference = internalReference.strip();
        this.missingInvoiceReason = missingInvoiceReason == null || missingInvoiceReason.isBlank() ? null : missingInvoiceReason.strip();
        this.currencyCode = currencyCode == null || currencyCode.isBlank() ? "EGP" : currencyCode.strip().toUpperCase();
        this.supplierId = supplierId;
        this.purchaseOrderId = purchaseOrderId;
        this.goodsReceiptId = goodsReceiptId;
        this.responsiblePartyId = responsiblePartyId;
        this.invoiceDate = invoiceDate;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.netAmount = totalAmount.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                .add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
        this.exchangeRateDate = invoiceDate;
        this.baseNetAmount = this.netAmount;
        this.dueDate = dueDate;
        this.notes = notes;
        this.status = Status.UNPAID.name();
    }

    public void applyExchangeRate(String baseCurrencyCode, BigDecimal exchangeRate, LocalDate rateDate,
                                  String source, String overrideReason) {
        if (exchangeRate == null || exchangeRate.signum() <= 0) {
            throw new IllegalArgumentException("Exchange rate must be greater than zero.");
        }
        this.baseCurrencyCode = baseCurrencyCode;
        this.exchangeRate = exchangeRate;
        this.exchangeRateDate = rateDate;
        this.exchangeRateSource = source;
        this.exchangeRateOverrideReason = overrideReason == null || overrideReason.isBlank() ? null : overrideReason.strip();
        this.baseNetAmount = netAmount.multiply(exchangeRate).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public void updatePaymentStatus(BigDecimal paidAmount) {
        if (paidAmount == null || paidAmount.signum() <= 0) this.status = Status.UNPAID.name();
        else if (paidAmount.compareTo(netAmount) >= 0) this.status = Status.PAID.name();
        else this.status = Status.PARTIALLY_PAID.name();
    }

    @PrePersist void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getDocumentReference() { return invoiceNumber != null ? invoiceNumber : internalReference; }
    public String getInternalReference() { return internalReference; }
    public String getMissingInvoiceReason() { return missingInvoiceReason; }
    public String getCurrencyCode() { return currencyCode; }
    public String getBaseCurrencyCode() { return baseCurrencyCode; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public LocalDate getExchangeRateDate() { return exchangeRateDate; }
    public String getExchangeRateSource() { return exchangeRateSource; }
    public String getExchangeRateOverrideReason() { return exchangeRateOverrideReason; }
    public BigDecimal getBaseNetAmount() { return baseNetAmount; }
    public String getSupplierId() { return supplierId; }
    public String getPurchaseOrderId() { return purchaseOrderId; }
    public String getGoodsReceiptId() { return goodsReceiptId; }
    public String getResponsiblePartyId() { return responsiblePartyId; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public String getNotes() { return notes; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
